package com.project.demo.modules.health.dto;

public record ChecksumMismatch(String version,

                               String expectedChecksum,

                               String actualChecksum
) {
    public char[] getVersion() {
        return new char[0];
    }
}
