package pl.menumod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Ustawienia moda, zapisywane w config/menumod.properties. */
public final class MenuConfig {

    private MenuConfig() {}

    public static int accent = 0x7C5CFF;
    public static boolean rounded = true;
    public static boolean joined = false;
    public static boolean background = true;

    public static volatile boolean noShadows = false;
    public static volatile boolean textShadows = false;

    public static boolean prevEntityShadows = true;
    public static boolean prevAo = true;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("menumod.properties");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try (InputStream in = Files.newInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            accent = Integer.parseInt(p.getProperty("accent", Integer.toString(accent)));
            rounded = Boolean.parseBoolean(p.getProperty("rounded", "true"));
            joined = Boolean.parseBoolean(p.getProperty("joined", "false"));
            background = Boolean.parseBoolean(p.getProperty("background", "true"));
            noShadows = Boolean.parseBoolean(p.getProperty("noShadows", "false"));
            textShadows = Boolean.parseBoolean(p.getProperty("textShadows", "false"));
            prevEntityShadows = Boolean.parseBoolean(p.getProperty("prevEntityShadows", "true"));
            prevAo = Boolean.parseBoolean(p.getProperty("prevAo", "true"));
        } catch (IOException | NumberFormatException ignored) {
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("accent", Integer.toString(accent));
        p.setProperty("rounded", Boolean.toString(rounded));
        p.setProperty("joined", Boolean.toString(joined));
        p.setProperty("background", Boolean.toString(background));
        p.setProperty("noShadows", Boolean.toString(noShadows));
        p.setProperty("textShadows", Boolean.toString(textShadows));
        p.setProperty("prevEntityShadows", Boolean.toString(prevEntityShadows));
        p.setProperty("prevAo", Boolean.toString(prevAo));
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "Menu Mod");
        } catch (IOException ignored) {
        }
    }
}
