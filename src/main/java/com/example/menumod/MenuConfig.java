package com.example.menumod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MenuConfig {
    // wygląd
    public int guiColor = 0xFF3B82F6;
    public boolean rounded = true;

    public int theme = 0;          // 0 = ciemny, 1 = jasny, 2 = podstawowy
    public boolean textShadow = true; // cień za tekstem
    public boolean sounds = true;

    // moduły
    public boolean noShadows = false;
    public int noShadowsKey = -1; // kod klawisza GLFW, -1 = brak
    // 0 = wyłącz cienie bloków i bytów, 1 = tylko cienie bytów, 2 = tylko cienie bloków
    public int shadowMode = 0;
    public boolean noEquipAnim = false; // brak animacji wyciągania przedmiotu

    // HUD
    public boolean hudOn = false;
    public int hudX = 4, hudY = 4;
    public boolean hudXYZ = true, hudFacing = true, hudFps = true, hudPing = true, hudTps = true;

    // profil
    public List<String> tags = new ArrayList<>();
    public Map<String, Long> playSeconds = new LinkedHashMap<>(); // "2026-10-21" -> sekundy

    /** Czy ściany bloków mają być bez cieniowania. */
    public boolean blockShadowsOff() {
        return noShadows && shadowMode != 1;
    }

    /** Czy cienie bytów (gracz, moby) mają być wyłączone. */
    public boolean entityShadowsOff() {
        return noShadows && shadowMode != 2;
    }

    public static MenuConfig I = new MenuConfig();

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("menumod.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                MenuConfig loaded = GSON.fromJson(Files.readString(FILE), MenuConfig.class);
                if (loaded != null) I = loaded;
            }
        } catch (Exception ignored) {
        }
        if (I.tags == null) I.tags = new ArrayList<>();
        if (I.playSeconds == null) I.playSeconds = new LinkedHashMap<>();
        I.guiColor |= 0xFF000000;

        // wywal wpisy starsze niż 90 dni
        String cutoff = LocalDate.now().minusDays(90).toString();
        I.playSeconds.keySet().removeIf(k -> k.compareTo(cutoff) < 0);
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(I));
        } catch (Exception ignored) {
        }
    }
}
