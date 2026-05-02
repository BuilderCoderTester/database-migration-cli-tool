package com.project.demo.component;

import org.springframework.stereotype.Component;

@Component
public class ConnectionContext {
    private Long currentConnectionId;

    public Long getCurrentConnectionId() {
        return currentConnectionId;
    }

    public void setCurrentConnectionId(Long currentConnectionId) {
        this.currentConnectionId = currentConnectionId;
    }
}
