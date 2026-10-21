package com.example.menumod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MenuScreen extends Screen {
    static final String[] NAMES = {"Gracze", "Ustawienia", "Mody", "Profil"};
    static final Item[] ICONS = {Items.PLAYER_HEAD, Items.COMPARATOR, Items.CHEST, Items.NAME_TAG};
    static final String[] DAYS = {"Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd"};

    // wymiary (w "wirtualnych" pikselach GUI)
    static final int W = 440, H = 280, SB = 112;
    static final int ITEM_H = 30, ITEM_PITCH = 34, ITEM_Y0 = 40;

    // kolory
    static final int BG = 0xF2101318;
    static final int SIDE = 0xFF161A20;
    static final int CARD = 0xFF1B2027;
    static final int CARD_HOVER = 0xFF242B34;
    static final int INSET = 0xFF14181D;
    static final int TEXT = 0xFFFFFFFF;
    static final int DIM = 0xFF8A94A3;
    static final int OFF = 0xFF39414C;
    static final int POPUP = 0xFF161A20;

    // geometria popupu z pickerem
    static final int PW = 188, PH = 154;

    private final long openedAt = Util.getMeasuringTimeMs();
    private long lastFrame = openedAt;
    private long tabAt = openedAt;

    private int tab = 1;
    private float selY;       // pozycja "szkła" względem py
    private float fit = 1f;   // skalowanie, gdy okno jest za małe
    private int px, py;
    private double mx, my;    // myszka w wirtualnych współrzędnych
    private int accent = 0xFF3B82F6;
    private int scroll = 0;
    private int playersMax = 0;

    // picker koloru
    private boolean pickerOpen, hexFocused, bindingKey;
    private float hh, hs, hv;
    private String hex = "";
    private int drag = 0; // 1 = pole SV, 2 = suwak hue

    public MenuScreen() {
        super(Text.literal("MenuMod"));
        selY = ITEM_Y0 + tab * ITEM_PITCH;
        play(MenuConfig.I.openSound, 1.0f);
    }

    // ------------------------------------------------------------ dźwięk

    static void play(String id, float pitch) {
        if (!MenuConfig.I.sounds) return;
        try {
            MinecraftClient.getInstance().getSoundManager()
                    .play(PositionedSoundInstance.master(SoundEvent.of(Identifier.of(id)), pitch));
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------ kolory

    static int alpha(int col, int a) {
        return (a << 24) | (col & 0xFFFFFF);
    }

    static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    static int shade(int rgb, float f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * f));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * f));
        int b = Math.min(255, (int) ((rgb & 255) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static int hsv(float h, float s, float v) {
        float r, g, b;
        int i = (int) (h * 6f);
        float f = h * 6f - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | ((int) (r * 255 + 0.5f) << 16) | ((int) (g * 255 + 0.5f) << 8) | (int) (b * 255 + 0.5f);
    }

    static float[] toHsv(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min, h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) % 6f;
        else if (max == g) h = (b - r) / d + 2f;
        else h = (r - g) / d + 4f;
        h /= 6f;
        if (h < 0) h += 1f;
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }

    static float ease(float t) {
        return 1f - (float) Math.pow(1f - t, 3);
    }

    // ------------------------------------------------------------ rysowanie kształtów

    static boolean rounded() {
        return MenuConfig.I.rounded;
    }

    /** Zaokrąglony prostokąt (bez nakładania się pikseli, więc działa z przezroczystością). */
    static void rr(DrawContext c, int x, int y, int w, int h, int r, int col) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (!rounded() || r <= 0) {
            c.fill(x, y, x + w, y + h, col);
            return;
        }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, col);
            c.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, col);
        }
        c.fill(x, y + r, x + w, y + h - r, col);
    }

    /** Zamaskowanie rogów kolorem tła (do zaokrąglenia gradientów). */
    static void mask(DrawContext c, int x, int y, int w, int h, int r, int bg) {
        r = Math.min(r, Math.min(w, h) / 2);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            if (inset <= 0) continue;
            c.fill(x, y + i, x + inset, y + i + 1, bg);
            c.fill(x + w - inset, y + i, x + w, y + i + 1, bg);
            c.fill(x, y + h - 1 - i, x + inset, y + h - i, bg);
            c.fill(x + w - inset, y + h - 1 - i, x + w, y + h - i, bg);
        }
    }

    static void ellipse(DrawContext c, int cxm, int cym, int rw, int rh, int col) {
        for (int dy = -rh; dy <= rh; dy++) {
            double f = Math.sqrt(1.0 - (dy * dy) / (double) (rh * rh));
            int half = (int) Math.round(rw * f);
            c.fill(cxm - half, cym + dy, cxm + half, cym + dy + 1, col);
        }
    }

    private void toggle(DrawContext c, int x, int y, boolean on) {
        rr(c, x, y, 28, 14, 7, on ? accent : OFF);
        rr(c, on ? x + 16 : x + 2, y + 2, 10, 10, 5, TEXT);
    }

    private void small(DrawContext c, String s, int x, int y, int col, float scale) {
        var m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        c.drawText(textRenderer, s, 0, 0, col, false);
        m.pop();
    }

    private void centered(DrawContext c, String s, int x, int y, int w, int col) {
        c.drawText(textRenderer, s, x + (w - textRenderer.getWidth(s)) / 2, y, col, false);
    }

    private boolean hov(int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static boolean hit(double x, double y, int rx, int ry, int rw, int rh) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }

    // ------------------------------------------------------------ layout (wspólny dla render i kliknięć)

    private int cx() { return px + 6 + SB + 14; }
    private int cw() { return W - 6 - SB - 14 - 12; }
    private int cy() { return py + 36; }

    private int setRowY(int i) { return cy() + 2 + i * 32; }
    private int bindRowY() { return cy() + 114; }

    private int ppx() { return px + W - PW - 14; }
    private int ppy() { return py + 68; }

    // ------------------------------------------------------------ picker: stan

    private int pickRgb() {
        return hsv(hh, hs, hv) & 0xFFFFFF;
    }

    private void syncHex() {
        hex = String.format("%06X", pickRgb());
    }

    private void openPicker() {
        float[] a = toHsv(MenuConfig.I.guiColor & 0xFFFFFF);
        hh = Math.min(a[0], 0.9999f);
        hs = a[1];
        hv = a[2];
        syncHex();
        pickerOpen = true;
        hexFocused = false;
    }

    private void closePicker(boolean save) {
        if (save) {
            MenuConfig.I.guiColor = 0xFF000000 | pickRgb();
            MenuConfig.save();
        }
        pickerOpen = false;
        hexFocused = false;
        drag = 0;
    }

    private void setSV(double x, double y) {
        hs = (float) Math.max(0, Math.min(1, (x - (ppx() + 10)) / 167.0));
        hv = 1f - (float) Math.max(0, Math.min(1, (y - (ppy() + 10)) / 79.0));
        syncHex();
    }

    private void setHue(double x) {
        hh = (float) Math.max(0, Math.min(0.9999, (x - (ppx() + 10)) / 167.0));
        syncHex();
    }

    // ------------------------------------------------------------ render

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;

        fit = Math.min(1f, Math.min((width - 8f) / W, (height - 8f) / H));
        float vw = width / fit, vh = height / fit;
        mx = mouseX / fit;
        my = mouseY / fit;
        px = (int) ((vw - W) / 2f);
        py = (int) ((vh - H) / 2f);
        accent = pickerOpen ? (0xFF000000 | pickRgb()) : MenuConfig.I.guiColor;

        float t = Math.min(1f, (now - openedAt) / 250f);
        float e = ease(t);

        // przyciemnienie tła (fade-in)
        c.fill(0, 0, width, height, ((int) (110 * e)) << 24);

        // animacja "szkła": płynnie leci do wybranej ikony
        float target = ITEM_Y0 + tab * ITEM_PITCH;
        selY += (target - selY) * (1f - (float) Math.exp(-dt * 16f));
        if (Math.abs(target - selY) < 0.05f) selY = target;

        var m = c.getMatrices();
        m.push();
        m.scale(fit, fit, 1f);
        m.translate(vw / 2f, vh / 2f, 0f);
        float s = 0.85f + 0.15f * e;
        m.scale(s, s, 1f);
        m.translate(-vw / 2f, -vh / 2f, 0f);

        // panel i sidebar
        rr(c, px, py, W, H, 10, BG);
        rr(c, px + 6, py + 6, SB, H - 12, 8, SIDE);
        c.drawText(textRenderer, Text.literal("MenuMod").formatted(Formatting.BOLD), px + 18, py + 18, accent, false);

        // ikony + nazwy po lewej
        int ex = px + 12, ew = SB - 12;
        for (int i = 0; i < 4; i++) {
            int y = py + ITEM_Y0 + i * ITEM_PITCH;
            if (i != tab && hov(ex, y, ew, ITEM_H)) {
                rr(c, ex, y, ew, ITEM_H, 8, 0x18FFFFFF);
            }
        }

        // szkło w kolorze GUI (rozciąga się podczas lotu)
        float dist = target - selY;
        float stretch = Math.min(12f, Math.abs(dist) * 0.3f);
        float top = py + selY - (dist > 0 ? stretch : 0);
        float bot = py + selY + ITEM_H + (dist < 0 ? stretch : 0);
        int gt = Math.round(top), gh = Math.round(bot - top);
        rr(c, ex, gt, ew, gh, 8, alpha(accent, 0x66));
        c.fillGradient(ex + 3, gt + 1, ex + ew - 3, gt + Math.max(2, gh / 2), 0x38FFFFFF, 0x00FFFFFF);
        c.fill(ex + 6, gt, ex + ew - 6, gt + 1, 0x66FFFFFF);

        for (int i = 0; i < 4; i++) {
            int y = py + ITEM_Y0 + i * ITEM_PITCH;
            c.drawItem(new ItemStack(ICONS[i]), ex + 7, y + 7);
            c.drawText(textRenderer, NAMES[i], ex + 30, y + 11, i == tab ? TEXT : DIM, false);
        }

        // nagłówek treści
        int cx = cx(), cw = cw(), cy = cy();
        c.drawText(textRenderer, Text.literal(NAMES[tab]).formatted(Formatting.BOLD), cx, py + 15, accent, false);
        c.fill(cx, py + 28, cx + cw, py + 29, 0x22FFFFFF);

        // treść zakładki (lekko wjeżdża od dołu)
        float te = ease(Math.min(1f, (now - tabAt) / 220f));
        m.push();
        m.translate(0f, (1f - te) * 8f, 0f);
        switch (tab) {
            case 0 -> drawPlayers(c, cx, cy, cw);
            case 1 -> drawSettings(c, cx, cy, cw);
            case 2 -> drawMods(c, cx, cy, cw);
            default -> drawProfile(c, cx, cy, cw, now);
        }
        m.pop();

        // picker na wierzchu
        if (pickerOpen) {
            m.push();
            m.translate(0f, 0f, 200f);
            drawPicker(c, now);
            m.pop();
        }

        m.pop();
    }

    // ------------------------------------------------------------ zakładka: Gracze

    private void drawPlayers(DrawContext c, int cx, int cy, int cw) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<AbstractClientPlayerEntity> list = new ArrayList<>();
        int sameChunk = 0;
        if (mc.world != null && mc.player != null) {
            for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
                if (p != mc.player) list.add(p);
            }
            list.sort(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p)));
            for (AbstractClientPlayerEntity p : list) {
                if (p.getChunkPos().equals(mc.player.getChunkPos())) sameChunk++;
            }
        }

        c.drawText(textRenderer, "W zasięgu: " + list.size() + "   Na Twoim chunku: " + sameChunk,
                cx, cy + 2, DIM, false);

        if (list.isEmpty()) {
            centered(c, "Nikogo nie ma w pobliżu.", cx, cy + 90, cw, DIM);
            return;
        }

        int rowH = 26, gap = 4, visible = 7;
        playersMax = Math.max(0, list.size() - visible);
        scroll = Math.max(0, Math.min(scroll, playersMax));

        for (int i = 0; i < Math.min(visible, list.size() - scroll); i++) {
            AbstractClientPlayerEntity p = list.get(scroll + i);
            int ry = cy + 16 + i * (rowH + gap);
            rr(c, cx, ry, cw - 8, rowH, 6, hov(cx, ry, cw - 8, rowH) ? CARD_HOVER : CARD);
            PlayerSkinDrawer.draw(c, p.getSkinTextures(), cx + 5, ry + 4, 18);
            c.drawText(textRenderer, p.getName().getString(), cx + 30, ry + 9, TEXT, false);

            String d = Math.round(mc.player.distanceTo(p)) + " m";
            int dw = textRenderer.getWidth(d);
            int right = cx + cw - 8 - 8;
            c.drawText(textRenderer, d, right - dw, ry + 9, DIM, false);

            if (p.getChunkPos().equals(mc.player.getChunkPos())) {
                int chipW = textRenderer.getWidth("CHUNK") + 10;
                int chipX = right - dw - 6 - chipW;
                rr(c, chipX, ry + 6, chipW, 14, 7, alpha(accent, 0x66));
                c.drawText(textRenderer, "CHUNK", chipX + 5, ry + 9, TEXT, false);
            }
        }

        if (playersMax > 0) {
            int trackH = visible * (rowH + gap) - gap;
            int thumbH = Math.max(14, trackH * visible / list.size());
            int thumbY = cy + 16 + (trackH - thumbH) * scroll / playersMax;
            rr(c, cx + cw - 4, cy + 16, 3, trackH, 1, 0x22FFFFFF);
            rr(c, cx + cw - 4, thumbY, 3, thumbH, 1, alpha(accent, 0xCC));
        }
    }

    // ------------------------------------------------------------ zakładka: Ustawienia

    private void drawSettings(DrawContext c, int cx, int cy, int cw) {
        MenuConfig cfg = MenuConfig.I;

        // kolor GUI
        int y = setRowY(0);
        rr(c, cx, y, cw, 26, 8, hov(cx, y, cw, 26) ? CARD_HOVER : CARD);
        c.drawText(textRenderer, "Kolor GUI", cx + 10, y + 9, TEXT, false);
        String hx = String.format("#%06X", accent & 0xFFFFFF);
        c.drawText(textRenderer, hx, cx + cw - 58 - textRenderer.getWidth(hx), y + 9, DIM, false);
        rr(c, cx + cw - 52, y + 5, 42, 16, 6, 0xFF000000 | (accent & 0xFFFFFF));

        // zaokrąglone ramki
        y = setRowY(1);
        rr(c, cx, y, cw, 26, 8, hov(cx, y, cw, 26) ? CARD_HOVER : CARD);
        c.drawText(textRenderer, "Zaokrąglone ramki", cx + 10, y + 9, TEXT, false);
        toggle(c, cx + cw - 38, y + 6, cfg.rounded);

        // dźwięki
        y = setRowY(2);
        rr(c, cx, y, cw, 26, 8, hov(cx, y, cw, 26) ? CARD_HOVER : CARD);
        c.drawText(textRenderer, "Dźwięki menu", cx + 10, y + 9, TEXT, false);
        toggle(c, cx + cw - 38, y + 6, cfg.sounds);

        // bindy
        c.drawText(textRenderer, "Bindy modułów", cx, bindRowY() - 14, DIM, false);
        y = bindRowY();
        rr(c, cx, y, cw, 26, 8, CARD);
        c.drawText(textRenderer, "Brak cieni", cx + 10, y + 9, TEXT, false);
        int bx = cx + cw - 84, by = y + 3;
        boolean bh = hov(bx, by, 76, 20);
        rr(c, bx, by, 76, 20, 6, bindingKey ? alpha(accent, 0xAA) : (bh ? 0xFF48515E : OFF));
        String label;
        if (bindingKey) label = "naciśnij...";
        else if (cfg.noShadowsKey <= 0) label = "—";
        else label = InputUtil.Type.KEYSYM.createFromCode(cfg.noShadowsKey).getLocalizedText().getString();
        centered(c, label, bx, by + 6, 76, TEXT);

        small(c, "Klik w przycisk, potem klawisz. Backspace czyści, Esc anuluje.", cx, bindRowY() + 34, DIM, 0.8f);
        small(c, "Dźwięk otwarcia zmienisz w config/menumod.json (openSound).", cx, bindRowY() + 46, DIM, 0.8f);
    }

    // ------------------------------------------------------------ zakładka: Mody

    private void drawMods(DrawContext c, int cx, int cy, int cw) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean on = MenuConfig.I.noShadows;

        // karta modułu
        rr(c, cx, cy + 2, cw, 36, 8, hov(cx, cy + 2, cw, 36) ? CARD_HOVER : CARD);
        c.drawText(textRenderer, Text.literal("Brak cieni").formatted(Formatting.BOLD), cx + 10, cy + 9, TEXT, false);
        small(c, "Wyłącza cieniowanie bloków i cienie bytów", cx + 10, cy + 23, DIM, 0.8f);
        toggle(c, cx + cw - 38, cy + 13, on);

        // podgląd
        int pw = (cw - 8) / 2, ph = 108, py2 = cy + 46;
        String st = on ? "WYŁ." : "WŁ.";
        int stCol = on ? 0xFFEF4444 : 0xFF22C55E;

        // --- bloki
        int x1 = cx;
        rr(c, x1, py2, pw, ph, 8, INSET);
        small(c, "Bloki", x1 + 8, py2 + 7, DIM, 0.9f);
        int mid = x1 + pw / 2;
        int base = 0x9AA6B2;
        c.fill(mid - 24, py2 + 28, mid + 24, py2 + 41, shade(base, 1.0f));
        c.fill(mid - 24, py2 + 42, mid - 1, py2 + 68, shade(base, on ? 1.0f : 0.80f));
        c.fill(mid + 1, py2 + 42, mid + 24, py2 + 68, shade(base, on ? 1.0f : 0.60f));
        c.drawText(textRenderer, "Cienie bloków:", x1 + 8, py2 + ph - 16, DIM, false);
        c.drawText(textRenderer, st, x1 + pw - 8 - textRenderer.getWidth(st), py2 + ph - 16, stCol, false);

        // --- byty (entity)
        int x2 = cx + pw + 8;
        rr(c, x2, py2, pw, ph, 8, INSET);
        small(c, "Byty (gracz, moby)", x2 + 8, py2 + 7, DIM, 0.9f);
        int mid2 = x2 + pw / 2;
        if (!on) {
            ellipse(c, mid2, py2 + 70, 22, 5, 0x88000000);
        }
        if (mc.player != null) {
            PlayerSkinDrawer.draw(c, mc.player.getSkinTextures(), mid2 - 18, py2 + 28, 36);
        }
        c.drawText(textRenderer, "Cienie bytów:", x2 + 8, py2 + ph - 16, DIM, false);
        c.drawText(textRenderer, st, x2 + pw - 8 - textRenderer.getWidth(st), py2 + ph - 16, stCol, false);
    }

    // ------------------------------------------------------------ zakładka: Profil

    static String fmtHms(long sec) {
        return String.format("%02d:%02d:%02d", sec / 3600, (sec / 60) % 60, sec % 60);
    }

    static String fmtHm(long sec) {
        if (sec >= 3600) return (sec / 3600) + "h " + ((sec / 60) % 60) + "m";
        return (sec / 60) + "m";
    }

    private void drawProfile(DrawContext c, int cx, int cy, int cw, long now) {
        MinecraftClient mc = MinecraftClient.getInstance();
        MenuConfig cfg = MenuConfig.I;
        LocalDate today = LocalDate.now();

        long[] days = new long[7];
        long week = 0, max = 0;
        for (int i = 0; i < 7; i++) {
            days[i] = cfg.playSeconds.getOrDefault(today.minusDays(6 - i).toString(), 0L);
            week += days[i];
            max = Math.max(max, days[i]);
        }

        // karta z głową i nickiem
        rr(c, cx, cy + 2, cw, 74, 8, CARD);
        if (mc.player != null) {
            PlayerSkinDrawer.draw(c, mc.player.getSkinTextures(), cx + 10, cy + 11, 56);
        } else {
            rr(c, cx + 10, cy + 11, 56, 56, 6, OFF);
        }
        String name = mc.getSession().getUsername();
        c.drawText(textRenderer, Text.literal(name).formatted(Formatting.BOLD), cx + 76, cy + 12, TEXT, false);
        c.drawText(textRenderer, "Nick: " + name, cx + 76, cy + 26, DIM, false);
        long sess = (System.currentTimeMillis() - MenuModClient.SESSION_START) / 1000;
        c.drawText(textRenderer, "Sesja: " + fmtHms(sess), cx + 76, cy + 41, TEXT, false);
        c.drawText(textRenderer, "Dziś: " + fmtHm(days[6]) + "   Tydzień: " + fmtHm(week), cx + 76, cy + 55, DIM, false);

        // wykres: czas gry w ostatnich 7 dniach
        int gy = cy + 82;
        rr(c, cx, gy, cw, 104, 8, CARD);
        c.drawText(textRenderer, "Czas gry — ostatnie 7 dni", cx + 10, gy + 8, DIM, false);
        int slot = (cw - 28) / 7;
        int baseY = gy + 104 - 18;
        for (int i = 0; i < 7; i++) {
            int bx = cx + 14 + i * slot + (slot - 22) / 2;
            int bh = max > 0 ? Math.max(2, (int) (days[i] * 48f / max)) : 2;
            boolean isToday = i == 6;
            rr(c, bx, baseY - bh, 22, bh, 4, days[i] == 0 ? OFF : (isToday ? accent : alpha(accent, 0x99)));
            if (days[i] > 0) {
                String v = fmtHm(days[i]);
                small(c, v, bx + 11 - (int) (textRenderer.getWidth(v) * 0.7f / 2f), baseY - bh - 9, DIM, 0.7f);
            }
            String dn = DAYS[today.minusDays(6 - i).getDayOfWeek().getValue() - 1];
            centered(c, dn, bx - 4, baseY + 5, 30, isToday ? TEXT : DIM);
        }

        // tagi (miejsce na przyszłe)
        int ty = cy + 194;
        rr(c, cx, ty, cw, 30, 8, CARD);
        c.drawText(textRenderer, "Tagi", cx + 10, ty + 11, DIM, false);
        int chipX = cx + 44;
        if (cfg.tags.isEmpty()) {
            String ph = "miejsce na tagi";
            int chipW = textRenderer.getWidth(ph) + 12;
            rr(c, chipX, ty + 7, chipW, 16, 8, 0x22FFFFFF);
            c.drawText(textRenderer, ph, chipX + 6, ty + 11, DIM, false);
        } else {
            for (String tag : cfg.tags) {
                int chipW = textRenderer.getWidth(tag) + 12;
                if (chipX + chipW > cx + cw - 8) break;
                rr(c, chipX, ty + 7, chipW, 16, 8, alpha(accent, 0x66));
                c.drawText(textRenderer, tag, chipX + 6, ty + 11, TEXT, false);
                chipX += chipW + 6;
            }
        }
    }

    // ------------------------------------------------------------ picker koloru

    private void drawPicker(DrawContext c, long now) {
        int ppx = ppx(), ppy = ppy();
        rr(c, ppx - 1, ppy - 1, PW + 2, PH + 2, 9, 0xFF2A313B);
        rr(c, ppx, ppy, PW, PH, 8, POPUP);

        // pole nasycenie / jasność
        int sx = ppx + 10, sy = ppy + 10, sw = 168, sh = 80;
        for (int i = 0; i < sw; i++) {
            float satur = i / (float) (sw - 1);
            c.fillGradient(sx + i, sy, sx + i + 1, sy + sh, hsv(hh, satur, 1f), 0xFF000000);
        }
        mask(c, sx, sy, sw, sh, 4, POPUP);

        // suwak hue
        int hx = ppx + 10, hy = ppy + 100, hw = 168, hhh = 10;
        for (int i = 0; i < hw; i++) {
            c.fill(hx + i, hy, hx + i + 1, hy + hhh, hsv(i / (float) (hw - 1) * 0.9999f, 1f, 1f));
        }
        mask(c, hx, hy, hw, hhh, 5, POPUP);

        // znaczniki
        int mxp = sx + Math.round(hs * (sw - 1));
        int myp = sy + Math.round((1f - hv) * (sh - 1));
        rr(c, mxp - 4, myp - 4, 9, 9, 4, TEXT);
        rr(c, mxp - 3, myp - 3, 7, 7, 3, 0xFF000000 | pickRgb());
        int hxp = hx + Math.round(hh * (hw - 1));
        rr(c, hxp - 2, hy - 2, 5, hhh + 4, 2, TEXT);

        // pole hex
        int fx = ppx + 10, fy = ppy + 122;
        if (hexFocused) rr(c, fx - 1, fy - 1, 102, 24, 7, accent);
        rr(c, fx, fy, 100, 22, 6, 0xFF0F1318);
        rr(c, fx + 4, fy + 4, 14, 14, 5, 0xFF000000 | pickRgb());
        String shown = "#" + hex;
        c.drawText(textRenderer, shown, fx + 24, fy + 7, TEXT, false);
        if (hexFocused && (now / 500) % 2 == 0 && hex.length() < 6) {
            int cxp = fx + 24 + textRenderer.getWidth(shown);
            c.fill(cxp, fy + 6, cxp + 1, fy + 16, TEXT);
        }

        // przycisk Gotowe
        int bx = ppx + 118, by = ppy + 122;
        boolean bh = hov(bx, by, 60, 22);
        int pc = 0xFF000000 | pickRgb();
        rr(c, bx, by, 60, 22, 6, bh ? mix(pc, 0xFFFFFFFF, 0.2f) : pc);
        centered(c, "Gotowe", bx, by + 7, 60, TEXT);
    }

    // ------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = mouseX / fit, y = mouseY / fit;
        mx = x;
        my = y;
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        MenuConfig cfg = MenuConfig.I;
        int cx = cx(), cw = cw(), cy = cy();

        // klawisz bindu: klik gdziekolwiek anuluje
        if (bindingKey) {
            bindingKey = false;
            return true;
        }

        // picker
        if (pickerOpen) {
            int ppx = ppx(), ppy = ppy();
            if (hit(x, y, ppx + 118, ppy + 122, 60, 22)) {
                play(cfg.clickSound, 1.2f);
                closePicker(true);
                return true;
            }
            if (hit(x, y, ppx + 10, ppy + 10, 168, 80)) {
                drag = 1;
                hexFocused = false;
                setSV(x, y);
                return true;
            }
            if (hit(x, y, ppx + 10, ppy + 96, 168, 18)) {
                drag = 2;
                hexFocused = false;
                setHue(x);
                return true;
            }
            if (hit(x, y, ppx + 10, ppy + 122, 100, 22)) {
                hexFocused = true;
                return true;
            }
            if (hit(x, y, ppx, ppy, PW, PH)) {
                hexFocused = false;
                return true;
            }
            // klik poza popupem = anuluj (bez zapisu)
            closePicker(false);
            return true;
        }

        // zakładki po lewej
        for (int i = 0; i < 4; i++) {
            if (hit(x, y, px + 12, py + ITEM_Y0 + i * ITEM_PITCH, SB - 12, ITEM_H)) {
                if (i != tab) {
                    tab = i;
                    tabAt = Util.getMeasuringTimeMs();
                    scroll = 0;
                    play(cfg.clickSound, 1.0f);
                }
                return true;
            }
        }

        if (tab == 1) {
            if (hit(x, y, cx, setRowY(0), cw, 26)) {
                play(cfg.clickSound, 1.0f);
                openPicker();
                return true;
            }
            if (hit(x, y, cx, setRowY(1), cw, 26)) {
                cfg.rounded = !cfg.rounded;
                MenuConfig.save();
                play(cfg.clickSound, 1.0f);
                return true;
            }
            if (hit(x, y, cx, setRowY(2), cw, 26)) {
                cfg.sounds = !cfg.sounds;
                MenuConfig.save();
                play(cfg.clickSound, 1.0f);
                return true;
            }
            if (hit(x, y, cx + cw - 84, bindRowY() + 3, 76, 20)) {
                bindingKey = true;
                play(cfg.clickSound, 1.0f);
                return true;
            }
        }

        if (tab == 2 && hit(x, y, cx, cy + 2, cw, 36)) {
            cfg.noShadows = !cfg.noShadows;
            MenuConfig.save();
            NoShadows.apply(MinecraftClient.getInstance());
            play(cfg.clickSound, 1.0f);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (pickerOpen && drag != 0) {
            double x = mouseX / fit, y = mouseY / fit;
            if (drag == 1) setSV(x, y);
            else setHue(x);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        drag = 0;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (tab == 0 && !pickerOpen) {
            scroll = Math.max(0, Math.min(playersMax, scroll - (int) Math.signum(vertical)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        MenuConfig cfg = MenuConfig.I;

        // przypisywanie klawisza
        if (bindingKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                // anuluj
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                cfg.noShadowsKey = -1;
                MenuConfig.save();
            } else {
                cfg.noShadowsKey = keyCode;
                MenuConfig.save();
            }
            bindingKey = false;
            return true;
        }

        if (pickerOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closePicker(false);
                return true;
            }
            if (hexFocused) {
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !hex.isEmpty()) {
                    hex = hex.substring(0, hex.length() - 1);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    hexFocused = false;
                    return true;
                }
            }
        }

        if (MenuModClient.OPEN.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (pickerOpen && hexFocused && hex.length() < 6 && Character.digit(chr, 16) >= 0) {
            hex = hex + Character.toUpperCase(chr);
            if (hex.length() == 6) {
                float[] a = toHsv(Integer.parseInt(hex, 16));
                hh = Math.min(a[0], 0.9999f);
                hs = a[1];
                hv = a[2];
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
