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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuScreen extends Screen {
    static final String[] NAMES = {"Gracze", "Ustawienia", "Mody", "Profil"};
    static final Item[] ICONS = {Items.PLAYER_HEAD, Items.COMPARATOR, Items.CHEST, Items.NAME_TAG};
    static final String[] DAYS = {"Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd"};

    // wymiary (w "wirtualnych" pikselach GUI)
    static final int W = 440, H = 280, SB = 112;
    static final int ITEM_H = 30, ITEM_PITCH = 34, ITEM_Y0 = 40;

    // kolory (zależne od motywu, ustawiane w applyTheme)
    static int BG, SIDE, CARD, CARD_HOVER, INSET, TEXT, DIM, OFF, OFF_HOVER, POPUP, FIELD, BORDER, HOVER_ITEM, DIVIDER;
    static final int WHITE = 0xFFFFFFFF;

    // dźwięki menu (na stałe, nie do zmiany w configu)
    static final String OPEN_SOUND = "minecraft:ui.toast.in";
    static final String CLICK_SOUND = "minecraft:ui.button.click";

    static final int ON = 0xFF22C55E; // stały kolor włączonych przełączników (nie zależy od koloru GUI)
    static final String[] THEMES = {"Ciemny", "Jasny", "Podstawowy"};

    static boolean sh() {
        return MenuConfig.I.textShadow;
    }

    static void applyTheme(int theme) {
        if (theme == 1) {
            BG = 0xF2F4F6F9; SIDE = 0xFFE6EAF0; CARD = 0xFFFFFFFF; CARD_HOVER = 0xFFEDF0F5;
            INSET = 0xFFECEFF4; TEXT = 0xFF1B2027; DIM = 0xFF6B7684; OFF = 0xFFC5CCD6; OFF_HOVER = 0xFFB4BCC8;
            POPUP = 0xFFFFFFFF; FIELD = 0xFFEDF0F5; BORDER = 0xFFD2D8E0; HOVER_ITEM = 0x14000000; DIVIDER = 0x22000000;
        } else if (theme == 2) {
            BG = 0xFF3A3A3A; SIDE = 0xFF2B2B2B; CARD = 0xFF555555; CARD_HOVER = 0xFF666666;
            INSET = 0xFF373737; TEXT = 0xFFFFFFFF; DIM = 0xFFC0C0C0; OFF = 0xFF2E2E2E; OFF_HOVER = 0xFF444444;
            POPUP = 0xFF444444; FIELD = 0xFF000000; BORDER = 0xFF000000; HOVER_ITEM = 0x30FFFFFF; DIVIDER = 0x44000000;
        } else {
            BG = 0xF2101318; SIDE = 0xFF161A20; CARD = 0xFF1B2027; CARD_HOVER = 0xFF242B34;
            INSET = 0xFF14181D; TEXT = 0xFFFFFFFF; DIM = 0xFF8A94A3; OFF = 0xFF39414C; OFF_HOVER = 0xFF48515E;
            POPUP = 0xFF161A20; FIELD = 0xFF0F1318; BORDER = 0xFF2A313B; HOVER_ITEM = 0x18FFFFFF; DIVIDER = 0x22FFFFFF;
        }
    }

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
    private int modView = 0; // 0 = lista modów, 1 = ustawienia cieni, 2 = ustawienia HUD
    private float slideX = 0f;
    private float frameDt = 0f;
    private final Map<Integer, Float> togAnim = new HashMap<>();
    private boolean tagInput;
    private String tagText = "";
    private final List<int[]> tagRects = new ArrayList<>();
    private int[] addRect;

    // picker koloru
    private boolean pickerOpen, hexFocused, bindingKey;
    private float hh, hs, hv;
    private String hex = "";
    private int drag = 0; // 1 = pole SV, 2 = suwak hue

    public MenuScreen() {
        this(1, 0);
        play(OPEN_SOUND, 1.0f);
    }

    public MenuScreen(int startTab, int startView) {
        super(Text.literal("Mod Menu"));
        applyTheme(MenuConfig.I.theme);
        tab = startTab;
        modView = startView;
        selY = ITEM_Y0 + tab * ITEM_PITCH;
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
        return MenuConfig.I.rounded && MenuConfig.I.theme != 2;
    }

    /** Zaokrąglony prostokąt z wygładzonymi rogami (bez nakładania pikseli, działa z przezroczystością). */
    static void rr(DrawContext c, int x, int y, int w, int h, int r, int col) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (!rounded() || r <= 0) {
            c.fill(x, y, x + w, y + h, col);
            return;
        }
        int a = col >>> 24;
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            double edge = r - Math.sqrt(r * r - dy * dy);
            int full = (int) Math.ceil(edge);
            c.fill(x + full, y + i, x + w - full, y + i + 1, col);
            c.fill(x + full, y + h - 1 - i, x + w - full, y + h - i, col);
            if (full > 0) {
                int pa = (int) (a * (full - edge));
                if (pa > 0) {
                    int pc = alpha(col, pa);
                    c.fill(x + full - 1, y + i, x + full, y + i + 1, pc);
                    c.fill(x + w - full, y + i, x + w - full + 1, y + i + 1, pc);
                    c.fill(x + full - 1, y + h - 1 - i, x + full, y + h - i, pc);
                    c.fill(x + w - full, y + h - 1 - i, x + w - full + 1, y + h - i, pc);
                }
            }
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

    /** Przełącznik z animacją (kulka płynnie przelatuje, tło zmienia kolor). Kolor nie zależy od koloru GUI. */
    private void toggle(DrawContext c, int x, int y, boolean on, int id) {
        float tgt = on ? 1f : 0f;
        float p = togAnim.getOrDefault(id, tgt);
        p += (tgt - p) * (1f - (float) Math.exp(-frameDt * 18f));
        if (Math.abs(tgt - p) < 0.01f) p = tgt;
        togAnim.put(id, p);
        rr(c, x, y, 34, 18, 9, mix(OFF, ON, p));
        rr(c, x + 3 + Math.round(16f * p), y + 3, 12, 12, 6, WHITE);
    }

    private void logo(DrawContext c, int x, int y, int sz) {
        rr(c, x, y, sz, sz, 6, 0xFF4F7DF3);
        c.fillGradient(x + 1, y + 1, x + sz - 1, y + sz - 1, 0xFF5BA4FF, 0xFF7B3FF2);
        mask(c, x, y, sz, sz, 6, SIDE);
        c.fill(x + 4, y + 4, x + sz - 4, y + 6, WHITE);
        c.fill(x + 4, y + 8, x + sz - 9, y + 10, WHITE);
        c.fill(x + 4, y + 12, x + sz - 4, y + 14, WHITE);
        rr(c, x + sz - 7, y + 8, 3, 3, 1, WHITE);
    }

    private void small(DrawContext c, String s, int x, int y, int col, float scale) {
        var m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        c.drawText(textRenderer, s, 0, 0, col, sh());
        m.pop();
    }

    private void centered(DrawContext c, String s, int x, int y, int w, int col) {
        c.drawText(textRenderer, s, x + (w - textRenderer.getWidth(s)) / 2, y, col, sh());
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

    private int setRowY(int i) { return cy() + 2 + i * 30; }
    private int bindRowY() { return cy() + 172; }

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
        applyTheme(MenuConfig.I.theme);
        long now = Util.getMeasuringTimeMs();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        frameDt = dt;

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
        logo(c, px + 16, py + 14, 18);
        c.drawText(textRenderer, Text.literal("Mod Menu").formatted(Formatting.BOLD), px + 40, py + 19, accent, sh());

        // ikony + nazwy po lewej
        int ex = px + 12, ew = SB - 12;
        for (int i = 0; i < 4; i++) {
            int y = py + ITEM_Y0 + i * ITEM_PITCH;
            if (i != tab && hov(ex, y, ew, ITEM_H)) {
                rr(c, ex, y, ew, ITEM_H, 8, HOVER_ITEM);
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
            c.drawText(textRenderer, NAMES[i], ex + 30, y + 11, i == tab ? TEXT : DIM, sh());
        }

        // nagłówek treści
        int cx = cx(), cw = cw(), cy = cy();
        c.drawText(textRenderer, Text.literal(NAMES[tab]).formatted(Formatting.BOLD), cx, py + 15, accent, sh());
        c.fill(cx, py + 28, cx + cw, py + 29, DIVIDER);

        // treść zakładki (lekko wjeżdża od dołu)
        float te = ease(Math.min(1f, (now - tabAt) / 220f));
        m.push();
        m.translate(slideX * (1f - te), slideX == 0f ? (1f - te) * 8f : 0f, 0f);
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
                cx, cy + 2, DIM, sh());

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
            c.drawText(textRenderer, p.getName().getString(), cx + 30, ry + 9, TEXT, sh());

            String d = Math.round(mc.player.distanceTo(p)) + " m";
            int dw = textRenderer.getWidth(d);
            int right = cx + cw - 8 - 8;
            c.drawText(textRenderer, d, right - dw, ry + 9, DIM, sh());

            if (p.getChunkPos().equals(mc.player.getChunkPos())) {
                int chipW = textRenderer.getWidth("CHUNK") + 10;
                int chipX = right - dw - 6 - chipW;
                rr(c, chipX, ry + 6, chipW, 14, 7, alpha(accent, 0x66));
                c.drawText(textRenderer, "CHUNK", chipX + 5, ry + 9, TEXT, sh());
            }
        }

        if (playersMax > 0) {
            int trackH = visible * (rowH + gap) - gap;
            int thumbH = Math.max(14, trackH * visible / list.size());
            int thumbY = cy + 16 + (trackH - thumbH) * scroll / playersMax;
            rr(c, cx + cw - 4, cy + 16, 3, trackH, 1, DIVIDER);
            rr(c, cx + cw - 4, thumbY, 3, thumbH, 1, alpha(accent, 0xCC));
        }
    }

    // ------------------------------------------------------------ zakładka: Ustawienia

    private void switchRow(DrawContext c, int cx, int cw, int y, String label, boolean on, int id) {
        rr(c, cx, y, cw, 26, 8, hov(cx, y, cw, 26) ? CARD_HOVER : CARD);
        c.drawText(textRenderer, label, cx + 10, y + 9, TEXT, sh());
        toggle(c, cx + cw - 44, y + 4, on, id);
    }

    /** Pozycja i szerokość i-tego przycisku wyboru motywu. */
    private int[] seg(int i, int cx, int cw) {
        int[] ws = new int[3];
        int total = 8;
        for (int k = 0; k < 3; k++) {
            ws[k] = textRenderer.getWidth(THEMES[k]) + 14;
            total += ws[k];
        }
        int x = cx + cw - 8 - total;
        for (int k = 0; k < i; k++) x += ws[k] + 4;
        return new int[]{x, ws[i]};
    }

    private void drawSettings(DrawContext c, int cx, int cy, int cw) {
        MenuConfig cfg = MenuConfig.I;

        // kolor GUI
        int y = setRowY(0);
        rr(c, cx, y, cw, 26, 8, hov(cx, y, cw, 26) ? CARD_HOVER : CARD);
        c.drawText(textRenderer, "Kolor GUI", cx + 10, y + 9, TEXT, sh());
        String hx = String.format("#%06X", accent & 0xFFFFFF);
        c.drawText(textRenderer, hx, cx + cw - 58 - textRenderer.getWidth(hx), y + 9, DIM, sh());
        rr(c, cx + cw - 52, y + 5, 42, 16, 8, 0xFF000000 | (accent & 0xFFFFFF));

        switchRow(c, cx, cw, setRowY(1), "Zaokrąglone ramki", cfg.rounded, 1);
        switchRow(c, cx, cw, setRowY(2), "Dźwięki menu", cfg.sounds, 2);
        switchRow(c, cx, cw, setRowY(3), "Cień za tekstem", cfg.textShadow, 3);

        // motyw: ciemny / jasny / podstawowy
        y = setRowY(4);
        rr(c, cx, y, cw, 26, 8, CARD);
        c.drawText(textRenderer, "Motyw", cx + 10, y + 9, TEXT, sh());
        for (int i = 0; i < 3; i++) {
            int[] r = seg(i, cx, cw);
            boolean sel = cfg.theme == i;
            boolean h = hov(r[0], y + 4, r[1], 18);
            rr(c, r[0], y + 4, r[1], 18, 9, sel ? ON : (h ? OFF_HOVER : OFF));
            centered(c, THEMES[i], r[0], y + 9, r[1], sel ? WHITE : TEXT);
        }

        // bindy
        c.drawText(textRenderer, "Bindy modułów", cx, bindRowY() - 14, DIM, sh());
        y = bindRowY();
        rr(c, cx, y, cw, 26, 8, CARD);
        c.drawText(textRenderer, "Brak cieni", cx + 10, y + 9, TEXT, sh());
        int bx = cx + cw - 84, by = y + 3;
        boolean bh = hov(bx, by, 76, 20);
        rr(c, bx, by, 76, 20, 10, bindingKey ? ON : (bh ? OFF_HOVER : OFF));
        String label;
        if (bindingKey) label = "naciśnij...";
        else if (cfg.noShadowsKey <= 0) label = "—";
        else label = InputUtil.Type.KEYSYM.createFromCode(cfg.noShadowsKey).getLocalizedText().getString();
        centered(c, label, bx, by + 6, 76, bindingKey ? WHITE : TEXT);
    }

    // ------------------------------------------------------------ zakładka: Mody

    private void gear(DrawContext c, int gx, int gy, int col, int bg) {
        ellipse(c, gx, gy, 5, 5, col);
        c.fill(gx - 1, gy - 7, gx + 1, gy - 4, col);
        c.fill(gx - 1, gy + 5, gx + 1, gy + 8, col);
        c.fill(gx - 7, gy - 1, gx - 4, gy + 1, col);
        c.fill(gx + 5, gy - 1, gx + 8, gy + 1, col);
        c.fill(gx + 3, gy - 5, gx + 5, gy - 3, col);
        c.fill(gx - 5, gy - 5, gx - 3, gy - 3, col);
        c.fill(gx + 3, gy + 3, gx + 5, gy + 5, col);
        c.fill(gx - 5, gy + 3, gx - 3, gy + 5, col);
        ellipse(c, gx, gy, 2, 2, bg);
    }

    static final String[] SHADOW_MODES = {
            "Wyłącz wszystko (bloki i byty)",
            "Wyłącz tylko cienie bytów",
            "Wyłącz tylko cienie bloków"
    };
    static final String[] HUD_NAMES = {"XYZ", "Facing (kierunek)", "FPS", "Ping", "TPS"};

    private boolean hudFlag(int i) {
        MenuConfig f = MenuConfig.I;
        return switch (i) {
            case 0 -> f.hudXYZ;
            case 1 -> f.hudFacing;
            case 2 -> f.hudFps;
            case 3 -> f.hudPing;
            default -> f.hudTps;
        };
    }

    private void flipHudFlag(int i) {
        MenuConfig f = MenuConfig.I;
        switch (i) {
            case 0 -> f.hudXYZ = !f.hudXYZ;
            case 1 -> f.hudFacing = !f.hudFacing;
            case 2 -> f.hudFps = !f.hudFps;
            case 3 -> f.hudPing = !f.hudPing;
            default -> f.hudTps = !f.hudTps;
        }
    }

    /** Przejście do podwidoku modów z animacją (view 0 = powrót). */
    private void go(int view) {
        slideX = view > 0 ? 28f : -28f;
        modView = view;
        tabAt = Util.getMeasuringTimeMs();
        play(CLICK_SOUND, 1.0f);
    }

    private void modCard(DrawContext c, int cx, int cw, int y, String title, String desc, boolean gearBtn, boolean on, int id) {
        rr(c, cx, y, cw, 36, 8, hov(cx, y, cw, 36) ? CARD_HOVER : CARD);
        c.drawText(textRenderer, Text.literal(title).formatted(Formatting.BOLD), cx + 10, y + 7, TEXT, sh());
        small(c, desc, cx + 10, y + 22, DIM, 0.8f);
        if (gearBtn) {
            int gbx = cx + cw - 80, gby = y + 8;
            boolean gh = hov(gbx, gby, 24, 20);
            int bg = gh ? OFF_HOVER : OFF;
            rr(c, gbx, gby, 24, 20, 10, bg);
            gear(c, gbx + 12, gby + 10, TEXT, bg);
        }
        toggle(c, cx + cw - 44, y + 9, on, id);
    }

    private void drawMods(DrawContext c, int cx, int cy, int cw) {
        if (modView == 1) {
            drawShadowSettings(c, cx, cy, cw);
            return;
        }
        if (modView == 2) {
            drawHudSettings(c, cx, cy, cw);
            return;
        }
        MenuConfig cfg = MenuConfig.I;
        modCard(c, cx, cw, cy + 2, "Brak cieni", "Wyłącza cieniowanie bloków i cienie bytów", true, cfg.noShadows, 10);
        modCard(c, cx, cw, cy + 44, "Brak animacji wyciągania", "Przedmiot w ręce pojawia się od razu", false, cfg.noEquipAnim, 11);
        modCard(c, cx, cw, cy + 86, "HUD", "XYZ, facing, fps, ping, tps - ustaw i przesuń", true, cfg.hudOn, 12);
    }

    private void backBar(DrawContext c, int cx, int cy, int cw, String label, boolean on, int id) {
        boolean bh = hov(cx, cy + 2, 60, 20);
        rr(c, cx, cy + 2, 60, 20, 10, bh ? CARD_HOVER : CARD);
        centered(c, "< Wróć", cx, cy + 8, 60, TEXT);
        c.drawText(textRenderer, label, cx + cw - 44 - 6 - textRenderer.getWidth(label), cy + 8, DIM, sh());
        toggle(c, cx + cw - 44, cy + 3, on, id);
    }

    private void drawHudSettings(DrawContext c, int cx, int cy, int cw) {
        backBar(c, cx, cy, cw, "HUD", MenuConfig.I.hudOn, 14);
        for (int i = 0; i < HUD_NAMES.length; i++) {
            int y = cy + 28 + i * 26;
            rr(c, cx, y, cw, 22, 8, hov(cx, y, cw, 22) ? CARD_HOVER : CARD);
            c.drawText(textRenderer, HUD_NAMES[i], cx + 10, y + 7, TEXT, sh());
            toggle(c, cx + cw - 44, y + 2, hudFlag(i), 20 + i);
        }
        int by = cy + 164;
        rr(c, cx, by, cw, 26, 8, hov(cx, by, cw, 26) ? CARD_HOVER : CARD);
        centered(c, "Przesuń HUD", cx, by + 9, cw, TEXT);
    }

    private void drawShadowSettings(DrawContext c, int cx, int cy, int cw) {
        MinecraftClient mc = MinecraftClient.getInstance();
        MenuConfig cfg = MenuConfig.I;

        backBar(c, cx, cy, cw, "Moduł", cfg.noShadows, 13);

        // tryby
        for (int i = 0; i < 3; i++) {
            int y = cy + 28 + i * 26;
            rr(c, cx, y, cw, 22, 8, hov(cx, y, cw, 22) ? CARD_HOVER : CARD);
            boolean sel = cfg.shadowMode == i;
            rr(c, cx + 8, y + 5, 12, 12, 6, sel ? ON : OFF);
            if (sel) rr(c, cx + 11, y + 8, 6, 6, 3, WHITE);
            c.drawText(textRenderer, SHADOW_MODES[i], cx + 28, y + 7, TEXT, sh());
        }

        // podgląd (pokazuje to, co faktycznie zostanie włączone)
        boolean blockOff = cfg.blockShadowsOff();
        boolean entOff = cfg.entityShadowsOff();
        int pw = (cw - 8) / 2, ph = 100, py2 = cy + 108;

        int x1 = cx;
        rr(c, x1, py2, pw, ph, 8, INSET);
        small(c, "Bloki", x1 + 8, py2 + 7, DIM, 0.9f);
        int mid = x1 + pw / 2;
        int base = 0x9AA6B2;
        c.fill(mid - 24, py2 + 24, mid + 24, py2 + 37, shade(base, 1.0f));
        c.fill(mid - 24, py2 + 38, mid - 1, py2 + 62, shade(base, blockOff ? 1.0f : 0.80f));
        c.fill(mid + 1, py2 + 38, mid + 24, py2 + 62, shade(base, blockOff ? 1.0f : 0.60f));
        c.drawText(textRenderer, "Cienie bloków:", x1 + 8, py2 + ph - 15, DIM, sh());
        String s1 = blockOff ? "WYŁ." : "WŁ.";
        c.drawText(textRenderer, s1, x1 + pw - 8 - textRenderer.getWidth(s1), py2 + ph - 15,
                blockOff ? 0xFFEF4444 : ON, sh());

        int x2 = cx + pw + 8;
        rr(c, x2, py2, pw, ph, 8, INSET);
        small(c, "Byty (gracz, moby)", x2 + 8, py2 + 7, DIM, 0.9f);
        int mid2 = x2 + pw / 2;
        if (!entOff) {
            ellipse(c, mid2, py2 + 62, 22, 5, 0x88000000);
        }
        if (mc.player != null) {
            PlayerSkinDrawer.draw(c, mc.player.getSkinTextures(), mid2 - 18, py2 + 22, 36);
        }
        c.drawText(textRenderer, "Cienie bytów:", x2 + 8, py2 + ph - 15, DIM, sh());
        String s2 = entOff ? "WYŁ." : "WŁ.";
        c.drawText(textRenderer, s2, x2 + pw - 8 - textRenderer.getWidth(s2), py2 + ph - 15,
                entOff ? 0xFFEF4444 : ON, sh());
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
        c.drawText(textRenderer, Text.literal(name).formatted(Formatting.BOLD), cx + 76, cy + 12, TEXT, sh());
        c.drawText(textRenderer, "Nick: " + name, cx + 76, cy + 26, DIM, sh());
        long sess = (System.currentTimeMillis() - MenuModClient.SESSION_START) / 1000;
        c.drawText(textRenderer, "Sesja: " + fmtHms(sess), cx + 76, cy + 41, TEXT, sh());
        c.drawText(textRenderer, "Dziś: " + fmtHm(days[6]) + "   Tydzień: " + fmtHm(week), cx + 76, cy + 55, DIM, sh());

        // wykres: czas gry w ostatnich 7 dniach
        int gy = cy + 82;
        rr(c, cx, gy, cw, 104, 8, CARD);
        c.drawText(textRenderer, "Czas gry — ostatnie 7 dni", cx + 10, gy + 8, DIM, sh());
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

        // tagi: klik w tag usuwa, "+" dodaje (Enter zatwierdza, Esc anuluje)
        int ty = cy + 194;
        rr(c, cx, ty, cw, 30, 8, CARD);
        c.drawText(textRenderer, "Tagi", cx + 10, ty + 11, DIM, sh());
        tagRects.clear();
        addRect = null;
        int chipX = cx + 44;
        for (int i = 0; i < cfg.tags.size(); i++) {
            String tag = cfg.tags.get(i);
            int chipW = textRenderer.getWidth(tag) + 22;
            if (chipX + chipW > cx + cw - 26) break;
            boolean h = hov(chipX, ty + 7, chipW, 16);
            rr(c, chipX, ty + 7, chipW, 16, 8, alpha(accent, h ? 0x99 : 0x66));
            c.drawText(textRenderer, tag, chipX + 6, ty + 11, TEXT, sh());
            c.drawText(textRenderer, "x", chipX + chipW - 9, ty + 11, DIM, sh());
            tagRects.add(new int[]{chipX, ty + 7, chipW, 16, i});
            chipX += chipW + 5;
        }
        if (tagInput) {
            int fw = Math.max(60, textRenderer.getWidth(tagText) + 16);
            rr(c, chipX, ty + 6, fw, 18, 9, FIELD);
            c.drawText(textRenderer, tagText, chipX + 8, ty + 11, TEXT, sh());
            if ((now / 500) % 2 == 0) {
                int cxp = chipX + 8 + textRenderer.getWidth(tagText);
                c.fill(cxp, ty + 10, cxp + 1, ty + 20, TEXT);
            }
        } else if (cfg.tags.size() < 6) {
            boolean h = hov(chipX, ty + 7, 18, 16);
            rr(c, chipX, ty + 7, 18, 16, 8, h ? OFF_HOVER : OFF);
            centered(c, "+", chipX, ty + 11, 18, TEXT);
            addRect = new int[]{chipX, ty + 7, 18, 16};
        }
    }

    // ------------------------------------------------------------ picker koloru

    private void drawPicker(DrawContext c, long now) {
        int ppx = ppx(), ppy = ppy();
        rr(c, ppx - 1, ppy - 1, PW + 2, PH + 2, 9, BORDER);
        rr(c, ppx, ppy, PW, PH, 8, POPUP);

        // pole nasycenie / jasność
        int sx = ppx + 10, sy = ppy + 10, sw = 168, sh = 80;
        for (int i = 0; i < sw; i++) {
            float satur = i / (float) (sw - 1);
            c.fillGradient(sx + i, sy, sx + i + 1, sy + sh, hsv(hh, satur, 1f), 0xFF000000);
        }
        mask(c, sx, sy, sw, sh, 4, POPUP);

        // suwak hue
        int hx = ppx + 10, hy = ppy + 100, hw = 168, hhh = 12;
        for (int i = 0; i < hw; i++) {
            c.fill(hx + i, hy, hx + i + 1, hy + hhh, hsv(i / (float) (hw - 1) * 0.9999f, 1f, 1f));
        }
        mask(c, hx, hy, hw, hhh, 6, POPUP);

        // znaczniki
        int mxp = sx + Math.round(hs * (sw - 1));
        int myp = sy + Math.round((1f - hv) * (sh - 1));
        rr(c, mxp - 4, myp - 4, 9, 9, 4, WHITE);
        rr(c, mxp - 3, myp - 3, 7, 7, 3, 0xFF000000 | pickRgb());
        int hxp = hx + Math.round(hh * (hw - 1));
        rr(c, hxp - 3, hy - 2, 6, hhh + 4, 3, WHITE);

        // pole hex
        int fx = ppx + 10, fy = ppy + 122;
        if (hexFocused) rr(c, fx - 1, fy - 1, 102, 24, 7, accent);
        rr(c, fx, fy, 100, 22, 6, FIELD);
        rr(c, fx + 4, fy + 4, 14, 14, 5, 0xFF000000 | pickRgb());
        String shown = "#" + hex;
        c.drawText(textRenderer, shown, fx + 24, fy + 7, TEXT, sh());
        if (hexFocused && (now / 500) % 2 == 0 && hex.length() < 6) {
            int cxp = fx + 24 + textRenderer.getWidth(shown);
            c.fill(cxp, fy + 6, cxp + 1, fy + 16, WHITE);
        }

        // przycisk Gotowe
        int bx = ppx + 118, by = ppy + 122;
        boolean bh = hov(bx, by, 60, 22);
        rr(c, bx, by, 60, 22, 11, bh ? mix(ON, WHITE, 0.2f) : ON);
        centered(c, "Gotowe", bx, by + 7, 60, WHITE);
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
                play(CLICK_SOUND, 1.2f);
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
                    modView = 0;
                    slideX = 0f;
                    tagInput = false;
                    play(CLICK_SOUND, 1.0f);
                }
                return true;
            }
        }

        if (tab == 1) {
            if (hit(x, y, cx, setRowY(0), cw, 26)) {
                play(CLICK_SOUND, 1.0f);
                openPicker();
                return true;
            }
            if (hit(x, y, cx, setRowY(1), cw, 26)) {
                cfg.rounded = !cfg.rounded;
                MenuConfig.save();
                play(CLICK_SOUND, 1.0f);
                return true;
            }
            if (hit(x, y, cx, setRowY(2), cw, 26)) {
                cfg.sounds = !cfg.sounds;
                MenuConfig.save();
                play(CLICK_SOUND, 1.0f);
                return true;
            }
            if (hit(x, y, cx, setRowY(3), cw, 26)) {
                cfg.textShadow = !cfg.textShadow;
                MenuConfig.save();
                play(CLICK_SOUND, 1.0f);
                return true;
            }
            for (int i = 0; i < 3; i++) {
                int[] r = seg(i, cx, cw);
                if (hit(x, y, r[0], setRowY(4) + 4, r[1], 18)) {
                    cfg.theme = i;
                    applyTheme(i);
                    MenuConfig.save();
                    play(CLICK_SOUND, 1.0f);
                    return true;
                }
            }
            if (hit(x, y, cx + cw - 84, bindRowY() + 3, 76, 20)) {
                bindingKey = true;
                play(CLICK_SOUND, 1.0f);
                return true;
            }
        }

        if (tab == 2) {
            if (modView == 0) {
                // karty: Brak cieni (cy+2), Brak animacji (cy+44), HUD (cy+86)
                if (hit(x, y, cx + cw - 80, cy + 10, 24, 20)) {
                    go(1);
                    return true;
                }
                if (hit(x, y, cx, cy + 2, cw, 36)) {
                    cfg.noShadows = !cfg.noShadows;
                    MenuConfig.save();
                    NoShadows.apply(MinecraftClient.getInstance());
                    play(CLICK_SOUND, 1.0f);
                    return true;
                }
                if (hit(x, y, cx, cy + 44, cw, 36)) {
                    cfg.noEquipAnim = !cfg.noEquipAnim;
                    MenuConfig.save();
                    play(CLICK_SOUND, 1.0f);
                    return true;
                }
                if (hit(x, y, cx + cw - 80, cy + 94, 24, 20)) {
                    go(2);
                    return true;
                }
                if (hit(x, y, cx, cy + 86, cw, 36)) {
                    cfg.hudOn = !cfg.hudOn;
                    MenuConfig.save();
                    play(CLICK_SOUND, 1.0f);
                    return true;
                }
            } else if (modView == 1) {
                if (hit(x, y, cx, cy + 2, 60, 20)) {
                    go(0);
                    return true;
                }
                if (hit(x, y, cx + cw - 44, cy + 3, 34, 18)) {
                    cfg.noShadows = !cfg.noShadows;
                    MenuConfig.save();
                    NoShadows.apply(MinecraftClient.getInstance());
                    play(CLICK_SOUND, 1.0f);
                    return true;
                }
                for (int i = 0; i < 3; i++) {
                    if (hit(x, y, cx, cy + 28 + i * 26, cw, 22)) {
                        cfg.shadowMode = i;
                        MenuConfig.save();
                        if (cfg.noShadows) NoShadows.apply(MinecraftClient.getInstance());
                        play(CLICK_SOUND, 1.0f);
                        return true;
                    }
                }
            } else {
                if (hit(x, y, cx, cy + 2, 60, 20)) {
                    go(0);
                    return true;
                }
                if (hit(x, y, cx + cw - 44, cy + 3, 34, 18)) {
                    cfg.hudOn = !cfg.hudOn;
                    MenuConfig.save();
                    play(CLICK_SOUND, 1.0f);
                    return true;
                }
                for (int i = 0; i < HUD_NAMES.length; i++) {
                    if (hit(x, y, cx, cy + 28 + i * 26, cw, 22)) {
                        flipHudFlag(i);
                        MenuConfig.save();
                        play(CLICK_SOUND, 1.0f);
                        return true;
                    }
                }
                if (hit(x, y, cx, cy + 164, cw, 26)) {
                    MenuConfig.save();
                    play(CLICK_SOUND, 1.0f);
                    MinecraftClient.getInstance().setScreen(new HudEditScreen());
                    return true;
                }
            }
        }

        if (tab == 3) {
            boolean wasInput = tagInput;
            tagInput = false;
            for (int[] r : tagRects) {
                if (hit(x, y, r[0], r[1], r[2], r[3])) {
                    cfg.tags.remove(r[4]);
                    MenuConfig.save();
                    play(CLICK_SOUND, 1.0f);
                    return true;
                }
            }
            if (!wasInput && addRect != null && hit(x, y, addRect[0], addRect[1], addRect[2], addRect[3])) {
                tagInput = true;
                tagText = "";
                play(CLICK_SOUND, 1.0f);
                return true;
            }
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

        // wpisywanie tagu
        if (tagInput) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                tagInput = false;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!tagText.isEmpty()) tagText = tagText.substring(0, tagText.length() - 1);
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                String t = tagText.trim();
                if (!t.isEmpty() && cfg.tags.size() < 6 && !cfg.tags.contains(t)) {
                    cfg.tags.add(t);
                    MenuConfig.save();
                }
                tagInput = false;
            }
            return true;
        }

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
        if (tagInput) {
            if (tagText.length() < 12 && !Character.isISOControl(chr)) tagText = tagText + chr;
            return true;
        }
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
