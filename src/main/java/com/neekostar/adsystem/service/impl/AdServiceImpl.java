package com.neekostar.adsystem.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.neekostar.adsystem.dto.AdCreateDto;
import com.neekostar.adsystem.dto.AdResponseDto;
import com.neekostar.adsystem.dto.AdUpdateDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.InvalidArgumentException;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.mapper.AdMapper;
import com.neekostar.adsystem.model.Ad;
import com.neekostar.adsystem.model.AdStatus;
import com.neekostar.adsystem.model.Category;
import com.neekostar.adsystem.model.Comment;
import com.neekostar.adsystem.model.User;
import com.neekostar.adsystem.repository.AdRepository;
import com.neekostar.adsystem.repository.CommentRepository;
import com.neekostar.adsystem.repository.UserRepository;
import com.neekostar.adsystem.service.AdService;
import com.neekostar.adsystem.service.MinioService;
import com.neekostar.adsystem.specification.AdSpecification;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Service
@Transactional(readOnly = true)
public class AdServiceImpl implements AdService {
    private final MinioService minioService;
    private final AdRepository adRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final AdMapper adMapper;

    private static final List<String> ALLOWED_SORT_FIELDS = List.of("price", "createdAt", "updatedAt");

    @Autowired
    public AdServiceImpl(MinioService minioService,
                         AdRepository adRepository,
                         UserRepository userRepository,
                         CommentRepository commentRepository,
                         AdMapper adMapper) {
        this.minioService = minioService;
        this.adRepository = adRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.adMapper = adMapper;
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "userAds", key = "#result.username"),
                    @CacheEvict(value = {"promotedAds", "nonPromotedAds", "filteredAds"}, allEntries = true)
            }
    )
    public AdResponseDto createAd(@NotNull AdCreateDto adCreateDto) {
        log.info("Creating a new ad: {}", adCreateDto.getTitle());
        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findUserByUsername(authenticatedUsername)
                .orElseThrow(() -> {
                    log.error("User not found. Username: {}", authenticatedUsername);
                    return new ResourceNotFoundException("User", "username", authenticatedUsername);
                });

        Ad ad = adMapper.toEntity(adCreateDto);
        ad.setIsPromoted(false);
        ad.setPromotionEndDate(null);
        ad.setUser(user);
        Ad savedAd = adRepository.save(ad);
        log.info("Ad created successfully. Ad ID: {}, User: {}", savedAd.getId(), authenticatedUsername);

        return adMapper.toDto(savedAd);
    }

    @Override
    @Transactional
    @Caching(
            put = @CachePut(value = "singleAd", key = "#adId"),
            evict = {
                    @CacheEvict(value = "userAds", key = "#result.username"),
                    @CacheEvict(value = {"promotedAds", "nonPromotedAds", "filteredAds"}, allEntries = true)
            }
    )
    public AdResponseDto updateAd(UUID adId,
                                  AdUpdateDto adUpdateDto) {
        log.info("Updating ad with ID: {}", adId);
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> {
                    log.error("Ad not found. ID: {}", adId);
                    return new ResourceNotFoundException("Ad", "id", String.valueOf(adId));
                });

        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(ad.getUser().getUsername())) {
            log.warn("User is not allowed to update the ad. User: {}, Ad owner: {}",
                    authenticatedUsername, ad.getUser().getUsername());
            throw new AccessDeniedException("You can only update your own ads");
        }

        if (adUpdateDto.getTitle() != null) {
            log.debug("Updating title. ID: {}", adId);
            ad.setTitle(adUpdateDto.getTitle());
        }
        if (adUpdateDto.getDescription() != null) {
            log.debug("Updating description. ID: {}", adId);
            ad.setDescription(adUpdateDto.getDescription());
        }
        if (adUpdateDto.getPrice() != null) {
            log.debug("Updating price. ID: {}, New price: {}", adId, adUpdateDto.getPrice());
            ad.setPrice(adUpdateDto.getPrice());
        }

        Ad updatedAd = adRepository.save(ad);
        log.info("Ad updated successfully. Ad ID: {}", updatedAd.getId());

        return adMapper.toDto(updatedAd);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "singleAd", key = "#adId"),
                    @CacheEvict(value = "userAds", allEntries = true),
                    @CacheEvict(value = {"promotedAds", "nonPromotedAds", "filteredAds"}, allEntries = true)
            }
    )
    public void deleteAd(UUID adId) {
        log.info("Deleting ad. ID: {}", adId);
        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> {
                    log.error("Ad not found. ID: {}", adId);
                    return new ResourceNotFoundException("Ad", "id", String.valueOf(adId));
                });

        if (!authenticatedUsername.equals(ad.getUser().getUsername())) {
            log.warn("Unauthorized deletion attempt. User: {}, Ad owner: {}",
                    authenticatedUsername, ad.getUser().getUsername());
            throw new AccessDeniedException("You can only delete your own ads");
        }

        commentRepository.deleteByAdId(adId);
        adRepository.delete(ad);
        log.info("Ad deleted successfully. ID: {}", adId);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    @Cacheable(value = "userAds", key = "#username + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<AdResponseDto> getAdsByUser(String username, Pageable pageable) {
        log.info("Fetching ads for user: {}", username);
        Page<Ad> ads = adRepository.findAdByUserUsernameAndStatus(username, AdStatus.ACTIVE, pageable);
        log.info("Fetched {} ads for user: {}", ads.getTotalElements(), username);
        return ads.map(adMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    @Cacheable(value = "singleAd", key = "#adId")
    public AdResponseDto getAd(UUID adId) {
        log.info("Fetching ad details. ID: {}", adId);
        return adRepository.findById(adId)
                .map(ad -> {
                    log.info("Ad retrieved. ID: {}, Title: {}", adId, ad.getTitle());
                    return adMapper.toDto(ad);
                })
                .orElseThrow(() -> {
                    log.error("Ad not found. ID: {}", adId);
                    return new ResourceNotFoundException("Ad", "id", String.valueOf(adId));
                });
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = {"promotedAds", "nonPromotedAds"}, allEntries = true),
                    @CacheEvict(value = "filteredAds", allEntries = true)
            }
    )
    public void promoteAd(UUID adId, int days) {
        log.info("Promoting ad. ID: {}, Days: {}", adId, days);
        if (days <= 0) {
            log.error("Invalid days parameter: {}", days);
            throw new InvalidArgumentException("Days must be a positive integer");
        }
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> {
                    log.error("Ad not found. ID: {}", adId);
                    return new ResourceNotFoundException("Ad", "id", String.valueOf(adId));
                });

        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(ad.getUser().getUsername())) {
            log.warn("Unauthorized promotion attempt. User: {}, Ad owner: {}",
                    authenticatedUsername, ad.getUser().getUsername());
            throw new AccessDeniedException("You can only promote your own ads");
        }

        ad.setIsPromoted(true);
        ad.setPromotionEndDate(LocalDateTime.now().plusDays(days));
        adRepository.save(ad);
        log.info("Ad promoted. ID: {}, End date: {}", adId, ad.getPromotionEndDate());
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    @Cacheable(value = "promotedAds")
    public List<AdResponseDto> getPromotedAds() {
        log.info("Fetching promoted ads");
        List<AdResponseDto> result = adRepository.findAll().stream()
                .filter(Ad::getIsPromoted)
                .filter(ad -> ad.getPromotionEndDate().isAfter(LocalDateTime.now()))
                .filter(ad -> ad.getStatus() == AdStatus.ACTIVE)
                .map(adMapper::toDto)
                .collect(Collectors.toList());

        log.info("Fetched {} promoted ads", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    @Cacheable(value = "nonPromotedAds")
    public List<AdResponseDto> getNonPromotedAds() {
        log.info("Fetching non-promoted ads");
        List<AdResponseDto> result = adRepository.findAll().stream()
                .filter(ad -> !ad.getIsPromoted() || ad.getPromotionEndDate().isBefore(LocalDateTime.now()))
                .filter(ad -> ad.getStatus() == AdStatus.ACTIVE)
                .map(adMapper::toDto)
                .collect(Collectors.toList());

        log.info("Fetched {} non-promoted ads", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    @Cacheable(value = "filteredAds", key = "{#city, #category, #minPrice, #maxPrice, #keyword, #sortBy, #sortDir}")
    public Page<AdResponseDto> filterAds(String city,
                                         Category category,
                                         BigDecimal minPrice,
                                         BigDecimal maxPrice,
                                         String keyword,
                                         String sortBy,
                                         String sortDir,
                                         Pageable pageable) {
        log.info("Filtering ads. City: {}, Category: {}, MinPrice: {}, MaxPrice: {}, Keyword: {}, SortBy: {}, SortDir: {}",
                city, category, minPrice, maxPrice, keyword, sortBy, sortDir);
        Specification<Ad> spec = AdSpecification.combineSpecifications(city, category, minPrice, maxPrice, keyword);

        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            log.warn("Invalid sortBy parameter: {}. Using default: createdAt", sortBy);
            sortBy = "createdAt";
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if (sortDir != null && sortDir.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }

        Sort sort = Sort.by(direction, sortBy);
        List<Ad> ads = adRepository.findAll(spec, sort);
        log.debug("Found {} ads after initial filtering", ads.size());

        LocalDateTime now = LocalDateTime.now();
        List<Ad> promoted = new ArrayList<>();
        List<Ad> nonPromoted = new ArrayList<>();

        for (Ad ad : ads) {
            if (ad.getStatus() == AdStatus.ACTIVE) {
                boolean isPromoted = ad.getIsPromoted() && ad.getPromotionEndDate() != null &&
                        ad.getPromotionEndDate().isAfter(now);
                if (isPromoted) {
                    promoted.add(ad);
                } else {
                    nonPromoted.add(ad);
                }
            }
        }

        log.info("Promoted ads: {}, Non-promoted ads: {}", promoted.size(), nonPromoted.size());

        nonPromoted.sort((ad1, ad2) -> {
            float rating1 = (ad1.getUser().getRating() != null) ? ad1.getUser().getRating() : 0f;
            float rating2 = (ad2.getUser().getRating() != null) ? ad2.getUser().getRating() : 0f;
            return Float.compare(rating2, rating1);
        });

        List<Ad> sortedAds = new ArrayList<>();
        sortedAds.addAll(promoted);
        sortedAds.addAll(nonPromoted);

        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int fromIndex = pageNumber * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, sortedAds.size());

        if (fromIndex > toIndex) {
            return new PageImpl<>(Collections.emptyList(), pageable, sortedAds.size());
        }

        List<Ad> pagedAds = sortedAds.subList(fromIndex, toIndex);

        List<AdResponseDto> result = pagedAds.stream()
                .map(adMapper::toDto)
                .collect(Collectors.toList());

        log.info("Filtered ads: {}", result.size());

        return new PageImpl<>(result, pageable, sortedAds.size());
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "singleAd", key = "#adId"),
                    @CacheEvict(value = "filteredAds", allEntries = true)
            }
    )
    public AdResponseDto commentAd(UUID adId, String comment) {
        log.info("Adding comment. Ad ID: {}", adId);
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> {
                    log.error("Ad not found. ID: {}", adId);
                    return new ResourceNotFoundException("Ad", "id", String.valueOf(adId));
                });

        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findUserByUsername(authenticatedUsername)
                .orElseThrow(() -> {
                    log.error("User not found. Username: {}", authenticatedUsername);
                    return new ResourceNotFoundException("User", "username", authenticatedUsername);
                });
        Comment newComment = new Comment();
        newComment.setCommentText(comment);
        newComment.setUser(user);
        newComment.setAd(ad);

        Comment savedComment = commentRepository.saveAndFlush(newComment);
        ad.getComments().add(savedComment);
        Ad savedAd = adRepository.save(ad);
        log.info("Comment added. Ad ID: {}, User: {}", adId, authenticatedUsername);

        return adMapper.toDto(savedAd);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "singleAd", key = "#adId"),
                    @CacheEvict(value = "userAds", allEntries = true),
                    @CacheEvict(value = {"promotedAds", "nonPromotedAds", "filteredAds"}, allEntries = true)
            }
    )
    public AdResponseDto uploadAdImage(UUID adId, MultipartFile file) {
        log.info("Uploading image for ad. Ad ID: {}", adId);
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> {
                    log.error("Ad not found. ID: {}", adId);
                    return new ResourceNotFoundException("Ad", "id", String.valueOf(adId));
                });

        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(ad.getUser().getUsername())) {
            log.warn("Unauthorized image upload attempt. User: {}, Ad owner: {}",
                    authenticatedUsername, ad.getUser().getUsername());
            throw new AccessDeniedException("You can only upload images for your own ads");
        }

        if (ad.getImageUrl() != null) {
            String oldObjectName = minioService.resolveObjectNameFromUrl(ad.getImageUrl());
            minioService.removeFile(oldObjectName);
            log.info("Old image removed. Ad ID: {}, Object name: {}", adId, oldObjectName);
        }

        String imageUrl = minioService.uploadFile(file, "ads");
        ad.setImageUrl(imageUrl);
        Ad savedAd = adRepository.saveAndFlush(ad);

        log.info("Image uploaded successfully. Ad ID: {}, Image URL: {}", adId, imageUrl);
        return adMapper.toDto(savedAd);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "singleAd", key = "#adId"),
                    @CacheEvict(value = "userAds", allEntries = true),
                    @CacheEvict(value = {"promotedAds", "nonPromotedAds", "filteredAds"}, allEntries = true)
            }
    )
    public void removeAdImage(UUID adId) {
        log.info("Removing image for ad. Ad ID: {}", adId);
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> {
                    log.error("Ad not found. ID: {}", adId);
                    return new ResourceNotFoundException("Ad", "id", String.valueOf(adId));
                });

        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(ad.getUser().getUsername())) {
            log.warn("Unauthorized image removal attempt. User: {}, Ad owner: {}",
                    authenticatedUsername, ad.getUser().getUsername());
            throw new AccessDeniedException("You can only remove images for your own ads");
        }

        if (ad.getImageUrl() != null) {
            String objectName = minioService.resolveObjectNameFromUrl(ad.getImageUrl());
            minioService.removeFile(objectName);
            ad.setImageUrl(null);
            adRepository.saveAndFlush(ad);
            log.info("Image removed successfully. Ad ID: {}, Object name: {}", adId, objectName);
        } else {
            log.warn("No image to remove. Ad ID: {}", adId);
        }
    }
}
