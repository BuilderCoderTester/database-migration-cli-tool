package com.project.demo.service;

import com.project.demo.component.MigrationComponent;
import com.project.demo.component.MigrationEngine;
import com.project.demo.component.MigrationLoader;
import com.project.demo.dto.MigrationResult;
import com.project.demo.dto.request.MigrationRequest;
import com.project.demo.model.MigrationScript;
import com.project.demo.repository.MigrationRepository;
import com.project.demo.utility.Helper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MigrationServiceTest {

    @Mock
    private MigrationLockService migrationLockService;
    @Mock private MigrationComponent migrationComponent;
    @Mock private MigrationLoader loader;
    @Mock private Helper helper;
    @Mock private MigrationEngine engine;
    @Mock private MigrationRepository repository;

    @InjectMocks
    private MigrationService migrationService;
    @Test
    void shouldThrowExceptionWhenConnectionIdIsNull() {
        MigrationRequest request = new MigrationRequest(null, null);

        assertThrows(RuntimeException.class, () -> {
            migrationService.migrate(request);
        });
    }

    @Test
    void shouldReturnNoPendingMigrations() throws Exception {

        Long connectionId = 19L;

        when(migrationLockService.acquireLock(any(), any()))
                .thenReturn("LOCKED");

        when(migrationComponent.getCurrentVersion(connectionId))
                .thenReturn(Optional.empty());

        when(loader.loadPendingMigrations(any(), eq(connectionId)))
                .thenReturn(Collections.emptyList());

        MigrationRequest request = new MigrationRequest(connectionId, null);

        MigrationResult result = migrationService.migrate(request);

        assertEquals("✓ No pending migrations", result.getMessage());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailedCount());

        verify(migrationLockService).releaseLock(eq(connectionId), any());
    }

    @Test
    void shouldApplyAllMigrationsSuccessfully() throws Exception {

        Long connectionId = 1L;

        MigrationScript script1 = new MigrationScript("1", "desc1", false);
        MigrationScript script2 = new MigrationScript("2", "desc2", false);

        when(migrationLockService.acquireLock(any(), any()))
                .thenReturn("LOCKED");

        when(migrationComponent.getCurrentVersion(connectionId))
                .thenReturn(Optional.of("0"));

        when(loader.loadPendingMigrations(any(), eq(connectionId)))
                .thenReturn(List.of(script1, script2));

        MigrationRequest request = new MigrationRequest(connectionId, null);

        MigrationResult result = migrationService.migrate(request);

        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailedCount());

        verify(engine, times(2))
                .migrateUp(any(), eq(connectionId));

        verify(migrationLockService)
                .releaseLock(eq(connectionId), any());
    }
}