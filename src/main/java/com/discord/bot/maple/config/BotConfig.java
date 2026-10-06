package com.discord.bot.maple.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BotConfig {
    @Value("${discord.bot.token}")
    private String botToken;

    @Value("${discord.bot.admin-ids:}")
    private String adminIdsRaw;

    private Set<String> adminIds;

    @PostConstruct
    private void parseAdminIds() {
        adminIds = Arrays.stream(adminIdsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public String getBotToken() {
        return botToken;
    }

    public boolean isAdmin(String userId) {
        return adminIds.contains(userId);
    }
}
