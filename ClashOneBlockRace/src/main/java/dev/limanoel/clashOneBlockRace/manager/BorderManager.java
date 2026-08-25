package dev.limanoel.clashOneBlockRace.manager;

import dev.limanoel.clashOneBlockRace.ClashOneBlockRace;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BorderManager {

    private final ClashOneBlockRace plugin;
    private final ConfigManager configManager;
    private final GameManager gameManager;

    private static class BorderData {
        int currentX;
        final int startX;
        final int minZ;
        final int maxZ;
        final int minY;
        final int maxY;
        final int thickness;
        final Material material;
        final Material glassMaterial;
        BukkitTask task;

        BorderData(int currentX, int minZ, int maxZ, int minY, int maxY, int thickness, Material material, Material glassMaterial) {
            this.currentX = currentX;
            this.startX = currentX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.minY = minY;
            this.maxY = maxY;
            this.thickness = thickness;
            this.material = material;
            this.glassMaterial = glassMaterial;
        }
    }

    private final Map<String, BorderData> activeBorders = new ConcurrentHashMap<>();

    public BorderManager(ClashOneBlockRace plugin, ConfigManager configManager, GameManager gameManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.gameManager = gameManager;
    }

    public void spawnInitialBorder(World world, int playerCount) {
        if (!configManager.isBorderEnabled() || world == null) return;

        String worldName = world.getName();
        stopBorder(worldName);

        int startX = configManager.getBorderStartXOffset();
        int thickness = configManager.getBorderThickness();
        int minY = configManager.getBorderMinY();
        int maxY = configManager.getBorderMaxY();
        int zSpacing = configManager.getSpawnZSpacing();
        int extraZ = configManager.getBorderExtraZPadding();

        int minZ = -extraZ;
        int maxZ = Math.max(0, (playerCount - 1) * zSpacing) + extraZ;
        Material material = configManager.getBorderMaterial();
        Material glass = configManager.getBorderGlassMaterial();

        BorderData data = new BorderData(startX, minZ, maxZ, minY, maxY, thickness, material, glass);
        activeBorders.put(worldName, data);

        placeWall(world, data, startX);
    }

    private void placeWall(World world, BorderData data, int frontX) {
        for (int dx = 0; dx < data.thickness; dx++) {
            int x = frontX - dx;
            for (int z = data.minZ; z <= data.maxZ; z++) {
                for (int y = data.minY; y <= data.maxY; y++) {
                    world.getBlockAt(x, y, z).setType(resolveBlockMaterial(data, y), false);
                }
            }
        }
    }

    private Material resolveBlockMaterial(BorderData data, int y) {
        if (configManager.getBorderStyle() == ConfigManager.BorderStyle.PLAIN) {
            return data.material;
        }

        int bedrockY = configManager.getBedrockY();
        int solidBottom = configManager.getBorderPatternSolidBottom();
        int solidTop = configManager.getBorderPatternSolidTop();
        int stripeHeight = Math.max(1, configManager.getBorderPatternStripeHeight());
        int stripeSolid = Math.min(stripeHeight, Math.max(0, configManager.getBorderPatternStripeSolid()));

        if (y <= bedrockY + solidBottom || y >= data.maxY - (solidTop - 1)) {
            return data.material;
        } else if (Math.floorMod(y - bedrockY, stripeHeight) < stripeSolid) {
            return data.material;
        } else {
            return data.glassMaterial;
        }
    }

    private void clearTrailingSlice(World world, BorderData data, int clearX) {
        for (int z = data.minZ; z <= data.maxZ; z++) {
            for (int y = data.minY; y <= data.maxY; y++) {
                world.getBlockAt(clearX, y, z).setType(Material.AIR, false);
            }
        }
    }

    private void placeFrontSlice(World world, BorderData data, int frontX) {
        for (int z = data.minZ; z <= data.maxZ; z++) {
            for (int y = data.minY; y <= data.maxY; y++) {
                world.getBlockAt(frontX, y, z).setType(resolveBlockMaterial(data, y), false);
            }
        }
    }

    public void startMoving(World world) {
        if (!configManager.isBorderEnabled() || world == null) return;
        String worldName = world.getName();

        BorderData data = activeBorders.get(worldName);
        if (data == null) return;

        if (data.task != null && !data.task.isCancelled()) {
            return;
        }

        int interval = configManager.getBorderMoveIntervalTicks();

        data.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameManager.isState(worldName, "ACTIVE") || world.getPlayers().isEmpty()) {
                    this.cancel();
                    return;
                }

                checkAndEliminatePlayers(world, data, worldName);

                data.currentX++;

                placeFrontSlice(world, data, data.currentX);

                int clearX = data.currentX - data.thickness;
                clearTrailingSlice(world, data, clearX);

                String moveSound = configManager.getBorderMoveSound();
                if (!moveSound.isEmpty()) {
                    Sound sound = configManager.resolveSound(moveSound);
                    if (sound != null) {
                        for (Player p : world.getPlayers()) {
                            p.playSound(p.getLocation(), sound, 0.5f, 1.0f);
                        }
                    }
                }

                checkAndEliminatePlayers(world, data, worldName);
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    private void checkAndEliminatePlayers(World world, BorderData data, String worldName) {
        List<Player> toEliminate = new ArrayList<>();

        for (Player player : world.getPlayers()) {
            if (isInsideBorder(player, data)) {
                toEliminate.add(player);
            }
        }

        for (Player player : toEliminate) {
            eliminatePlayer(player, world, data, worldName);
        }
    }

    private boolean isInsideBorder(Player player, BorderData data) {
        if (player.getGameMode() != GameMode.SURVIVAL) return false;
        double playerX = player.getLocation().getX();
        return playerX <= data.currentX + 0.5;
    }

    public void checkPlayerCollision(Player player) {
        World world = player.getWorld();
        if (world == null) return;
        String worldName = world.getName();

        BorderData data = activeBorders.get(worldName);
        if (data == null) return;

        if (isInsideBorder(player, data)) {
            eliminatePlayer(player, world, data, worldName);
        }
    }

    private void eliminatePlayer(Player player, World world, BorderData data, String worldName) {
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        player.setGameMode(GameMode.SPECTATOR);
        player.setFallDistance(0);
        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        player.setFireTicks(0);

        Location safeLoc = new Location(world, data.currentX + 5, configManager.getSpawnY() + 5, player.getLocation().getZ());
        player.teleport(safeLoc);

        configManager.sendTitle(player, "eliminated-border");
        configManager.playSound(player, "elimination");
        configManager.sendMessage(player, "type-leave-hint");

        for (Player target : world.getPlayers()) {
            if (!target.equals(player)) {
                configManager.sendMessage(target, "eliminated-border-chat", "%player%", player.getName());
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            gameManager.checkWinner(worldName);
        }, 1L);
    }

    public void stopBorder(String worldName) {
        BorderData data = activeBorders.remove(worldName);
        if (data != null && data.task != null && !data.task.isCancelled()) {
            data.task.cancel();
        }
    }
}
