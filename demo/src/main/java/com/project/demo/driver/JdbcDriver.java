package com.project.demo.driver;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URLClassLoader;
import java.sql.Driver;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JdbcDriver {

    private Driver driver;

    private URLClassLoader classLoader;

    private DriverInfo info;
}
