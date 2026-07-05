package com.project.demo.modules.migration.dto;

import java.util.List;

public record MigrationReview(String summary,
                              String riskLevel,
                              List<String> changes,
                              List<String> recommendations,
                              String rollbackQuality,
                              String estimatedImpact) {
}
