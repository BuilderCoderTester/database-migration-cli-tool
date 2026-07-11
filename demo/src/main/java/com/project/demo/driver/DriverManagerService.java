package com.project.demo.driver;

import com.project.demo.enumuration.DatabaseType;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.List;

public class DriverManagerService {

    private final DriverRegistry registry = new DriverRegistry();

    private final DriverScanner scanner = new DriverScanner();

    public void loadAllDrivers() throws Exception {

        List<DriverInfo> drivers = scanner.scan();

        for (DriverInfo info : drivers) {

            loadDriver(info);
        }
    }

    private void loadDriver(DriverInfo info) throws Exception {

        URL url = info.getJarPath().toUri().toURL();

        URLClassLoader loader =
                new URLClassLoader(
                        new URL[]{url},
                        getClass().getClassLoader()
                );

        Class<?> clazz =
                Class.forName(
                        info.getDriverClass(),
                        true,
                        loader
                );

        Driver driver =
                (Driver) clazz
                        .getDeclaredConstructor()
                        .newInstance();

        DriverManager.registerDriver(
                new DriverShim(driver)
        );

        info.setLoaded(true);

        registry.register(
                info.getDatabaseType(),
                new JdbcDriver(driver, loader, info)
        );

        System.out.println(
                info.getDatabaseType() +
                        " Driver Loaded."
        );
    }

    public JdbcDriver getDriver(DatabaseType type) {

        return registry.get(type);
    }

    public boolean isInstalled(DatabaseType type) {

        return registry.contains(type);
    }

}
