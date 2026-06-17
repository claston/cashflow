package com.example.cashflow.shared.observability;

import static org.assertj.core.api.Assertions.filter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.FilterChain;

import jakarta.servlet.ServletException;

class CorrelationIdFilterTest{

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldUseCorrelationIdFromRequestHeader() throws ServletException, IOException
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(CorrelationIdContext.HEADER_NAME, "test-correlation-001");

        CapturingFilterChain filterChain = new CapturingFilterChain();
        filter.doFilter(request, response, filterChain); 
        
        assertEquals("test-correlation-001", response.getHeader(CorrelationIdContext.HEADER_NAME));
        assertEquals("test-correlation-001", filterChain.capturedCorrelationId);
    }

    void shouldGenerateCorrelationIdWhenHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        CapturingFilterChain filterChain = new CapturingFilterChain();
        filter.doFilter(request, response, filterChain);

        String generatedCorrelaionId = response.getHeader(CorrelationIdContext.HEADER_NAME);

        assertNotNull(generatedCorrelaionId);
        assertEquals(generatedCorrelaionId, filterChain.capturedCorrelationId);
    }

    void shoudClearMdcAfterRequest() throws ServletException, IOException{
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("test-correlation-id-001", CorrelationIdContext.HEADER_NAME);

        filter.doFilter(request, response, new CapturingFilterChain());

        assertEquals(null, MDC.get(CorrelationIdContext.MDC_KEY));
    }

    public static class CapturingFilterChain implements FilterChain {

        private String capturedCorrelationId;

        @Override
        public void doFilter(
            jakarta.servlet.ServletRequest request,
            jakarta.servlet.ServletResponse response
        ){
            this.capturedCorrelationId = MDC.get(CorrelationIdContext.MDC_KEY);
        }
    }
}