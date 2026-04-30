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

    public void validateBeforeUp(MigrationScript script) {

        if (!repository.existsByDirtyTrue()) {
            throw new RuntimeException("Database is in DIRTY state. Resolve before continuing.");
        }
//        repository.findById(script.getVersion()).ifPresent(existing -> {
//            String newChecksum = checksumService.calculate(script.getUpScript());
//            System.out.println("VERSION " + script.getVersion() + " " + " CHECKSUM " + newChecksum);

            // the string equal function is not returning anything **BUG**
//            System.out.println("EXISTING : " + newChecksum.equals(existing.getChecksum().toString()));

//            if (!Boolean.parseBoolean(existing.getChecksum())) {
//                throw new RuntimeException("Checksum mismatch for version: " + script.getVersion());
//            }
//            throw new RuntimeException("Migration already applied: " + script.getVersion());
//        });
//        Optional<Migration> lastMigration =
//                repository.findTopByOrderByExecutedAtDesc();
//        if(lastMigration.isEmpty()){
//            System.out.println("LAST MIGRATION PRESENT : " + true);
//        }
//
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