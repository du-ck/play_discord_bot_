package com.discord.bot.maple.config;

import com.discord.bot.maple.bots.CoreCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BotConfig {
    @Value("${discord.bot.token}")
    private String botToken;

    public String getBotToken() {
        return botToken;
    }
}