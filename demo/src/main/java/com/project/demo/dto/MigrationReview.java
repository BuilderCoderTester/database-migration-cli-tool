package com.project.demo.dto;

import java.util.List;

public record MigrationReview(String summary,
                              String riskLevel,
                              List<String> changes,
                              List<String> recommendations,
                              String rollbackQuality,
                              String estimatedImpact) {
}
