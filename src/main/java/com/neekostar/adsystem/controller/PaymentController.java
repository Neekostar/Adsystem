package com.neekostar.adsystem.controller;

import java.util.UUID;
import com.neekostar.adsystem.dto.ErrorResponse;
import com.neekostar.adsystem.dto.PaymentResponseDto;
import com.neekostar.adsystem.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@Tag(
        name = "Payment Management",
        description = "This controller handles all operations related to payments for advertisements. <br><br>" +
                "The available operations include: <ul>" +
                "<li><b>Create Payment</b> – Processes a payment for purchasing an advertisement. " +
                "This operation verifies that the authenticated user exists, that the advertisement is available for purchase " +
                "(not already SOLD), and that the buyer is not the ad owner. On success, the ad is marked as SOLD and a sale history " +
                "record is created.</li>" +
                "<li><b>Get Payment History</b> – Retrieves the payment history for the authenticated user. " +
                "The result is cached for performance and includes all payments made by the user.</li>" +
                "</ul>" +
                "Possible exceptions include: <br><br>" +
                "<b>ResourceNotFoundException</b> – when the user or advertisement is not found; <br>" +
                "<b>AccessDeniedException</b> – when the advertisement is already sold or the user attempts to buy their own ad; <br>" +
                "<b>IllegalArgumentException</b> – for invalid ad price values."
)
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{adId}")
    @Operation(
            summary = "Create Payment",
            description = "Creates a new payment for the specified advertisement. " +
                    "The authenticated user (buyer) must not be the owner of the ad, and the ad must not already be sold. " +
                    "Upon successful payment, the ad status is updated to SOLD and a sale history record is created. " +
                    "Possible errors: <ul>" +
                    "<li><b>ResourceNotFoundException</b> – if the ad or buyer is not found.</li>" +
                    "<li><b>AccessDeniedException</b> – if the ad is already sold or if the user tries to purchase their own ad.</li>" +
                    "<li><b>IllegalArgumentException</b> – if the ad price is non-positive.</li>" +
                    "</ul>",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "adId",
                            description = "Unique identifier (UUID) of the advertisement to purchase",
                            required = true
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Payment created successfully; returns the created payment details",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = PaymentResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid payment data (e.g., ad price is non-positive)",
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
                                                              "message": "Ad price must be positive",
                                                              "path": "/api/payments/11111111-2222-3333-4444-555555555555",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The ad is already sold or the user cannot purchase their own ad",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "ForbiddenExample_BoughtOwnAd",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You can't purchase your own ads",
                                                              "path": "/api/payments/11111111-2222-3333-4444-555555555555",
                                                              "method": "POST"
                                                            }
                                                            """
                                            ),
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "ForbiddenExample_AdSold",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "Ad is already sold",
                                                              "path": "/api/payments/11111111-2222-3333-4444-555555555555",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Ad or buyer not found",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "Ad not found",
                                                              "path": "/api/payments/11111111-2222-3333-4444-555555555555",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> createPayment(@PathVariable UUID adId) {
        PaymentResponseDto paymentResponse = paymentService.createPayment(adId);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentResponse);
    }

    @GetMapping
    @Operation(
            summary = "Get Payment History",
            description = "Retrieves the payment history for the authenticated user. " +
                    "The result is a list of payments made by the user and is cached for performance.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Payment history retrieved successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = PaymentResponseDto.class)
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<?> getPaymentHistory(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentResponseDto> paymentHistory = paymentService.getPaymentHistory(authenticatedUsername, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(paymentHistory);
    }

}
