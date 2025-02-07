package com.discord.bot.maple;

import com.discord.bot.maple.config.BotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.EnumSet;

@SpringBootApplication
//@EnableScheduling
public class MapleApplication implements CommandLineRunner {

    @Autowired
    private BotConfig botConfig;


    public static void main(String[] args) {
        SpringApplication.run(MapleApplication.class, args);
    }


    @Override
    public void run(String... args) throws Exception {

    }
}
