package com.project.demo.config;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.shell.command.annotation.EnableCommand;

@Configuration
@EnableCommand
public class ShellConfig {

    @Bean
    @Primary
    public Terminal terminalConfig() throws Exception {
        return TerminalBuilder.builder()
                .system(true)
                .build();
    }
}