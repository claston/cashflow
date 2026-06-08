package com.example.cashflow.shared.exception;

import java.time.Instant;

public record ErrorResponse(
        String error,
        String message,
        Instant timestamp
) {
}
