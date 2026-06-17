package com.example.cashflow.shared.observability;

public final class CorrelationIdContext {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private CorrelationIdContext(){
    }
    
}
