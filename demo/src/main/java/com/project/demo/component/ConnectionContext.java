package com.project.demo.component;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class ConnectionContext {
    private Long currentConnectionId;
    private String currentDatabase;
    public Long getCurrentConnectionId() {
        return currentConnectionId;
    }

    public void setCurrentConnectionId(Long currentConnectionId) {
        this.currentConnectionId = currentConnectionId;
    }
}
