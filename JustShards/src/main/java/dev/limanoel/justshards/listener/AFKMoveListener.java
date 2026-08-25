package dev.limanoel.justshards.listener;



import dev.limanoel.justshards.LucoPointPlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

public class AFKMoveListener implements Listener {

    private final LucoPointPlugin plugin;

    public AFKMoveListener(LucoPointPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();

        if (!plugin.getAfkTeleporting().contains(uuid)) return;

        if (event.getFrom().distanceSquared(event.getTo()) < 0.01) return;

        plugin.getAfkTeleporting().remove(uuid);

        event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("afk-cancel")
        ));
    }
}

