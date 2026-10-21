package pl.menumod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class MenuScreen extends Screen {

    private enum Tab { MODY, PROFIL }

    private static final int W = 280;
    private static final int H = 180;

    private static final int COL_PANEL   = 0xE6141420;
    private static final int COL_BORDER  = 0xFF7C5CFF;
    private static final int COL_HEADER  = 0xFF1E1E30;
    private static final int COL_TITLE   = 0xFFFFFFFF;
    private static final int COL_MUTED   = 0xFF9A9AB5;
    private static final int COL_ACCENT  = 0xFFB9A6FF;

    private Tab tab = Tab.MODY;
    private int left, top;
    private ButtonWidget modyBtn, profilBtn;

    public MenuScreen() {
        super(Text.literal("Menu"));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;

        modyBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Mody"), b -> tab = Tab.MODY)
                .dimensions(left + 10, top + 28, 80, 20).build());

        profilBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Profil"), b -> tab = Tab.PROFIL)
                .dimensions(left + 94, top + 28, 80, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);

        // panel + ramka
        ctx.fill(left - 1, top - 1, left + W + 1, top + H + 1, COL_BORDER);
        ctx.fill(left, top, left + W, top + H, COL_PANEL);

        // nagłówek
        ctx.fill(left, top, left + W, top + 22, COL_HEADER);
        ctx.fill(left, top + 22, left + W, top + 23, COL_BORDER);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        modyBtn.active = tab != Tab.MODY;
        profilBtn.active = tab != Tab.PROFIL;

        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Menu"), left + 10, top + 7, COL_TITLE);

        // linia pod zakładkami
        ctx.fill(left + 10, top + 52, left + W - 10, top + 53, 0x55FFFFFF);

        if (tab == Tab.MODY) {
            renderMody(ctx);
        } else {
            renderProfil(ctx);
        }
    }

    private void renderMody(DrawContext ctx) {
        int cx = left + W / 2;
        int cy = top + 52 + (H - 52) / 2 - 4;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Chwilowo niedostępne"), cx, cy, COL_MUTED);
    }

    private void renderProfil(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        int x = left + 16;
        int y = top + 64;

        // głowa gracza
        PlayerSkinDrawer.draw(ctx, p.getSkinTextures(), x, y, 48);

        int tx = x + 62;
        ctx.drawTextWithShadow(textRenderer, Text.literal(mc.getSession().getUsername()), tx, y + 2, COL_ACCENT);

        ctx.drawTextWithShadow(textRenderer,
                Text.literal("Poziom: " + p.experienceLevel), tx, y + 16, COL_TITLE);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal(String.format("Zdrowie: %.1f / %.0f", p.getHealth(), p.getMaxHealth())), tx, y + 28, COL_TITLE);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("Głód: " + p.getHungerManager().getFoodLevel() + " / 20"), tx, y + 40, COL_TITLE);

        ctx.drawTextWithShadow(textRenderer,
                Text.literal("Pozycja: " + p.getBlockX() + ", " + p.getBlockY() + ", " + p.getBlockZ()),
                x, y + 58, COL_MUTED);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
