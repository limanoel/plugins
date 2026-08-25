package dev.limanoel.justCrates;

import dev.limanoel.justCrates.commands.JCratesCommand;
import dev.limanoel.justCrates.listeners.CrateListener;
import dev.limanoel.justCrates.listeners.HologramManager;
import dev.limanoel.justCrates.managers.CrateManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class JustCrates extends JavaPlugin {
   private static JustCrates instance;
   private CrateManager crateManager;
   private HologramManager hologramManager;
   private CrateListener crateListener;

   public void onEnable() {
      instance = this;
      this.saveDefaultConfig();
      this.hologramManager = new HologramManager(this);
      this.crateManager = new CrateManager(this);
      this.crateManager.loadCrates();
      this.crateListener = new CrateListener(this);
      JCratesCommand cmdHandler = new JCratesCommand(this);
      this.getCommand("jcrates").setExecutor(cmdHandler);
      this.getCommand("jcrates").setTabCompleter(cmdHandler);
      this.getServer().getPluginManager().registerEvents(this.crateListener, this);
   }

   public void onDisable() {
      if (this.hologramManager != null) {
         this.hologramManager.shutdown();
         this.hologramManager.removeAllHolograms();
      }

      if (this.crateManager != null) {
         this.crateManager.saveCrates();
      }

   }

   public void reloadAll() {
      this.reloadConfig();
      this.crateManager.reloadCrates();
   }

   public static JustCrates getInstance() {
      return instance;
   }

   public CrateManager getCrateManager() {
      return this.crateManager;
   }

   public HologramManager getHologramManager() {
      return this.hologramManager;
   }

   public CrateListener getCrateListener() {
      return this.crateListener;
   }
}
