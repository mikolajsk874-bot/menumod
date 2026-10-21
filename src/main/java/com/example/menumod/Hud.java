package com.example.menumod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Własny HUD: XYZ, facing, fps, ping, tps. */
public class Hud {
    static final int LINE_H = 12;
    static final int LABEL = 0xFFB0B0B0;
    static final int VALUE = 0xFF2FA3A3;

    static List<String[]> entries(MinecraftClient mc) {
        List<String[]> l = new ArrayList<>();
        MenuConfig f = MenuConfig.I;
        ClientPlayerEntity p = mc.player;

        if (f.hudXYZ) {
            String v = p == null ? "0 0 0"
                    : (int) Math.floor(p.getX()) + " " + (int) Math.floor(p.getY()) + " " + (int) Math.floor(p.getZ());
            l.add(new String[]{"XYZ:", v});
        }
        if (f.hudFacing) {
            l.add(new String[]{"facing:", p == null ? "north" : p.getHorizontalFacing().asString()});
        }
        if (f.hudFps) {
            l.add(new String[]{"fps:", String.valueOf(mc.getCurrentFps())});
        }
        if (f.hudPing) {
            int ping = 0;
            if (p != null && mc.getNetworkHandler() != null) {
                PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
                if (e != null) ping = e.getLatency();
            }
            l.add(new String[]{"ping:", String.valueOf(ping)});
        }
        if (f.hudTps) {
            int tps = mc.world == null ? 20 : Math.round(mc.world.getTickManager().getTickRate());
            l.add(new String[]{"tps:", String.valueOf(tps)});
        }
        return l;
    }

    static int lineWidth(TextRenderer tr, String[] e) {
        return tr.getWidth(Text.literal(e[0]).formatted(Formatting.BOLD)) + 4 + tr.getWidth(e[1]);
    }

    public static int width(MinecraftClient mc) {
        int w = 0;
        for (String[] e : entries(mc)) w = Math.max(w, lineWidth(mc.textRenderer, e));
        return w;
    }

    public static int height(MinecraftClient mc) {
        return entries(mc).size() * LINE_H;
    }

    public static void draw(DrawContext c, MinecraftClient mc, int x, int y) {
        TextRenderer tr = mc.textRenderer;
        boolean sh = MenuConfig.I.textShadow;
        int yy = y;
        for (String[] e : entries(mc)) {
            int lw = tr.getWidth(Text.literal(e[0]).formatted(Formatting.BOLD)) + 4;
            int tw = lw + tr.getWidth(e[1]);
            c.fill(x - 2, yy - 1, x + tw + 2, yy + 10, 0x66000000);
            c.drawText(tr, Text.literal(e[0]).formatted(Formatting.BOLD), x, yy, LABEL, sh);
            c.drawText(tr, e[1], x + lw, yy, VALUE, sh);
            yy += LINE_H;
        }
    }
}
