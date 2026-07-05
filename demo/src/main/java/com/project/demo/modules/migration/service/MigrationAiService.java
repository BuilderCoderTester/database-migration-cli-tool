package com.project.demo.modules.migration.service;

import com.project.demo.modules.migration.dto.MigrationReview;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class MigrationAiService {

    private final ChatClient chatClient;

    public MigrationAiService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public MigrationReview review(String sql) {

        String prompt = """
            You are a database migration reviewer.

            Analyze this migration:

            %s

            Return:
            - summary
            - riskLevel
            - changes
            - recommendations
            - rollbackQuality
            - estimatedImpact
            """.formatted(sql);

        return chatClient.prompt(prompt)
                .call()
                .entity(MigrationReview.class);
    }
}