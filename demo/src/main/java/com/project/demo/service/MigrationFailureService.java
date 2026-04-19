package com.project.demo.service;

import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.repository.MigrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MigrationFailureService {

    private final MigrationRepository repository;

    public MigrationFailureService(MigrationRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(MigrationScript script, Exception e) {

        Migration migration = new Migration();

        migration.setVersion(script.getVersion());
        migration.setDescription(script.getDescription());
        migration.setScript(script.getUpScript()); // ✅ FIXED

        migration.setExecutedAt(LocalDateTime.now());
        migration.setSuccess(false);

        // ✅ FIXED naming
        migration.setErrorMessage(e.getMessage());

        // ✅ stack trace method
        migration.setErrorStackTrace(getStackTrace(e));

        // ✅ mark DB dirty
        migration.setDirty(true);

        // ✅ retry logic
        Optional<Migration> existing = repository.findById(script.getVersion());
        int retry = existing.map(Migration::getRetryCount).orElse(0);
        migration.setRetryCount(retry + 1);

        repository.save(migration);
    }

    // ✅ Utility method moved here
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}