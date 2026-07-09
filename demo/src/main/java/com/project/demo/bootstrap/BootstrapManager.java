package com.project.demo.bootstrap;

import com.project.demo.DemoApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class BootstrapManager {

    private static ConfigurableApplicationContext context;

    private BootstrapManager() {}

    public static void startSpring() {

        if (context == null) {

            context =
                    new SpringApplicationBuilder(DemoApplication.class)
                            .web(WebApplicationType.NONE)
                            .run();

        }

    }

    public static void stopSpring() {

        if (context != null) {

            context.close();

            context = null;

        }

    }

    public static ConfigurableApplicationContext getContext() {

        return context;

    }

}