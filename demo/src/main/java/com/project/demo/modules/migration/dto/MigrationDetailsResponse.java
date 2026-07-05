package com.project.demo.modules.migration.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@ResponseBody
@Getter
@Setter
@AllArgsConstructor
public class MigrationDetailsResponse {
    private MigrationDetailsDTO migration;

    private List<RelatedScriptDTO> relatedScripts;

    private MigrationStatisticsDTO statistics;
}
