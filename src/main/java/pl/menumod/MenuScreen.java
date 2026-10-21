package pl.menumod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class MenuScreen extends Screen {

    private enum Tab {
        MODY("Mody"), PROFIL("Profil"), USTAWIENIA("Ustawienia");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    // wymiary
    private static final int W = 340;
    private static final int H = 224;
    private static final int SB = 40;   // szerokość panelu z ikonami
    private static final int HDR = 26;  // wysokość nagłówka
    private static final int PAD = 12;
    private static final int ANIM_MS = 260;

    // kolory (RGB)
    private static final int[] PALETTE = {0x7C5CFF, 0x3B82F6, 0x06B6D4, 0x22C55E, 0xEAB308, 0xF97316, 0xEF4444, 0xEC4899};
    private static final int WHITE = 0xFFFFFF;
    private static final int MUTED = 0x9A9AB5;
    private static final int PANEL = 0x12121C;
    private static final int CARD = 0x1E1E2E;
    private static final int OFF = 0x3A3A4F;

    private final ItemStack iconMody = new ItemStack(Items.CHEST);
    private final ItemStack iconUstawienia = new ItemStack(Items.COMPARATOR);

    private Tab tab = Tab.MODY;
    private boolean listening = false;
    private long openedAt = 0L;
    private float alpha = 0f;
    private int left, top;

    public MenuScreen() {
        super(Text.literal("Menu"));
    }

    // ---------- układ ----------

    private int gap() { return MenuConfig.joined ? 0 : 6; }
    private int mainX2() { return left + W - SB - gap(); }
    private int sbX1() { return left + W - SB; }
    private int cntY1() { return top + HDR + gap(); }
    private int ix1() { return left + PAD; }
    private int ix2() { return mainX2() - PAD; }
    private int iy1() { return cntY1() + PAD; }
    private int tabX() { return sbX1() + 6; }
    private int tabY(int i) { return top + 8 + i * 32; }

    // ---------- cykl życia ----------

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        if (openedAt == 0L) {
            openedAt = System.currentTimeMillis();
            MinecraftClient.getInstance().getSoundManager()
                    .play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f));
        }
    }

    @Override
    public void removed() {
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void click() {
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    // ---------- rysowanie ----------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float t = MathHelper.clamp((System.currentTimeMillis() - openedAt) / (float) ANIM_MS, 0f, 1f);
        float ease = 1f - (float) Math.pow(1f - t, 3);
        alpha = ease;

        // przyciemnienie tła gry
        ctx.fill(0, 0, width, height, col(0x000000, 0.45f));

        // animacja otwierania: powiększanie + wjazd z dołu + pojawianie się
        float scale = 0.9f + 0.1f * ease;
        float cx = left + W / 2f;
        float cy = top + H / 2f;
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(cx, cy + (1f - ease) * 12f, 0f);
        m.scale(scale, scale, 1f);
        m.translate(-cx, -cy, 0f);

        drawPanels(ctx);
        drawHeader(ctx);

        switch (tab) {
            case MODY -> drawMody(ctx, mouseX, mouseY);
            case PROFIL -> drawProfil(ctx);
            case USTAWIENIA -> drawUstawienia(ctx, mouseX, mouseY);
        }

        drawSidebar(ctx, mouseX, mouseY);

        m.pop();
    }

    private void drawPanels(DrawContext ctx) {
        int r = MenuConfig.rounded ? 8 : 0;
        if (MenuConfig.joined) {
            panel(ctx, left, top, left + W, top + H, r);
            ctx.fill(left + 8, top + HDR, mainX2() - 8, top + HDR + 1, col(MenuConfig.accent, 0.5f));
            ctx.fill(sbX1(), top + 8, sbX1() + 1, top + H - 8, col(MenuConfig.accent, 0.5f));
        } else {
            panel(ctx, left, top, mainX2(), top + HDR, r);
            panel(ctx, left, cntY1(), mainX2(), top + H, r);
            panel(ctx, sbX1(), top, left + W, top + H, r);
        }
    }

    private void panel(DrawContext ctx, int x1, int y1, int x2, int y2, int r) {
        if (MenuConfig.background) {
            rrect(ctx, x1 - 1, y1 - 1, x2 + 1, y2 + 1, r + 1, col(MenuConfig.accent, 1f));
            rrect(ctx, x1, y1, x2, y2, r, col(PANEL, 0.96f));
        } else {
            outline(ctx, x1, y1, x2, y2, r, col(MenuConfig.accent, 1f));
        }
    }

    private void drawHeader(DrawContext ctx) {
        int y = top + (HDR - 8) / 2;
        text(ctx, "Menu", left + PAD, y, WHITE, 1f);
        int w = textRenderer.getWidth("Menu");
        text(ctx, ">  " + tab.label, left + PAD + w + 6, y, MUTED, 1f);
    }

    private void drawSidebar(DrawContext ctx, int mx, int my) {
        Tab[] tabs = Tab.values();
        int r = MenuConfig.rounded ? 6 : 0;

        for (int i = 0; i < tabs.length; i++) {
            int x = tabX();
            int y = tabY(i);
            boolean hov = inside(mx, my, x, y, 28, 28);
            if (tab == tabs[i]) {
                rrect(ctx, x, y, x + 28, y + 28, r, col(MenuConfig.accent, 0.9f));
            } else if (hov) {
                rrect(ctx, x, y, x + 28, y + 28, r, col(WHITE, 0.14f));
            }
            drawIcon(ctx, tabs[i], x + 6, y + 6);
        }

        // podpis po najechaniu na ikonę
        for (int i = 0; i < tabs.length; i++) {
            if (inside(mx, my, tabX(), tabY(i), 28, 28) && alpha > 0.9f) {
                String s = tabs[i].label;
                int tw = textRenderer.getWidth(s);
                int x2 = sbX1() - 4;
                int y = tabY(i) + 6;
                rrect(ctx, x2 - tw - 12, y, x2, y + 16, MenuConfig.rounded ? 4 : 0, col(0x000000, 0.88f));
                text(ctx, s, x2 - tw - 6, y + 4, WHITE, 1f);
            }
        }
    }

    private void drawIcon(DrawContext ctx, Tab t, int x, int y) {
        switch (t) {
            case MODY -> ctx.drawItem(iconMody, x, y);
            case USTAWIENIA -> ctx.drawItem(iconUstawienia, x, y);
            case PROFIL -> {
                ClientPlayerEntity p = MinecraftClient.getInstance().player;
                if (p != null) {
                    PlayerSkinDrawer.draw(ctx, p.getSkinTextures(), x, y, 16);
                }
            }
        }
    }

    // ---------- zakładka: Mody ----------

    private void drawMody(DrawContext ctx, int mx, int my) {
        int y = iy1();
        modCard(ctx, mx, my, y, "Brak cieni", "Wyłącza cienie bloków i graczy.", MenuConfig.noShadows);
        modCard(ctx, mx, my, y + 50, "Cienie tekstu", "Dodaje cień pod każdym tekstem w grze.", MenuConfig.textShadows);
        text(ctx, "Więcej modów wkrótce...", ix1(), y + 108, MUTED, 1f);
    }

    private void modCard(DrawContext ctx, int mx, int my, int y, String name, String desc, boolean on) {
        int x1 = ix1();
        int x2 = ix2();
        boolean hov = inside(mx, my, x1, y, x2 - x1, 44);
        box(ctx, x1, y, x2, y + 44, hov);
        text(ctx, name, x1 + 12, y + 10, WHITE, 1f);
        text(ctx, desc, x1 + 12, y + 25, MUTED, 1f);
        sw(ctx, x2 - 12 - 28, y + 15, on);
    }

    // ---------- zakładka: Profil ----------

    private void drawProfil(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        int x = ix1();
        int y = iy1();

        PlayerSkinDrawer.draw(ctx, p.getSkinTextures(), x, y, 48);

        int tx = x + 60;
        text(ctx, mc.getSession().getUsername(), tx, y + 2, 0xDCD3FF, 1f);
        text(ctx, "Poziom: " + p.experienceLevel, tx, y + 16, WHITE, 1f);
        text(ctx, String.format("Zdrowie: %.1f / %.0f", p.getHealth(), p.getMaxHealth()), tx, y + 28, WHITE, 1f);
        text(ctx, "Głód: " + p.getHungerManager().getFoodLevel() + " / 20", tx, y + 40, WHITE, 1f);
        text(ctx, "Pozycja: " + p.getBlockX() + ", " + p.getBlockY() + ", " + p.getBlockZ(), x, y + 60, MUTED, 1f);
    }

    // ---------- zakładka: Ustawienia ----------

    private void drawUstawienia(DrawContext ctx, int mx, int my) {
        int x1 = ix1();
        int x2 = ix2();
        int y = iy1();

        text(ctx, "Kolor GUI", x1, y, WHITE, 1f);
        int rr = MenuConfig.rounded ? 5 : 0;
        for (int i = 0; i < PALETTE.length; i++) {
            int sx = x1 + i * 26;
            int sy = y + 13;
            if (PALETTE[i] == MenuConfig.accent) {
                rrect(ctx, sx - 2, sy - 2, sx + 22, sy + 22, rr + 1, col(WHITE, 1f));
            }
            rrect(ctx, sx, sy, sx + 20, sy + 20, rr, col(PALETTE[i], 1f));
        }

        int ry = y + 41;
        row(ctx, mx, my, ry, "Zaokrąglone ramki", MenuConfig.rounded);
        row(ctx, mx, my, ry + 27, "Połączone panele", MenuConfig.joined);
        row(ctx, mx, my, ry + 54, "Tło paneli", MenuConfig.background);

        // klawisz otwierania menu
        int ky = ry + 81;
        box(ctx, x1, ky, x2, ky + 24, false);
        text(ctx, "Klawisz menu", x1 + 10, ky + 8, WHITE, 1f);

        int bw = 64;
        int bx1 = x2 - 8 - bw;
        boolean hov = inside(mx, my, bx1, ky + 3, bw, 18);
        int br = MenuConfig.rounded ? 4 : 0;
        if (listening) {
            rrect(ctx, bx1, ky + 3, bx1 + bw, ky + 21, br, col(MenuConfig.accent, 0.45f));
            outline(ctx, bx1, ky + 3, bx1 + bw, ky + 21, br, col(MenuConfig.accent, 1f));
        } else {
            rrect(ctx, bx1, ky + 3, bx1 + bw, ky + 21, br, col(hov ? 0x3A3A55 : 0x2C2C42, 1f));
        }
        String label = keyLabel();
        text(ctx, label, bx1 + (bw - textRenderer.getWidth(label)) / 2, ky + 8, WHITE, 1f);

        text(ctx, "Esc przy wyborze = brak klawisza.", x1, ky + 30, MUTED, 1f);
    }

    private void row(DrawContext ctx, int mx, int my, int y, String label, boolean on) {
        int x1 = ix1();
        int x2 = ix2();
        boolean hov = inside(mx, my, x1, y, x2 - x1, 24);
        box(ctx, x1, y, x2, y + 24, hov);
        text(ctx, label, x1 + 10, y + 8, WHITE, 1f);
        sw(ctx, x2 - 10 - 28, y + 5, on);
    }

    private String keyLabel() {
        if (listening) return "> ... <";
        KeyBinding kb = MenuModClient.openKey;
        if (kb == null || kb.isUnbound()) return "Brak";
        return kb.getBoundKeyLocalizedText().getString();
    }

    // ---------- elementy GUI ----------

    private void box(DrawContext ctx, int x1, int y1, int x2, int y2, boolean hov) {
        int r = MenuConfig.rounded ? 5 : 0;
        if (MenuConfig.background) {
            rrect(ctx, x1, y1, x2, y2, r, col(hov ? 0x2A2A40 : CARD, 1f));
        } else {
            outline(ctx, x1, y1, x2, y2, r, col(WHITE, hov ? 0.55f : 0.22f));
        }
    }

    private void sw(DrawContext ctx, int x, int y, boolean on) {
        boolean round = MenuConfig.rounded;
        rrect(ctx, x, y, x + 28, y + 14, round ? 7 : 0, col(on ? MenuConfig.accent : OFF, 1f));
        int kx = on ? x + 16 : x + 2;
        rrect(ctx, kx, y + 2, kx + 10, y + 12, round ? 5 : 0, col(WHITE, 1f));
    }

    private void text(DrawContext ctx, String s, int x, int y, int rgb, float opacity) {
        int c = col(rgb, opacity);
        if ((c >>> 24) < 8) return; // przy bardzo małej przezroczystości gra rysuje tekst jako w pełni widoczny
        ctx.drawTextWithShadow(textRenderer, s, x, y, c);
    }

    private int col(int rgb, float opacity) {
        int a = MathHelper.clamp((int) (255f * opacity * alpha), 0, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int inset(int r, int i) {
        double dy = r - i - 0.5;
        return (int) Math.round(r - Math.sqrt(r * r - dy * dy));
    }

    /** Wypełniony prostokąt z zaokrąglonymi rogami. */
    private static void rrect(DrawContext c, int x1, int y1, int x2, int y2, int r, int color) {
        if (r <= 0) {
            c.fill(x1, y1, x2, y2, color);
            return;
        }
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        c.fill(x1, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            int dx = inset(r, i);
            c.fill(x1 + dx, y1 + i, x2 - dx, y1 + i + 1, color);
            c.fill(x1 + dx, y2 - i - 1, x2 - dx, y2 - i, color);
        }
    }

    /** Sama ramka (1 px) z zaokrąglonymi rogami, środek zostaje przezroczysty. */
    private static void outline(DrawContext c, int x1, int y1, int x2, int y2, int r, int color) {
        if (r <= 0) {
            c.fill(x1, y1, x2, y1 + 1, color);
            c.fill(x1, y2 - 1, x2, y2, color);
            c.fill(x1, y1, x1 + 1, y2, color);
            c.fill(x2 - 1, y1, x2, y2, color);
            return;
        }
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        int prev = 0;
        for (int i = 0; i < r; i++) {
            int dx = inset(r, i);
            if (i == 0) {
                c.fill(x1 + dx, y1, x2 - dx, y1 + 1, color);
                c.fill(x1 + dx, y2 - 1, x2 - dx, y2, color);
            } else {
                int w = Math.max(1, prev - dx);
                c.fill(x1 + dx, y1 + i, x1 + dx + w, y1 + i + 1, color);
                c.fill(x2 - dx - w, y1 + i, x2 - dx, y1 + i + 1, color);
                c.fill(x1 + dx, y2 - i - 1, x1 + dx + w, y2 - i, color);
                c.fill(x2 - dx - w, y2 - i - 1, x2 - dx, y2 - i, color);
            }
            prev = dx;
        }
        c.fill(x1, y1 + r, x1 + 1, y2 - r, color);
        c.fill(x2 - 1, y1 + r, x2, y2 - r, color);
    }

    // ---------- obsługa myszy i klawiatury ----------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || alpha < 0.95f) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;

        // ikony zakładek
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            if (inside(mx, my, tabX(), tabY(i), 28, 28)) {
                listening = false;
                if (tab != tabs[i]) {
                    tab = tabs[i];
                    click();
                }
                return true;
            }
        }

        int x1 = ix1();
        int x2 = ix2();
        int y = iy1();

        if (tab == Tab.MODY) {
            if (inside(mx, my, x1, y, x2 - x1, 44)) {
                NoShadows.set(!MenuConfig.noShadows);
                click();
                return true;
            }
            if (inside(mx, my, x1, y + 50, x2 - x1, 44)) {
                MenuConfig.textShadows = !MenuConfig.textShadows;
                MenuConfig.save();
                click();
                return true;
            }
        } else if (tab == Tab.USTAWIENIA) {
            for (int i = 0; i < PALETTE.length; i++) {
                if (inside(mx, my, x1 + i * 26, y + 13, 20, 20)) {
                    MenuConfig.accent = PALETTE[i];
                    MenuConfig.save();
                    click();
                    return true;
                }
            }

            int ry = y + 41;
            if (inside(mx, my, x1, ry, x2 - x1, 24)) {
                MenuConfig.rounded = !MenuConfig.rounded;
                MenuConfig.save();
                click();
                return true;
            }
            if (inside(mx, my, x1, ry + 27, x2 - x1, 24)) {
                MenuConfig.joined = !MenuConfig.joined;
                MenuConfig.save();
                click();
                return true;
            }
            if (inside(mx, my, x1, ry + 54, x2 - x1, 24)) {
                MenuConfig.background = !MenuConfig.background;
                MenuConfig.save();
                click();
                return true;
            }

            int ky = ry + 81;
            int bw = 64;
            int bx1 = x2 - 8 - bw;
            if (inside(mx, my, bx1, ky + 3, bw, 18)) {
                listening = !listening;
                click();
                return true;
            }
        }

        listening = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            listening = false;
            KeyBinding kb = MenuModClient.openKey;
            if (kb != null) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    kb.setBoundKey(InputUtil.UNKNOWN_KEY);   // Esc = puste
                } else {
                    kb.setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
                }
                KeyBinding.updateKeysByCode();
                MinecraftClient.getInstance().options.write();
            }
            click();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
