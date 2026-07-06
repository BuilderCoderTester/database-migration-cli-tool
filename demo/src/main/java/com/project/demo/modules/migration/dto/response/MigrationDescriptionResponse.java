package com.project.demo.modules.migration.dto.response;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.*;

import java.sql.Time;
import java.time.LocalDateTime;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MigrationDescriptionResponse {
    private String name;
    private String description;
    private String version;
    private String script;
    private LocalDateTime executedAt;
    private boolean success;
    private long executionTime;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.set(value);
    }
}
