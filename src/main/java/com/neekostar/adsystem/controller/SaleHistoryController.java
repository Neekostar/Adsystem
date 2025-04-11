package com.neekostar.adsystem.controller;

import com.neekostar.adsystem.dto.ErrorResponse;
import com.neekostar.adsystem.dto.SaleHistoryResponseDto;
import com.neekostar.adsystem.service.SaleHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
@Tag(
        name = "Sale History Management",
        description = "This controller manages the retrieval of sale and purchase history. <br><br>" +
                "The available operations include: <ul>" +
                "<li><b>Get Sales by Seller</b> – Retrieves all sale records associated with a specific seller. " +
                "The response is a list of sales made by the seller. If no records are found, an empty list is returned.</li>" +
                "<li><b>Get Purchases by Buyer</b> – Retrieves all purchase records associated with the authenticated buyer. " +
                "The response is a list of purchases (sales that the buyer has made). If no records are found, an empty list is returned.</li>" +
                "</ul>" +
                "Possible error responses include: <br><br>" +
                "<b>400</b> – if an invalid username is provided (e.g., empty or malformed)."
)
public class SaleHistoryController {

    private final SaleHistoryService saleHistoryService;

    @Autowired
    public SaleHistoryController(SaleHistoryService saleHistoryService) {
        this.saleHistoryService = saleHistoryService;
    }

    @GetMapping("/seller/{sellerUsername}")
    @Operation(
            summary = "Get Sales by Seller",
            description = "Retrieves a list of all sales for the specified seller. " +
                    "The seller's username is provided as a path parameter. " +
                    "If no sales are found, an empty list is returned. " +
                    "Possible error: <b>400</b> if an invalid seller username is provided.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "sellerUsername",
                            description = "The username of the seller whose sales history is to be retrieved",
                            required = true
                    )
            },
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Sales retrieved successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SaleHistoryResponseDto.class)
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid seller username provided",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "Invalid seller username",
                                                              "path": "/api/sales/seller/john_doe",
                                                              "method": "GET"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> getSalesBySeller(@PathVariable String sellerUsername,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SaleHistoryResponseDto> sales = saleHistoryService.getSalesBySeller(sellerUsername, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(sales);
    }

    @GetMapping("/purchases")
    @Operation(
            summary = "Get Purchases by Buyer",
            description = "Retrieves the purchase history for the authenticated buyer. " +
                    "The buyer's username is obtained from the security context. " +
                    "If no purchase records are found, an empty list is returned. " +
                    "Possible error: <b>400</b> if the buyer's username is invalid.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Purchases retrieved successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SaleHistoryResponseDto.class)
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid buyer username",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "Invalid buyer username",
                                                              "path": "/api/sales/purchases",
                                                              "method": "GET"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> getSalesByBuyer(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(page, size);
        Page<SaleHistoryResponseDto> purchases = saleHistoryService.getPurchasesByBuyer(authenticatedUsername, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(purchases);
    }
}
