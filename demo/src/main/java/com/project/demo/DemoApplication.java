package com.project.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		ConsoleLogger.info("Direct execution of DemoApplication detected. Spinning up full Spring stack...");
		SpringApplication.run(DemoApplication.class, args);
		ConsoleLogger.success("DemoApplication server infrastructure is online.");
	}

}