package com.example.cashflow.shared.json;

import com.example.cashflow.cashflow.command.domain.CashFlowEntryCreatedEvent;
import com.example.cashflow.eventbus.domain.DomainEvent;
import com.example.cashflow.shared.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class DomainEventJsonSerializer {

    private final ObjectMapper objectMapper;

    public DomainEventJsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("EVENT_SERIALIZATION_ERROR", "Could not serialize domain event.");
        }
    }

    public DomainEvent deserialize(String eventType, String payload) {
        try {
            return switch (eventType) {
                case "CashFlowEntryCreated" -> objectMapper.readValue(payload, CashFlowEntryCreatedEvent.class);
                default -> throw new BusinessException(
                        "UNKNOWN_EVENT_TYPE",
                        "Unsupported domain event type: " + eventType
                );
            };
        } catch (JsonProcessingException exception) {
            throw new BusinessException("EVENT_DESERIALIZATION_ERROR", "Could not deserialize domain event payload.");
        }
    }
}
