package com.example.menumod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Ekran przesuwania HUD-a: przeciągnij HUD myszką i kliknij Gotowe. */
public class HudEditScreen extends Screen {
    private boolean dragging;
    private int offX, offY;
    private int hx, hy;

    public HudEditScreen() {
        super(Text.literal("HUD"));
        hx = MenuConfig.I.hudX;
        hy = MenuConfig.I.hudY;
    }

    private void finish() {
        MenuConfig.I.hudX = hx;
        MenuConfig.I.hudY = hy;
        MenuConfig.save();
        MinecraftClient.getInstance().setScreen(new MenuScreen(2, 2));
    }

    private boolean inDone(double x, double y) {
        int bx = width / 2 - 40, by = height - 40;
        return x >= bx && x < bx + 80 && y >= by && y < by + 22;
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        MenuScreen.applyTheme(MenuConfig.I.theme);
        c.fill(0, 0, width, height, 0x80000000);

        int w = Hud.width(mc), h = Hud.height(mc);
        hx = Math.max(0, Math.min(hx, Math.max(0, width - w)));
        hy = Math.max(0, Math.min(hy, Math.max(0, height - h)));

        if (w > 0) {
            Hud.draw(c, mc, hx, hy);
            int col = dragging ? 0xFF22C55E : 0xFFFFFFFF;
            c.fill(hx - 4, hy - 3, hx + w + 4, hy - 2, col);
            c.fill(hx - 4, hy + h, hx + w + 4, hy + h + 1, col);
            c.fill(hx - 4, hy - 3, hx - 3, hy + h + 1, col);
            c.fill(hx + w + 3, hy - 3, hx + w + 4, hy + h + 1, col);
        } else {
            String s = "Wszystkie elementy HUD są wyłączone.";
            c.drawText(textRenderer, s, (width - textRenderer.getWidth(s)) / 2, height / 2, 0xFFFFFFFF, true);
        }

        String hint = "Przeciągnij HUD w wybrane miejsce";
        c.drawText(textRenderer, hint, (width - textRenderer.getWidth(hint)) / 2, height / 2 - 30, 0xFFFFFFFF, true);

        int bx = width / 2 - 40, by = height - 40;
        boolean hov = inDone(mouseX, mouseY);
        MenuScreen.rr(c, bx, by, 80, 22, 11, hov ? MenuScreen.mix(MenuScreen.ON, 0xFFFFFFFF, 0.2f) : MenuScreen.ON);
        c.drawText(textRenderer, "Gotowe", bx + (80 - textRenderer.getWidth("Gotowe")) / 2, by + 7, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inDone(mouseX, mouseY)) {
                finish();
                return true;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            int w = Hud.width(mc), h = Hud.height(mc);
            if (w > 0 && mouseX >= hx - 4 && mouseX < hx + w + 4 && mouseY >= hy - 3 && mouseY < hy + h + 1) {
                dragging = true;
                offX = (int) mouseX - hx;
                offY = (int) mouseY - hy;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging) {
            hx = (int) mouseX - offX;
            hy = (int) mouseY - offY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
            finish();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
