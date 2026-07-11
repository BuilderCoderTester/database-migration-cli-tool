package com.project.demo.modules.migration.service;

import com.project.demo.modules.health.dto.ChecksumMismatch;
import com.project.demo.modules.migration.model.ConnectionConfig;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.HexFormat;
import java.util.List;

@Service
public class ChecksumService {
    public String calculate(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public List<ChecksumMismatch> validateChecksums(Connection connection, ConnectionConfig config) {
        return  null;
    }
}