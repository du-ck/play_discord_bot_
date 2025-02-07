package com.discord.bot.maple.bots;

import java.io.File;

public class Util {
    private static Util instance;
    private Util() {

    }

    public static Util getInstance() {
        if (instance == null) {
            instance = new Util();
        }
        return instance;
    }
    public final String path = "/home/opc/bot/resource";

    public File getFile(String fileName) {
        File file = new File(path, fileName);
        if (file.exists() && file.isFile()) {
            return file;
        } else {
            throw new IllegalArgumentException("File not found: " + file.getAbsolutePath());
        }
    }
}
