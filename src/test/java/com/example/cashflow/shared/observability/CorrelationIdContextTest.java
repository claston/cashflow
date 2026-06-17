package com.example.cashflow.shared.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CorrelationIdContextTest {

    @Test
    void shouldExposeCorrelationIdHeaderName(){
        assertEquals("X-Correlation-Id", CorrelationIdContext.HEADER_NAME);
    }
    
    void shouldExposeMDCKey(){
        assertEquals("correlationId", CorrelationIdContext.MDC_KEY);
    }
}
