package com.project.demo.dto;

import com.project.demo.enumuration.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Getter
@Setter
@RequiredArgsConstructor
public class MigrationScriptStatus {
    private Status status;
    private String tableName;

}
