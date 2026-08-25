package dev.limanoel.justshards.commands;

import dev.limanoel.justshards.LucoPointPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final LucoPointPlugin plugin;

    public ReloadCommand(LucoPointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("shards.reload")) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&x&5&A&B&F&D&9&lSHARDS ➔ Du hast keine Berechtigung!"));
            return true;
        }

        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&x&5&A&B&F&D&9&lSHARDS ➔ Lade Configs neu..."));

        try {
            plugin.reloadConfig();
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&x&5&A&B&F&D&9&lSHARDS ➔ ✓ config.yml reloaded"));

            plugin.reloadShardsConfig();
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&x&5&A&B&F&D&9&lSHARDS ➔ ✓ shards.yml reloaded"));

            plugin.reloadCrateShopConfig();
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&x&5&A&B&F&D&9&lSHARDS ➔ ✓ crateshop.yml reloaded"));

            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&x&5&A&B&F&D&9&lSHARDS ➔ ✓ Alle Configs erfolgreich reloaded!"));

        } catch (Exception e) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&x&5&A&B&F&D&9&lSHARDS ➔ &cFehler beim Reload: " + e.getMessage()));
            e.printStackTrace();
        }

        return true;
    }
}
