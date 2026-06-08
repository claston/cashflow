package com.example.cashflow.diagnostics.application;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class ProjectionFailureModeService {

    private final AtomicBoolean enabled = new AtomicBoolean(false);

    public boolean isEnabled() {
        return enabled.get();
    }

    public boolean setEnabled(boolean newValue) {
        enabled.set(newValue);
        return enabled.get();
    }
}
