package dev.limanoel.justshards.placeholder;



import dev.limanoel.justshards.LucoPointPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PointsPlaceholder extends PlaceholderExpansion {

    private final LucoPointPlugin plugin;

    public PointsPlaceholder(LucoPointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "shards";
    }

    @Override
    public String getAuthor() {
        return "Limanoel";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {

        if (player == null) {
            return "";
        }

        if (params.equalsIgnoreCase("amount")) {

            return String.valueOf(
                    plugin.getShards(player.getUniqueId())
            );
        }

        return null;
    }
}

