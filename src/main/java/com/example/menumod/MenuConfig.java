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

    // dźwięki: wpisz tu dowolne ID dźwięku, np. "minecraft:entity.experience_orb.pickup"
    public boolean sounds = true;
    public String openSound = "minecraft:ui.toast.in";
    public String clickSound = "minecraft:ui.button.click";

    // moduły
    public boolean noShadows = false;
    public int noShadowsKey = -1; // kod klawisza GLFW, -1 = brak

    // profil
    public List<String> tags = new ArrayList<>();
    public Map<String, Long> playSeconds = new LinkedHashMap<>(); // "2026-10-21" -> sekundy

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
        if (I.openSound == null) I.openSound = "minecraft:ui.toast.in";
        if (I.clickSound == null) I.clickSound = "minecraft:ui.button.click";
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
