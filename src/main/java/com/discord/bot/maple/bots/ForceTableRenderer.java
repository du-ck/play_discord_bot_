package com.discord.bot.maple.bots;

import com.discord.bot.maple.bots.ImageRenderer.TableRow;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.discord.bot.maple.bots.ImageRenderer.TableRow.cont;
import static com.discord.bot.maple.bots.ImageRenderer.TableRow.start;

@Service
public class ForceTableRenderer {

    private final ImageRenderer imageRenderer;

    private byte[] cachedArcane;
    private byte[] cachedSacred;

    public ForceTableRenderer(ImageRenderer imageRenderer) {
        this.imageRenderer = imageRenderer;
    }

    @PostConstruct
    public void preRender() {
        cachedArcane = imageRenderer.renderTable("아케인포스 보스", "#9b59b6", "150%", arcaneRows());
        cachedSacred = imageRenderer.renderTable("어센틱포스 보스", "#1abc9c", "125%", sacredRows());
        System.out.println("[ForceTableRenderer] 이미지 캐싱 완료");
    }

    public byte[] renderArcane() { return cachedArcane; }
    public byte[] renderSacred() { return cachedSacred; }

    private List<TableRow> arcaneRows() {
        return List.of(
            start("루시드",      1, "이지/노말/하드",       "360",  "540"),
            start("윌",          2, "이지",                "560",  "840"),
            cont(                   "노말/하드",            "760",  "1140"),
            start("더스크",      1, "노말/카오스",          "730",  "1095"),
            start("진 힐라",     2, "노말",                 "820",  "1230"),
            cont(                   "하드",                 "900",  "1350"),
            start("듄켈",        1, "노말/하드",            "850",  "1275"),
            start("검은 마법사", 1, "하드/익스트림 (110%)", "1320", "1455")
        );
    }

    private List<TableRow> sacredRows() {
        return List.of(
            start("세렌",          2, "1페",       "150", "200"),
            cont(                     "2페",       "200", "250"),
            start("칼로스",        5, "이지",      "200", "250"),
            cont(                     "노말 1페",  "250", "250"),
            cont(                     "노말 2페",  "300", "350"),
            cont(                     "카오스",    "330", "380"),
            cont(                     "익스트림",  "440", "490"),
            start("카링",          4, "이지",      "230", "280"),
            cont(                     "노말",      "330", "380"),
            cont(                     "하드",      "350", "400"),
            cont(                     "익스트림",  "480", "530"),
            start("최초의 대적자", 4, "이지",      "220", "270"),
            cont(                     "노말",      "320", "370"),
            cont(                     "하드",      "340", "390"),
            cont(                     "익스트림",  "460", "510"),
            start("찬란한 흉성",   2, "노말",      "400", "450"),
            cont(                     "하드",      "550", "600"),
            start("림보",          1, "노말/하드", "500", "550"),
            start("발드릭스",      1, "노말/하드", "700", "750"),
            start("유피테르",      1, "노말/하드", "810", "860"),
            start("벨로나",        3, "이지",      "400", "450"),
            cont(                     "노말",      "450", "500"),
            cont(                     "하드",      "550", "600")
        );
    }
}
