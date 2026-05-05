package com.project.demo.component;

import com.project.demo.model.Migration;
import com.project.demo.repository.MigrationRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
public class MigrationComponent {

    @Autowired
    private final MigrationRepository migrationRepository;

    /// get current version of the schema or scripts
    public Optional<String> getCurrentVersion(Long connectionId) {
        return migrationRepository.findLastSuccessful(connectionId).map(Migration::getVersion);
    }
}
