package com.discord.bot.maple.bots;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class ImageRenderer {

    public record TableRow(String boss, int span, String diff, String v100, String vtarget) {
        public static TableRow start(String boss, int span, String diff, String v100, String vtarget) {
            return new TableRow(boss, span, diff, v100, vtarget);
        }
        public static TableRow cont(String diff, String v100, String vtarget) {
            return new TableRow(null, 0, diff, v100, vtarget);
        }
    }

    /** jar 에 동봉된 한글 폰트. 서버에 폰트가 깔려있지 않아도 한글이 깨지지 않는다. (OFL 1.1) */
    private static final String BUNDLED_FONT = "fonts/NanumGothic.ttf";

    private String fontFamily = Font.SANS_SERIF;

    @PostConstruct
    public void init() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try (InputStream in = ImageRenderer.class.getClassLoader().getResourceAsStream(BUNDLED_FONT)) {
            if (in == null) {
                System.err.println("[ImageRenderer] 번들 폰트를 찾지 못했습니다: " + BUNDLED_FONT + " — 기본 폰트 사용.");
                return;
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            ge.registerFont(font);
            fontFamily = font.getFamily();
            System.out.println("[ImageRenderer] 폰트 등록: " + font.getFontName());
        } catch (Exception e) {
            System.err.println("[ImageRenderer] 폰트 로드 실패 — 기본 폰트 사용: " + e.getMessage());
        }
    }

    public byte[] renderTable(String title, String accentHex, String targetLabel, List<TableRow> rows) {
        final int W       = 540;
        final int ACCENT  = 4;
        final int TITLE_H = 46;
        final int HDR_H   = 34;
        final int ROW_H   = 34;
        final int PAD     = 16;

        // Column left edges: 4 + 145 + 177 + 108 + 106 = 540
        final int X_BOSS = ACCENT;
        final int X_DIFF = X_BOSS + 145;
        final int X_V100 = X_DIFF + 177;
        final int X_VTGT = X_V100 + 108;

        final int TOTAL_H = TITLE_H + HDR_H + rows.size() * ROW_H;

        Color cBg     = new Color(0x2b, 0x2d, 0x31);
        Color cHdrBg  = new Color(0x23, 0x24, 0x28);
        Color cBorder = new Color(0x3a, 0x3c, 0x41);
        Color cAccent = Color.decode(accentHex);
        Color cNormal = new Color(0xdc, 0xdd, 0xde);
        Color cMuted  = new Color(0x9b, 0x9e, 0xa4);
        Color cDim    = new Color(0xb8, 0xbb, 0xbe);

        Font fTitle  = new Font(fontFamily, Font.BOLD,  16);
        Font fHeader = new Font(fontFamily, Font.BOLD,  11);
        Font fBoss   = new Font(fontFamily, Font.BOLD,  14);
        Font fCell   = new Font(fontFamily, Font.PLAIN, 14);

        BufferedImage img = new BufferedImage(W, TOTAL_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        // Background
        g.setColor(cBg);
        g.fillRect(0, 0, W, TOTAL_H);

        // Accent left border
        g.setColor(cAccent);
        g.fillRect(0, 0, ACCENT, TOTAL_H);

        // ── Title ──
        g.setFont(fTitle);
        g.setColor(Color.WHITE);
        g.drawString(title, X_BOSS + PAD, baseline(0, TITLE_H, g.getFontMetrics()));
        hline(g, cBorder, ACCENT, TITLE_H - 1, W);

        // ── Header ──
        g.setColor(cHdrBg);
        g.fillRect(ACCENT, TITLE_H, W - ACCENT, HDR_H);
        g.setFont(fHeader);
        g.setColor(cMuted);
        FontMetrics fmH = g.getFontMetrics();
        int hY = baseline(TITLE_H, HDR_H, fmH);
        g.drawString("보스",   X_BOSS + PAD, hY);
        g.drawString("난이도", X_DIFF + PAD, hY);
        rightText(g, "100%",       X_V100, X_VTGT, PAD, hY, fmH);
        rightText(g, targetLabel,  X_VTGT, W,      PAD, hY, fmH);
        hline(g, cBorder, ACCENT, TITLE_H + HDR_H - 1, W);

        // ── Column separators (header + rows) ──
        vline(g, cBorder, X_DIFF, TITLE_H, TOTAL_H);
        vline(g, cBorder, X_V100, TITLE_H, TOTAL_H);
        vline(g, cBorder, X_VTGT, TITLE_H, TOTAL_H);

        // ── Rows ──
        int dy = TITLE_H + HDR_H;
        g.setFont(fCell);
        FontMetrics fmC = g.getFontMetrics();

        for (int i = 0; i < rows.size(); i++) {
            TableRow row = rows.get(i);
            int rowTop = dy + i * ROW_H;

            if (i < rows.size() - 1) {
                boolean nextIsCont = rows.get(i + 1).boss() == null;
                int lineStart = nextIsCont ? X_DIFF : ACCENT;
                hline(g, cBorder, lineStart, rowTop + ROW_H - 1, W);
            }

            if (row.boss() != null) {
                g.setFont(fBoss);
                FontMetrics fmB = g.getFontMetrics();
                g.setColor(Color.WHITE);
                g.drawString(row.boss(), X_BOSS + PAD, baseline(rowTop, row.span() * ROW_H, fmB));
                g.setFont(fCell);
            }

            int textY = baseline(rowTop, ROW_H, fmC);
            g.setColor(cDim);
            g.drawString(row.diff(), X_DIFF + PAD, textY);
            g.setColor(cNormal);
            rightText(g, row.v100(),    X_V100, X_VTGT, PAD, textY, fmC);
            rightText(g, row.vtarget(), X_VTGT, W,      PAD, textY, fmC);
        }

        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("PNG 변환 실패", e);
        }
    }

    private int baseline(int top, int height, FontMetrics fm) {
        return top + (height - fm.getHeight()) / 2 + fm.getAscent();
    }

    private void hline(Graphics2D g, Color c, int x1, int y, int x2) {
        g.setColor(c);
        g.fillRect(x1, y, x2 - x1, 1);
    }

    private void vline(Graphics2D g, Color c, int x, int y1, int y2) {
        g.setColor(c);
        g.fillRect(x, y1, 1, y2 - y1);
    }

    private void rightText(Graphics2D g, String text, int colLeft, int colRight, int pad, int y, FontMetrics fm) {
        g.drawString(text, colRight - pad - fm.stringWidth(text), y);
    }
}
