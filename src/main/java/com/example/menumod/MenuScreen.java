package com.example.menumod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public class MenuScreen extends Screen {
    static final String[] NAMES = {"Gracz", "Ustawienia", "Mody", "Profil"};
    static final Item[] ICONS = {Items.PLAYER_HEAD, Items.COMPARATOR, Items.CHEST, Items.NAME_TAG};
    static final int[] COLORS = {0xFF3B82F6, 0xFF22C55E, 0xFFEF4444, 0xFFA855F7, 0xFFF59E0B};
    static final int W = 320, H = 200;

    private final long openedAt = Util.getMeasuringTimeMs();
    private int tab = 1;
    private int px, py;

    public MenuScreen() {
        super(Text.literal("MenuMod"));
        // dźwięk otwarcia menu
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.UI_TOAST_IN, 1.0f));
    }

    private static void box(DrawContext c, int x, int y, int w, int h, int col) {
        if (!MenuConfig.I.rounded) {
            c.fill(x, y, x + w, y + h, col);
            return;
        }
        c.fill(x + 2, y, x + w - 2, y + h, col);
        c.fill(x, y + 2, x + w, y + h - 2, col);
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, col);
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void row(DrawContext c, int x, int y, String label, boolean on) {
        box(c, x, y, W - 50, 24, 0xFF252B33);
        c.drawText(textRenderer, label, x + 8, y + 8, 0xFFFFFFFF, false);
        c.drawText(textRenderer, on ? "WŁ." : "WYŁ.", x + W - 50 - 36, y + 8,
                on ? 0xFF22C55E : 0xFFEF4444, false);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        float t = Math.min(1f, (Util.getMeasuringTimeMs() - openedAt) / 250f);
        float e = 1f - (float) Math.pow(1f - t, 3); // ease-out
        px = (width - W) / 2;
        py = (height - H) / 2;

        // przyciemnienie tła (fade-in)
        c.fill(0, 0, width, height, ((int) (110 * e)) << 24);

        // animacja rozwijania panelu
        var m = c.getMatrices();
        m.push();
        float s = 0.8f + 0.2f * e;
        m.translate(width / 2f, height / 2f, 0);
        m.scale(s, s, 1);
        m.translate(-width / 2f, -height / 2f, 0);

        box(c, px, py, W, H, 0xF0101418);
        c.drawText(textRenderer, NAMES[tab], px + 10, py + 10, MenuConfig.I.guiColor, true);

        // ikony po prawej
        for (int i = 0; i < 4; i++) {
            int sx = px + W - 32, sy = py + 10 + i * 30;
            box(c, sx, sy, 24, 24, i == tab ? MenuConfig.I.guiColor : 0xFF252B33);
            c.drawItem(new ItemStack(ICONS[i]), sx + 4, sy + 4);
        }

        switch (tab) {
            case 1 -> {
                c.drawText(textRenderer, "Kolor GUI", px + 10, py + 40, 0xFFFFFFFF, false);
                for (int i = 0; i < COLORS.length; i++) {
                    box(c, px + 10 + i * 26, py + 54, 20, 20, COLORS[i]);
                }
                row(c, px + 10, py + 90, "Zaokrąglone ramki", MenuConfig.I.rounded);
            }
            case 2 -> row(c, px + 10, py + 40, "Brak cieni", MenuConfig.I.noShadows);
            default -> c.drawText(textRenderer, "Wkrótce", px + 10, py + 40, 0xFFAAAAAA, false);
        }

        m.pop();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < 4; i++) {
            if (hit(mx, my, px + W - 32, py + 10 + i * 30, 24, 24)) {
                tab = i;
                return true;
            }
        }

        if (tab == 1) {
            for (int i = 0; i < COLORS.length; i++) {
                if (hit(mx, my, px + 10 + i * 26, py + 54, 20, 20)) {
                    MenuConfig.I.guiColor = COLORS[i];
                    MenuConfig.save();
                    return true;
                }
            }
            if (hit(mx, my, px + 10, py + 90, W - 50, 24)) {
                MenuConfig.I.rounded = !MenuConfig.I.rounded;
                MenuConfig.save();
                return true;
            }
        }

        if (tab == 2 && hit(mx, my, px + 10, py + 40, W - 50, 24)) {
            MenuConfig.I.noShadows = !MenuConfig.I.noShadows;
            MenuConfig.save();
            NoShadows.apply(MinecraftClient.getInstance());
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }
}
