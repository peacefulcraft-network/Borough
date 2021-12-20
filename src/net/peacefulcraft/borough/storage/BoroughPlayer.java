package net.peacefulcraft.borough.storage;

import java.util.UUID;

import org.bukkit.entity.Player;

public class BoroughPlayer {
	
	private UUID uuid;

	private Player user;

	private long playerRegistryId;

	// Player action bar preferences
	private boolean showActionBar;
		/** @return True if player has action bar preference */
		public boolean doesShowActionBar() { return showActionBar; }

	/**
	 * Initializes player profile
	 * @param id Player UUID
	 */
	public BoroughPlayer(UUID id, long playerRegistryId) {
		this.uuid = id;
		this.playerRegistryId = playerRegistryId;
	}

	/**
	 * 
	 * @param p
	 */
	public void linkPlayer(Player p) {
		this.user = p;
		//TODO: Link player with SQL database
		//TODO: Load their action bar preferences
	}

}
