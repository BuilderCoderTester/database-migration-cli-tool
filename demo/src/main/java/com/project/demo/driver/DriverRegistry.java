package com.project.demo.driver;

import com.project.demo.enumuration.DatabaseType;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DriverRegistry {
    private final Map<DatabaseType, JdbcDriver> registry =
            new ConcurrentHashMap<>();

    public void register(DatabaseType type,
                         JdbcDriver driver) {

        registry.put(type, driver);
    }

    public JdbcDriver get(DatabaseType type) {

        return registry.get(type);
    }

    public boolean contains(DatabaseType type) {

        return registry.containsKey(type);
    }

    public Collection<JdbcDriver> getAll() {

        return registry.values();
    }

    public void remove(DatabaseType type) {

        registry.remove(type);
    }
}
