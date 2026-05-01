package com.discord.bot.maple.bots;

import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class Util {

    public final String path = "/home/opc/bot/resource";
    //public final String path = "\\javaProject\\github\\discord_bot\\bot\\play_discord_bot_\\src\\main\\resources";

    public File getFile(String fileName) {
        File file = new File(path, fileName);
        if (file.exists() && file.isFile()) {
            return file;
        } else {
            throw new IllegalArgumentException("File not found: " + file.getAbsolutePath());
        }
    }

    /**
     * HHmm 형태의 문자열값을
     * 앞 2자리 HH → 00~23
     * 뒤 2자리 mm → 00~59
     * 유효성검사 로직
     */
    public static boolean isValidHHmm(String input) {
        if (input == null || input.length() != 4 || !input.matches("\\d{4}")) {
            return false;
        }

        int hh = Integer.parseInt(input.substring(0, 2));
        int mm = Integer.parseInt(input.substring(2, 4));

        return hh >= 0 && hh <= 23 && mm >= 0 && mm <= 59;
    }
}
