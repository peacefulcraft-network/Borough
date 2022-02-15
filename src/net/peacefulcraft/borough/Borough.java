package net.peacefulcraft.borough;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.Chunk;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import net.peacefulcraft.borough.commands.BoroughAdmin;
import net.peacefulcraft.borough.commands.ClaimCommand;
import net.peacefulcraft.borough.commands.UnclaimCommand;
import net.peacefulcraft.borough.config.MainConfiguration;
import net.peacefulcraft.borough.listeners.ChunkCacheEventListeners;
import net.peacefulcraft.borough.listeners.EntityListener;
import net.peacefulcraft.borough.listeners.PlayerBlockListener;
import net.peacefulcraft.borough.listeners.PlayerCacheEventListeners;
import net.peacefulcraft.borough.listeners.PlayerListener;
import net.peacefulcraft.borough.listeners.PlayerMovementListener;
import net.peacefulcraft.borough.listeners.VehicleListener;
import net.peacefulcraft.borough.listeners.WorldListeners;
import net.peacefulcraft.borough.storage.BoroughChunk;
import net.peacefulcraft.borough.storage.BoroughChunkPermissionLevel;
import net.peacefulcraft.borough.storage.BoroughClaimStore;
import net.peacefulcraft.borough.storage.SQLQueries;
import net.peacefulcraft.borough.storage.UUIDCache;

public class Borough extends JavaPlugin {

	public static final String messagingPrefix = ChatColor.GREEN + "[" + ChatColor.BLUE + "PCN" + ChatColor.GREEN + "]"
			+ ChatColor.RESET;

	private static Borough _this;
		public static Borough _this() { return _this; }

	private static MainConfiguration configuration;
		public static MainConfiguration getConfiguration() { return configuration; }

	private static BoroughClaimStore claimStore;
		public static BoroughClaimStore getClaimStore() { return claimStore; }

	private static UUIDCache uuidCache;
		public static UUIDCache getUUIDCache() { return uuidCache; }

	public static ExecutorService mysqlThreadPool;
	/**
	 * Called when Bukkit server enables the plguin
	 * For improved reload behavior, use this as if it was the class constructor
	 */
	public void onEnable() {
		this._this = this;
		// Save default config if one does not exist. Then load the configuration into
		// memory
		configuration = new MainConfiguration();

		mysqlThreadPool = Executors.newFixedThreadPool(configuration.getWorkerPoolSize());

		SQLQueries.setup();

		uuidCache = new UUIDCache();
		claimStore = new BoroughClaimStore();

		this.setupCommands();
		this.setupEventListeners();

		ArrayList<UUID> needToFetch = new ArrayList<UUID>();
		this.getServer().getOnlinePlayers().forEach((p) -> {
			needToFetch.add(p.getUniqueId());
		});
		logDebug("Found " + needToFetch.size() + " users who data that were already connected.");

		mysqlThreadPool.submit(() -> {
			needToFetch.forEach((uuid) -> claimStore.getClaimNamesByUser(uuid, BoroughChunkPermissionLevel.BUILDER));
			logDebug("Finished user pre-caching.");
		});

		ArrayList<ChunkEntry> chunksToLoad = new ArrayList<ChunkEntry>();
		this.getServer().getWorlds().forEach((world) -> {
			for (Chunk c: world.getLoadedChunks()) {
				chunksToLoad.add(new ChunkEntry(world.getName(), c.getX(), c.getX()));
			}
		});
		logDebug("Found " + chunksToLoad.size() + " chunks to pre-fetch that were already loaded.");
		mysqlThreadPool.submit(() -> {
			chunksToLoad.forEach((c) -> {
				BoroughChunk chunk = claimStore.getChunk(c.world, c.x, c.z);
				if (chunk.isChunkClaimed()) {
					chunk.getClaimMeta();
				}
			});
			logDebug("Finished chunk pre-caching.");
		});
	}

	private class ChunkEntry {
		String world;
		int x;
		int z;

		public ChunkEntry(String world, int x, int z) {
			this.world = world;
			this.x = x;
			this.z = z;
		}
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
	 * For improved reload behavior, try to reset the plugin to it's initaial state
	 * here.
	 */
	public void onDisable() {
		this.getServer().getScheduler().cancelTasks(this);
		try {
			mysqlThreadPool.awaitTermination(5000, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			logWarning("MySQL threadpool shutdown was interrupted. Minor dataloss may have occured.");
		}
		SQLQueries.teardown();
	}

	private void setupCommands() {
		ClaimCommand claimCommand = new ClaimCommand();
		this.getCommand("claim").setExecutor(claimCommand);
		this.getCommand("claim").setTabCompleter(claimCommand);
		this.getCommand("unclaim").setExecutor(new UnclaimCommand());		
		this.getCommand("boroughadmin").setExecutor(new BoroughAdmin());
	}

	private void setupEventListeners() {
		this.getServer().getPluginManager().registerEvents(new ChunkCacheEventListeners(), this);
		this.getServer().getPluginManager().registerEvents(new PlayerCacheEventListeners(), this);
		this.getServer().getPluginManager().registerEvents(new PlayerMovementListener(), this);

		this.getServer().getPluginManager().registerEvents(new PlayerBlockListener(), this);
		this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		this.getServer().getPluginManager().registerEvents(new EntityListener(), this);
		this.getServer().getPluginManager().registerEvents(new VehicleListener(), this);
		this.getServer().getPluginManager().registerEvents(new WorldListeners(), this);
	}
}