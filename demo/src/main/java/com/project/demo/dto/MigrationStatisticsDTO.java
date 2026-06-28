package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.ResponseBody;

@ResponseBody
@Getter
@Setter
@AllArgsConstructor
public class MigrationStatisticsDTO {
    private int totalRelatedScripts;

    private int successfulScripts;

    private int failedScripts;

    private int pendingScripts;
}
