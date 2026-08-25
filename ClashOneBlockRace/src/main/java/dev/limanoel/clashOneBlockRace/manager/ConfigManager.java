package dev.limanoel.clashOneBlockRace.manager;

import dev.limanoel.clashOneBlockRace.ClashOneBlockRace;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final ClashOneBlockRace plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ConfigManager(ClashOneBlockRace plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public Component parseComponent(String text, String... placeholders) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        String formatted = applyPlaceholders(text, placeholders);

        if (formatted.contains("§")) {
            Component legacyParsed = LegacyComponentSerializer.legacySection().deserialize(formatted);
            return legacyParsed;
        }

        if (formatted.contains("&")) {
            formatted = formatted.replace("&0", "§0").replace("&1", "§1").replace("&2", "§2")
                    .replace("&3", "§3").replace("&4", "§4").replace("&5", "§5")
                    .replace("&6", "§6").replace("&7", "§7").replace("&8", "§8")
                    .replace("&9", "§9").replace("&a", "§a").replace("&b", "§b")
                    .replace("&c", "§c").replace("&d", "§d").replace("&e", "§e")
                    .replace("&f", "§f").replace("&k", "§k").replace("&l", "§l")
                    .replace("&m", "§m").replace("&n", "§n").replace("&o", "§o")
                    .replace("&r", "§r")
                    .replace("&A", "§a").replace("&B", "§b").replace("&C", "§c")
                    .replace("&D", "§d").replace("&E", "§e").replace("&F", "§f")
                    .replace("&K", "§k").replace("&L", "§l").replace("&M", "§m")
                    .replace("&N", "§n").replace("&O", "§o").replace("&R", "§r");

            if (!formatted.contains("<")) {
                return LegacyComponentSerializer.legacySection().deserialize(formatted);
            }
        }

        try {
            return miniMessage.deserialize(formatted);
        } catch (Exception e) {
            return Component.text(formatted);
        }
    }

    public String parseString(String text, String... placeholders) {
        if (text == null) return "";
        String formatted = applyPlaceholders(text, placeholders);

        if (formatted.contains("<") && formatted.contains(">")) {
            try {
                Component comp = miniMessage.deserialize(formatted);
                return LegacyComponentSerializer.legacySection().serialize(comp);
            } catch (Exception e) {
            }
        }

        return ChatColor.translateAlternateColorCodes('&', formatted);
    }

    private String applyPlaceholders(String text, String... placeholders) {
        if (placeholders == null || placeholders.length == 0) {
            return text;
        }
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String key = placeholders[i];
            String val = placeholders[i + 1];
            if (key != null && val != null) {
                text = text.replace(key, val);
            }
        }
        return text;
    }

    public void sendMessage(Player player, String path, String... placeholders) {
        if (player == null) return;
        String raw = getConfig().getString("messages." + path, "");
        if (raw.isEmpty()) return;

        String prefix = getConfig().getString("messages.prefix", "");
        if (!prefix.isEmpty() && !path.startsWith("winner-banner")) {
            player.sendMessage(parseComponent(prefix + raw, placeholders));
        } else {
            player.sendMessage(parseComponent(raw, placeholders));
        }
    }

    public void sendRawMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return;
        player.sendMessage(parseComponent(message));
    }

    public void sendTitle(Player player, String titleKey, String... placeholders) {
        if (player == null) return;
        String titleRaw = getConfig().getString("titles." + titleKey + ".title", "");
        String subtitleRaw = getConfig().getString("titles." + titleKey + ".subtitle", "");

        int fadeIn = getConfig().getInt("titles." + titleKey + ".fade-in", 10);
        int stay = getConfig().getInt("titles." + titleKey + ".stay", 70);
        int fadeOut = getConfig().getInt("titles." + titleKey + ".fade-out", 20);

        String title = parseString(titleRaw, placeholders);
        String subtitle = parseString(subtitleRaw, placeholders);

        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    public void playSound(Player player, String soundKey) {
        if (player == null) return;
        String soundName = getConfig().getString("sounds." + soundKey, "");
        if (soundName.isEmpty()) return;

        try {
            Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundName.toLowerCase()));
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
        } catch (Exception ignored) {
        }
    }

    public void playSound(Player player, String soundKey, float volume, float pitch) {
        if (player == null) return;
        String soundName = getConfig().getString("sounds." + soundKey, "");
        if (soundName.isEmpty()) return;

        try {
            Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundName.toLowerCase()));
            if (sound != null) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        } catch (Exception ignored) {
        }
    }

    public Sound resolveSound(String rawName) {
        if (rawName == null || rawName.isEmpty()) return null;
        try {
            return Registry.SOUNDS.get(NamespacedKey.minecraft(rawName.toLowerCase()));
        } catch (Exception e) {
            return null;
        }
    }

    public int getMinPlayers() {
        return getConfig().getInt("game.min-players", 2);
    }

    public int getCountdownSeconds() {
        return getConfig().getInt("game.countdown-seconds", 30);
    }

    public List<Integer> getCountdownAnnouncements() {
        return getConfig().getIntegerList("game.countdown-announcements");
    }

    public int getItemIntervalSeconds() {
        return getConfig().getInt("game.item-interval-seconds", 15);
    }

    public int getSpawnZSpacing() {
        return getConfig().getInt("game.spawn-z-spacing", 15);
    }

    public int getSpawnY() {
        return getConfig().getInt("game.spawn-y", 60);
    }

    public int getBedrockY() {
        return getConfig().getInt("game.bedrock-y", 59);
    }

    public int getEndGameDelaySeconds() {
        return getConfig().getInt("game.end-game-delay-seconds", 6);
    }

    public org.bukkit.Location getLobbySpawnLocation() {
        String mode = getConfig().getString("game.lobby-spawn.mode", "WORLD_SPAWN").toUpperCase();
        if ("POSITION".equals(mode)) {
            String worldName = getConfig().getString("game.lobby-spawn.world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                world = Bukkit.getWorlds().get(0);
            }
            double x = getConfig().getDouble("game.lobby-spawn.x", 0.5);
            double y = getConfig().getDouble("game.lobby-spawn.y", 64.0);
            double z = getConfig().getDouble("game.lobby-spawn.z", 0.5);
            float yaw = (float) getConfig().getDouble("game.lobby-spawn.yaw", 0.0);
            float pitch = (float) getConfig().getDouble("game.lobby-spawn.pitch", 0.0);
            return new org.bukkit.Location(world, x, y, z, yaw, pitch);
        } else {
            World mainWorld = Bukkit.getWorlds().get(0);
            return mainWorld.getSpawnLocation();
        }
    }

    public enum BorderStyle {
        PLAIN,
        PATTERN
    }

    public boolean isBorderEnabled() {
        return getConfig().getBoolean("border.enabled", true);
    }

    public BorderStyle getBorderStyle() {
        String style = getConfig().getString("border.style", "PATTERN").toUpperCase();
        try {
            return BorderStyle.valueOf(style);
        } catch (Exception e) {
            return BorderStyle.PATTERN;
        }
    }

    public int getBorderPatternSolidBottom() {
        return getConfig().getInt("border.pattern.solid-bottom", 2);
    }

    public int getBorderPatternSolidTop() {
        return getConfig().getInt("border.pattern.solid-top", 2);
    }

    public int getBorderPatternStripeHeight() {
        return getConfig().getInt("border.pattern.stripe-height", 6);
    }

    public int getBorderPatternStripeSolid() {
        return getConfig().getInt("border.pattern.stripe-solid", 2);
    }

    public Material getBorderMaterial() {
        String name = getConfig().getString("border.material", "RED_CONCRETE");
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return Material.RED_CONCRETE;
        }
    }

    public Material getBorderGlassMaterial() {
        String name = getConfig().getString("border.glass-material", "RED_STAINED_GLASS");
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return Material.RED_STAINED_GLASS;
        }
    }

    public int getBorderStartXOffset() {
        return getConfig().getInt("border.start-x-offset", -15);
    }

    public int getBorderThickness() {
        return getConfig().getInt("border.thickness", 2);
    }

    public int getBorderMinY() {
        return getConfig().getInt("border.min-y", 40);
    }

    public int getBorderMaxY() {
        return getConfig().getInt("border.max-y", 100);
    }

    public int getBorderExtraZPadding() {
        return getConfig().getInt("border.extra-z-padding", 25);
    }

    public int getBorderMoveIntervalTicks() {
        return getConfig().getInt("border.move-interval-ticks", 20);
    }

    public boolean isBorderStartWithFirstItem() {
        return getConfig().getBoolean("border.start-with-first-item", true);
    }

    public String getBorderMoveSound() {
        return getConfig().getString("border.move-sound", "");
    }

    public boolean isDirectionIndicatorEnabled() {
        return getConfig().getBoolean("direction-indicator.enabled", true);
    }

    public String getDirectionIndicatorText() {
        return getConfig().getString("direction-indicator.text", "<gold><bold>➡ BAUE HIER LANG ➡");
    }

    public int getDirectionIndicatorOffsetX() {
        return getConfig().getInt("direction-indicator.offset-x", 4);
    }

    public int getDirectionIndicatorOffsetY() {
        return getConfig().getInt("direction-indicator.offset-y", 2);
    }

    public List<Material> loadItemPool() {
        String mode = getConfig().getString("items.mode", "ALL_EXCEPT_BLACKLIST").toUpperCase();
        List<Material> pool = new ArrayList<>();

        if ("WHITELIST".equals(mode)) {
            List<String> whitelist = getConfig().getStringList("items.whitelist");
            for (String s : whitelist) {
                try {
                    Material mat = Material.valueOf(s.toUpperCase());
                    if (mat.isItem() && !mat.isAir()) {
                        pool.add(mat);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        } else {
            Set<String> blacklist = new HashSet<>(getConfig().getStringList("items.blacklist"));
            for (Material mat : Material.values()) {
                if (mat.isItem() && !mat.isAir() && !mat.isLegacy()) {
                    String name = mat.name().toUpperCase();
                    if (!blacklist.contains(name)) {
                        pool.add(mat);
                    }
                }
            }
        }

        if (pool.isEmpty()) {
            pool.add(Material.DIRT);
            pool.add(Material.COBBLESTONE);
            pool.add(Material.OAK_PLANKS);
        }

        return pool;
    }

    public Component getActionBarTimer(int secondsLeft) {
        String format = getConfig().getString("actionbar.timer", "<yellow>Nächstes Item in: <gold><bold>%seconds%s</bold></gold></yellow>");
        return parseComponent(format, "%seconds%", String.valueOf(secondsLeft));
    }

    public Component getGuiTitle() {
        return parseComponent(getConfig().getString("gui.title", "<gold><bold>OneBlockRace"));
    }

    public int getGuiSize() {
        return getConfig().getInt("gui.size", 27);
    }

    public Material getGuiJoinMaterial() {
        try {
            return Material.valueOf(getConfig().getString("gui.join-item.material", "NETHERITE_SWORD").toUpperCase());
        } catch (Exception e) {
            return Material.NETHERITE_SWORD;
        }
    }

    public int getGuiJoinSlot() {
        return getConfig().getInt("gui.join-item.slot", 13);
    }

    public String getGuiJoinName() {
        return getConfig().getString("gui.join-item.name", "<dark_aqua><bold>OneBlockRace - Join");
    }

    public List<String> getGuiJoinLore() {
        return getConfig().getStringList("gui.join-item.lore");
    }

    public Material getGuiBarrierMaterial() {
        try {
            return Material.valueOf(getConfig().getString("gui.barrier-item.material", "BARRIER").toUpperCase());
        } catch (Exception e) {
            return Material.BARRIER;
        }
    }

    public int getGuiBarrierSlot() {
        return getConfig().getInt("gui.barrier-item.slot", 26);
    }

    public String getGuiBarrierName() {
        return getConfig().getString("gui.barrier-item.name", "<dark_red><bold>Abbrechen");
    }

    public List<String> getGuiBarrierLore() {
        return getConfig().getStringList("gui.barrier-item.lore");
    }
}
