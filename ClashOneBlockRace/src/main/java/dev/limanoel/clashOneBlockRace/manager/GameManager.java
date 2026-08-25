package dev.limanoel.clashOneBlockRace.manager;

import dev.limanoel.clashOneBlockRace.ClashOneBlockRace;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    private final HashMap<String, String> gameState = new HashMap<>();
    private final HashMap<String, BukkitTask> countdownTasks = new HashMap<>();
    private final HashMap<String, BukkitTask> activeGameTasks = new HashMap<>();
    private final Map<UUID, Location> playerBedrockLocations = new ConcurrentHashMap<>();
    private final Map<UUID, TextDisplay> playerDirectionIndicators = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private final ClashOneBlockRace plugin;
    private final ConfigManager configManager;
    private BorderManager borderManager;
    private final String worldPrefix = "oneblockmultiplayer_";

    public GameManager(ClashOneBlockRace plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void setBorderManager(BorderManager borderManager) {
        this.borderManager = borderManager;
    }

    public void setGameState(String worldName, String status) {
        gameState.put(worldName, status.toUpperCase());
    }

    public String getGameState(String worldName) {
        return gameState.getOrDefault(worldName, "WAITING");
    }

    public boolean isState(String worldName, String status) {
        return getGameState(worldName).equalsIgnoreCase(status);
    }

    public HashMap<String, String> getActiveGames() {
        return gameState;
    }

    public void registerPlayerBedrock(UUID uuid, Location location) {
        playerBedrockLocations.put(uuid, location);
    }

    /**
     * Registriert die schwebende Richtungs-Anzeige eines Spielers, damit sie später
     * (z.B. bei /leave oder Elimination) wieder sauber entfernt werden kann.
     */
    public void registerDirectionIndicator(UUID uuid, TextDisplay display) {
        if (uuid == null || display == null) return;
        playerDirectionIndicators.put(uuid, display);
    }

    /**
     * Entfernt die schwebende Richtungs-Anzeige eines Spielers, falls vorhanden.
     */
    private void removeDirectionIndicator(UUID uuid) {
        TextDisplay display = playerDirectionIndicators.remove(uuid);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    /**
     * Entfernt den Bedrock-Block eines Spielers, wenn er das Match verlässt.
     */
    public void removePlayerBedrock(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        Location loc = playerBedrockLocations.remove(uuid);

        removeDirectionIndicator(uuid);

        if (loc != null && loc.getWorld() != null) {
            if (loc.getBlock().getType() == Material.BEDROCK) {
                loc.getBlock().setType(Material.AIR);
            }
        } else if (player.getWorld().getName().startsWith(worldPrefix)) {
            Location locBelow = new Location(player.getWorld(), 0, configManager.getBedrockY(), player.getLocation().getBlockZ());
            if (locBelow.getBlock().getType() == Material.BEDROCK) {
                locBelow.getBlock().setType(Material.AIR);
            }
        }
    }

    public void setCountdownTask(String worldName, BukkitTask task) {
        cancelCountdown(worldName);
        countdownTasks.put(worldName, task);
    }

    public void cancelCountdown(String worldName) {
        BukkitTask task = countdownTasks.remove(worldName);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public void cancelActiveGameTask(String worldName) {
        BukkitTask task = activeGameTasks.remove(worldName);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        if (borderManager != null) {
            borderManager.stopBorder(worldName);
        }
    }

    public void checkWinner(String worldName) {
        if (!isState(worldName, "ACTIVE")) {
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        Player survivor = null;
        int survivalCount = 0;

        for (Player player : world.getPlayers()) {
            if (player.getGameMode() == GameMode.SURVIVAL) {
                survivalCount++;
                survivor = player;
            }
        }

        if (survivalCount == 1 && survivor != null) {
            final Player winner = survivor;
            setGameState(worldName, "ENDED");
            cancelActiveGameTask(worldName);

            for (Player player : world.getPlayers()) {
                if (player.equals(winner)) {
                    configManager.sendTitle(winner, "winner");
                    configManager.playSound(winner, "winner");
                } else {
                    configManager.sendTitle(player, "game-end", "%winner%", winner.getName());
                    configManager.playSound(player, "game-end");
                }

                configManager.sendMessage(player, "winner-banner-header");
                configManager.sendMessage(player, "winner-banner-title");
                configManager.sendMessage(player, "winner-banner-winner", "%winner%", winner.getName());
                configManager.sendMessage(player, "winner-banner-footer");
            }

            int delaySeconds = configManager.getEndGameDelaySeconds();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location spawnLoc = configManager.getLobbySpawnLocation();
                for (Player player : world.getPlayers()) {
                    if (player.getGameMode() == GameMode.SPECTATOR) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                    player.teleport(spawnLoc);
                    if (plugin.getInventoryManager() != null) {
                        plugin.getInventoryManager().restoreInventory(player);
                    }
                }
                deleteWorld(world);
            }, delaySeconds * 20L);

        } else if (survivalCount == 0 && !world.getPlayers().isEmpty()) {
            setGameState(worldName, "ENDED");
            cancelActiveGameTask(worldName);

            for (Player player : world.getPlayers()) {
                configManager.playSound(player, "draw");
                configManager.sendTitle(player, "draw");
            }

            int delaySeconds = configManager.getEndGameDelaySeconds();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location spawnLoc = configManager.getLobbySpawnLocation();
                for (Player player : world.getPlayers()) {
                    if (player.getGameMode() == GameMode.SPECTATOR) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                    player.teleport(spawnLoc);
                    if (plugin.getInventoryManager() != null) {
                        plugin.getInventoryManager().restoreInventory(player);
                    }
                }
                deleteWorld(world);
            }, delaySeconds * 20L);
        }
    }

    public void startActiveGameLoop(String worldName) {
        cancelActiveGameTask(worldName);

        final int itemInterval = configManager.getItemIntervalSeconds();
        final List<Material> itemPool = configManager.loadItemPool();

        // Initiale Border spawnen
        World world = Bukkit.getWorld(worldName);
        if (world != null && borderManager != null) {
            borderManager.spawnInitialBorder(world, world.getPlayers().size());
            if (!configManager.isBorderStartWithFirstItem()) {
                borderManager.startMoving(world);
            }
        }

        BukkitTask task = new BukkitRunnable() {
            int secondsLeft = itemInterval;
            boolean firstItemDistributed = false;

            @Override
            public void run() {
                World currentWorld = Bukkit.getWorld(worldName);
                if (currentWorld == null || !isState(worldName, "ACTIVE") || currentWorld.getPlayers().isEmpty()) {
                    this.cancel();
                    return;
                }

                // Actionbar für alle Spieler in der Welt anzeigen
                for (Player player : currentWorld.getPlayers()) {
                    player.sendActionBar(configManager.getActionBarTimer(secondsLeft));
                }

                if (secondsLeft <= 0) {
                    // Border starten, sobald das erste Item vergeben wird
                    if (!firstItemDistributed) {
                        firstItemDistributed = true;
                        if (borderManager != null && configManager.isBorderStartWithFirstItem()) {
                            borderManager.startMoving(currentWorld);
                        }
                    }

                    if (!itemPool.isEmpty()) {
                        for (Player player : currentWorld.getPlayers()) {
                            if (player.getGameMode() == GameMode.SURVIVAL) {
                                Material randomMaterial = itemPool.get(random.nextInt(itemPool.size()));
                                ItemStack item = new ItemStack(randomMaterial, 1);
                                String itemName = formatItemName(randomMaterial);

                                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                                for (ItemStack drop : leftover.values()) {
                                    currentWorld.dropItemNaturally(player.getLocation(), drop);
                                }

                                configManager.playSound(player, "item-received");
                                configManager.sendMessage(player, "item-received", "%item%", itemName);
                            }
                        }
                    }

                    secondsLeft = itemInterval;
                } else {
                    secondsLeft--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        activeGameTasks.put(worldName, task);
    }

    private String formatItemName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return formatted.toString().trim();
    }

    public int getPlayerCountInWorld(World world) {
        if (world != null) {
            return world.getPlayers().size();
        }
        return 0;
    }

    public void checkWorldAfterPlayerLeave(World world) {
        if (world == null) return;
        String worldName = world.getName();
        if (!worldName.startsWith(worldPrefix)) return;

        int playerCount = world.getPlayers().size();

        if (playerCount == 0) {
            cancelCountdown(worldName);
            cancelActiveGameTask(worldName);
            deleteWorld(world);
        } else if (playerCount < configManager.getMinPlayers() && isState(worldName, "STARTING")) {
            cancelCountdown(worldName);
            setGameState(worldName, "WAITING");
            for (Player player : world.getPlayers()) {
                configManager.sendMessage(player, "countdown-cancelled");
            }
        }
    }

    public void cleanEmptyWorlds() {
        List<World> worldsToDelete = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().startsWith(worldPrefix) && world.getPlayers().isEmpty()) {
                worldsToDelete.add(world);
            }
        }

        for (World world : worldsToDelete) {
            deleteWorld(world);
        }
    }

    public void deleteWorld(World world) {
        if (world == null) return;
        String worldName = world.getName();
        File worldFolder = world.getWorldFolder();

        cancelCountdown(worldName);
        cancelActiveGameTask(worldName);
        gameState.remove(worldName);

        // Verwaiste Bedrock-/Richtungsanzeige-Referenzen für diese Welt aufräumen,
        // damit die Maps nicht mit stale Einträgen für entladene Welten volllaufen.
        playerBedrockLocations.entrySet().removeIf(entry -> {
            Location loc = entry.getValue();
            return loc != null && loc.getWorld() != null && loc.getWorld().equals(world);
        });
        playerDirectionIndicators.entrySet().removeIf(entry -> {
            TextDisplay display = entry.getValue();
            if (display == null) return true;
            if (display.isValid() && display.getWorld() != null && display.getWorld().equals(world)) {
                display.remove();
                return true;
            }
            return !display.isValid();
        });

        Location spawnLoc = configManager.getLobbySpawnLocation();
        if (spawnLoc != null && spawnLoc.getWorld() != null && !spawnLoc.getWorld().equals(world)) {
            for (Player p : world.getPlayers()) {
                p.teleport(spawnLoc);
            }
        }

        boolean unloaded = Bukkit.unloadWorld(world, false);
        if (!unloaded) {
            plugin.getLogger().warning("[ClashOneBlockRace] Konnte Welt '" + worldName + "' nicht direkt entladen.");
        }

        scheduleFolderDeletion(worldFolder, 1);
    }

    private void scheduleFolderDeletion(File folder, int attempt) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (folder != null && folder.exists()) {
                boolean deleted = deleteDirectory(folder);
                if (!deleted && attempt < 5) {
                    scheduleFolderDeletion(folder, attempt + 1);
                } else if (deleted) {
                    plugin.getLogger().info("[ClashOneBlockRace] Die Welt '" + folder.getName() + "' wurde erfolgreich gelöscht.");
                }
            }
        }, attempt == 1 ? 2L : 20L);
    }

    public void deleteAllOneBlockWorlds() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().startsWith(worldPrefix)) {
                deleteWorld(world);
            }
        }
    }

    private boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.setWritable(true);
                        file.delete();
                    }
                }
            }
            path.setWritable(true);
            return path.delete();
        }
        return false;
    }
}
