package dev.limanoel.clashOneBlockRace.listener;

import dev.limanoel.clashOneBlockRace.manager.BorderManager;
import dev.limanoel.clashOneBlockRace.manager.ConfigManager;
import dev.limanoel.clashOneBlockRace.manager.GameManager;
import dev.limanoel.clashOneBlockRace.manager.InventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class GameStartListener implements Listener {

    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final InventoryManager inventoryManager;
    private final ConfigManager configManager;
    private final BorderManager borderManager;

    public GameStartListener(JavaPlugin plugin, GameManager gameManager, InventoryManager inventoryManager, ConfigManager configManager, BorderManager borderManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.inventoryManager = inventoryManager;
        this.configManager = configManager;
        this.borderManager = borderManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        World currentWorld = player.getWorld();
        String worldName = currentWorld.getName();

        if (!worldName.toLowerCase().contains("oneblock")) {
            return;
        }

        if (gameManager.isState(worldName, "STARTING") || gameManager.isState(worldName, "WAITING")) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getX() != to.getX() || from.getZ() != to.getZ() || from.getY() != to.getY())) {
                event.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
                return;
            }
        }

        if (player.getLocation().getY() < 0) {
            handleVoidFall(player, currentWorld, worldName);
        }

        if (gameManager.isState(worldName, "ACTIVE") && borderManager != null) {
            borderManager.checkPlayerCollision(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        World world = player.getWorld();
        String worldName = world.getName();

        if (!worldName.toLowerCase().contains("oneblock")) {
            return;
        }

        if (gameManager.isState(worldName, "WAITING") || gameManager.isState(worldName, "STARTING")) {
            event.setCancelled(true);
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                handleVoidFall(player, world, worldName);
            }
            return;
        }

        if (gameManager.isState(worldName, "ACTIVE") && event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            handleVoidFall(player, world, worldName);
        }
    }

    private void handleVoidFall(Player player, World world, String worldName) {
        if (gameManager.isState(worldName, "ACTIVE")) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SPECTATOR);
                player.setFallDistance(0);
                player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                player.setFireTicks(0);

                Location specLoc = new Location(world, 0, configManager.getSpawnY() + 5, player.getLocation().getZ());
                player.teleport(specLoc);

                configManager.sendTitle(player, "eliminated-void");
                configManager.playSound(player, "elimination");
                configManager.sendMessage(player, "type-leave-hint");

                for (Player target : world.getPlayers()) {
                    if (!target.equals(player)) {
                        configManager.sendMessage(target, "eliminated-void-chat", "%player%", player.getName());
                    }
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    gameManager.checkWinner(worldName);
                }, 1L);
            }
        } else {
            player.setFallDistance(0);
            player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            player.teleport(new Location(world, 0, configManager.getSpawnY(), player.getLocation().getZ()));
        }
    }

    @EventHandler
    public void onSpectatorTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getName().toLowerCase().contains("oneblock")) {
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
                event.setCancelled(true);
                configManager.sendMessage(player, "spectator-teleport-blocked");
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.getWorld().getName().toLowerCase().contains("oneblock")) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            gameManager.removePlayerBedrock(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !player.getWorld().getName().toLowerCase().contains("oneblock")) {
                if (inventoryManager.hasSavedInventory(player)) {
                    inventoryManager.restoreInventory(player);
                }
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerJoinWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World currentWorld = player.getWorld();
        String worldName = currentWorld.getName();

        World leavedworld = event.getFrom();

        if (leavedworld.getName().toLowerCase().contains("oneblock")) {
            gameManager.removePlayerBedrock(player);

            if (!currentWorld.getName().toLowerCase().contains("oneblock")) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        inventoryManager.restoreInventory(player);
                    }
                }, 1L);
            }

            for (Player target : leavedworld.getPlayers()) {
                if (!target.equals(player)) {
                    configManager.sendMessage(target, "player-left", "%player%", player.getName());
                }
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                gameManager.checkWorldAfterPlayerLeave(leavedworld);
                gameManager.checkWinner(leavedworld.getName());
            }, 2L);
        }

        if (worldName.toLowerCase().contains("oneblock")) {
            int playerCount = currentWorld.getPlayers().size();

            configManager.sendMessage(player, "players-in-lobby", "%count%", String.valueOf(playerCount));

            for (Player target : currentWorld.getPlayers()) {
                if (!target.equals(player)) {
                    configManager.sendMessage(target, "player-joined", "%player%", player.getName());
                }
            }

            if (playerCount >= configManager.getMinPlayers() && gameManager.isState(worldName, "WAITING")) {
                gameManager.setGameState(worldName, "STARTING");
                startCountdown(worldName, "WAITING");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        String worldName = world.getName();

        if (worldName.toLowerCase().contains("oneblock")) {
            gameManager.removePlayerBedrock(player);
            inventoryManager.restoreInventory(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                gameManager.checkWorldAfterPlayerLeave(world);
                gameManager.checkWinner(worldName);
            }, 2L);
        }
    }

    @EventHandler
    public void onPlayerSwitchGamemodeEvent(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        if (worldName.toLowerCase().contains("oneblock") && gameManager.isState(worldName, "ACTIVE")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                gameManager.checkWinner(worldName);
            }, 1L);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && inventoryManager.hasSavedInventory(player)) {
                    inventoryManager.restoreInventory(player);
                    configManager.sendMessage(player, "inventory-restored-rejoin");
                }
            }
        }.runTaskLater(plugin, 20L);
    }

    public void startCountdown(String worldName, String status) {
        if (!worldName.toLowerCase().contains("oneblock")) {
            return;
        }

        final int countdownDuration = configManager.getCountdownSeconds();
        final List<Integer> announcements = configManager.getCountdownAnnouncements();
        final int minPlayers = configManager.getMinPlayers();

        BukkitTask task = new BukkitRunnable() {
            int secondsLeft = countdownDuration;

            @Override
            public void run() {
                World bukkitWorld = Bukkit.getWorld(worldName);

                if (bukkitWorld == null) {
                    this.cancel();
                    return;
                }

                int playerCount = bukkitWorld.getPlayers().size();
                if (playerCount < minPlayers) {
                    gameManager.cancelCountdown(worldName);
                    gameManager.setGameState(worldName, "WAITING");
                    for (Player player : bukkitWorld.getPlayers()) {
                        configManager.sendMessage(player, "countdown-cancelled");
                    }
                    this.cancel();
                    return;
                }

                for (Player player : bukkitWorld.getPlayers()) {
                    if (announcements.contains(secondsLeft)) {
                        configManager.sendMessage(player, "countdown-announcement", "%seconds%", String.valueOf(secondsLeft));
                    }

                    if (secondsLeft <= 5 && secondsLeft > 0) {
                        configManager.sendTitle(player, "countdown", "%seconds%", String.valueOf(secondsLeft));
                        configManager.playSound(player, "countdown-tick");
                    }
                }

                if (secondsLeft <= 0) {
                    gameManager.setGameState(worldName, "ACTIVE");

                    for (Player player : bukkitWorld.getPlayers()) {
                        configManager.sendTitle(player, "start");
                        configManager.playSound(player, "game-start");
                        configManager.sendMessage(player, "game-started");
                    }

                    gameManager.startActiveGameLoop(worldName);

                    this.cancel();
                    return;
                }

                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        gameManager.setCountdownTask(worldName, task);
    }
}
