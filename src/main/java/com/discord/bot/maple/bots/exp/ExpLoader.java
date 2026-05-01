package com.discord.bot.maple.bots.exp;

import com.discord.bot.maple.bots.Util;
import com.opencsv.CSVReader;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExpLoader {

    private static List<ExpTable> expTable = new ArrayList<>();


    public ExpLoader() {
        /*this.loadExpTable();
        System.out.println("EXP 테이블 로드 완료. 총 " + expTable.size() + "개 엔트리");*/
    }

    public static List<ExpTable> getExpTable() {
        return expTable;
    }

    public static void loadExpTable() {
        Util util = new Util();

        try (CSVReader reader = new CSVReader(new FileReader(util.getFile("exp.csv")))) {
            String[] nextLine;
            boolean isHeader = true;
            while ((nextLine = reader.readNext()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String strLevel = nextLine[0];
                long level = Long.parseLong(strLevel);
                String expStr = nextLine[1].replace(",", ""); // 쉼표 제거
                long exp = Long.parseLong(expStr);
                expTable.add(new ExpTable(level, exp));
            }
        } catch (Exception e) {
            e.printStackTrace(); //
        }
    }
}
