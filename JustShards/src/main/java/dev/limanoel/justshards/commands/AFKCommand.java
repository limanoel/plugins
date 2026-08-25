package dev.limanoel.justshards.commands;

import dev.limanoel.justshards.LucoPointPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AFKCommand implements CommandExecutor {

    private final LucoPointPlugin plugin;

    private final Map<UUID, Integer> countdown = new HashMap<>();
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    public AFKCommand(LucoPointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) return true;

        UUID uuid = player.getUniqueId();

        if (plugin.getAfkTeleporting().contains(uuid)) {
            player.sendMessage("§x§F§F§7§E§B§8§lSHARDS §8» §fDu bist bereits im AFK-Teleport!");
            return true;
        }

        start(player);
        return true;
    }

    private void start(Player player) {

        UUID uuid = player.getUniqueId();

        plugin.getAfkTeleporting().add(uuid);

        int startTime = 5;
        countdown.put(uuid, startTime);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            if (!player.isOnline()) {
                cancel(uuid);
                return;
            }

            if (!plugin.getAfkTeleporting().contains(uuid)) {
                cancel(uuid);
                return;
            }

            Integer timeObj = countdown.get(uuid);
            if (timeObj == null) {
                cancel(uuid);
                return;
            }

            int time = timeObj;

            if (time <= 0) {
                teleport(player);
                cancel(uuid);
                return;
            }

            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("afk-message")
                            .replace("%time%", String.valueOf(time))
            ));

            countdown.put(uuid, time - 1);

        }, 0L, 20L);

        tasks.put(uuid, task);
    }

    private void teleport(Player player) {

        FileConfiguration cfg = plugin.getConfig();

        World world = Bukkit.getWorld(cfg.getString("afk-location.world"));

        if (world == null) return;

        Location loc = new Location(
                world,
                cfg.getDouble("afk-location.x"),
                cfg.getDouble("afk-location.y"),
                cfg.getDouble("afk-location.z")
        );

        player.teleport(loc);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("afk-teleport")
        ));

        plugin.getAfkTeleporting().remove(player.getUniqueId());
    }

    private void cancel(UUID uuid) {

        countdown.remove(uuid);

        BukkitTask task = tasks.remove(uuid);

        if (task != null) {
            task.cancel();
        }

        plugin.getAfkTeleporting().remove(uuid);
    }
}
