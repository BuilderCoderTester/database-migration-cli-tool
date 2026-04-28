package com.project.demo.component;

import com.project.demo.model.Migration;
import com.project.demo.model.MigrationScript;
import com.project.demo.repository.MigrationRepository;
import com.project.demo.service.ChecksumService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MigrationValidator {
    private final MigrationRepository repository;
    private final ChecksumService checksumService;

    public Boolean validateBeforeUp(MigrationScript script) {

        if (!repository.existsByDirtyTrue()) {
            System.out.println("break at the DirtyDb");
            throw new RuntimeException("Database is in DIRTY state. Resolve before continuing.");
        }
        System.out.println("running--after checking dirt");
//        repository.findById(script.getVersion()).ifPresent(existing -> {
//            System.out.println("break at the find");
//            String newChecksum = checksumService.calculate(script.getUpScript());
//            System.out.println("the checksum " + newChecksum);
//            if (!existing.getChecksum().equals(newChecksum)) {
//                throw new RuntimeException("Checksum mismatch for version: " + script.getVersion());
//            }
//            throw new RuntimeException("Migration already applied: " + script.getVersion());
//        });
        System.out.println("point -1 ");
//        Optional<Migration> lastMigration =
//                repository.findTopByOrderByExecutedAtDesc();
        System.out.println("point -2 ");

//        if (lastMigration.isPresent()) {
//
//            long last = extractVersionNumber(lastMigration.get().getVersion());
//            long current = extractVersionNumber(script.getVersion());
//
//            if (current <= last) {
//                throw new RuntimeException(
//                        "Out-of-order migration. Current: " + script.getVersion() +
//                                ", Last applied: " + lastMigration.get().getVersion()
//                );
//            }
//
//            // Optional strict sequence enforcement
//            if (current != last + 1) {
//                throw new RuntimeException(
//                        "Missing migration(s). Expected: V" + (last + 1) +
//                                " but found: " + script.getVersion()
//                );
//            }
//        }
        return true;
    }

    public boolean validateDirtyDb() {
        if (repository.existsByDirtyTrue()) {
            throw new RuntimeException("Database is in DIRTY state. Resolve before continuing.");
        } else {
            return true;
        }
    }

    private long extractVersionNumber(String version) {
        return Long.parseLong(version.replaceAll("\\D", ""));
    }
}