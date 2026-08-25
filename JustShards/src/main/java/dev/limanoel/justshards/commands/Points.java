package dev.limanoel.justshards.commands;


import dev.limanoel.justshards.LucoPointPlugin;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class Points implements CommandExecutor {

    private final LucoPointPlugin plugin;

    public Points(LucoPointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {

        if (!(sender instanceof Player player)) {
            return true;
        }

        int shards = plugin.getShards(player.getUniqueId());

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&x&5&A&B&F&D&9&lSHARDS ➔ Du hast &e" + shards + " &7SHARDS"));

        return true;
    }
}

