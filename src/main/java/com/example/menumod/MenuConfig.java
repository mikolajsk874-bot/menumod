package com.example.menumod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class MenuConfig {
    public int guiColor = 0xFF3B82F6;
    public boolean rounded = true;
    public boolean noShadows = false;

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
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(I));
        } catch (Exception ignored) {
        }
    }
}
