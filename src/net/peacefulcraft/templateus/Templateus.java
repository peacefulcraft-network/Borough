package net.peacefulcraft.templateus;

import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import net.peacefulcraft.templateus.commands.ExampleCommand;
import net.peacefulcraft.templateus.config.MainConfiguration;
import net.peacefulcraft.templateus.listeners.PlayerJoinListener;
public class Templateus extends JavaPlugin {
  
  public static final String messagingPrefix = ChatColor.GREEN + "[" + ChatColor.BLUE + "PCN" + ChatColor.GREEN + "]" + ChatColor.RESET;

  private static Templateus _this;
    public static Templateus _this() { return _this; }

  private static MainConfiguration configuration;
    public static MainConfiguration getConfiguration() { return configuration; }

  /**
   * Called when Bukkit server enables the plguin
   * For improved reload behavior, use this as if it was the class constructor
   */
  public void onEnable() {
    this._this = this;
    // Save default config if one does not exist. Then load the configuration into memory
    configuration = new MainConfiguration();

    this.setupCommands();
    this.setupEventListeners();
  }

  public void logDebug(String message) {
    if (configuration.isDebugEnabled()) {
      this.getServer().getLogger().log(Level.INFO, message);
    }
  }
  
  public void logNotice(String message) {
	this.getServer().getLogger().log(Level.INFO, message);
  }

  public void logWarning(String message) {
    this.getServer().getLogger().log(Level.WARNING, message);
  }

  public void logSevere(String message) { 
    this.getServer().getLogger().log(Level.SEVERE, message);
  }

  /**
   * Called whenever Bukkit server disableds the plugin
   * For improved reload behavior, try to reset the plugin to it's initaial state here.
   */
  public void onDisable () {
    this.getServer().getScheduler().cancelTasks(this);
  }

    private void setupCommands() {
      this.getCommand("example").setExecutor(new ExampleCommand());
    }

    private void setupEventListeners() {
      this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
    }
}