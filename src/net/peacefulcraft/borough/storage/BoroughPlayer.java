package net.peacefulcraft.borough.storage;

import java.util.UUID;

import com.google.gson.JsonObject;

import org.bukkit.entity.Player;

import net.peacefulcraft.borough.Borough;

public class BoroughPlayer {
	
	private UUID uuid;

	private JsonObject preferences;

	private Player user;
		public Player getPlayer() { return user; }

	/** @return True if player has action bar preference */
	public boolean doesShowActionBar() {
		if (this.preferences.has("show_action_bar")) {
			return this.preferences.get("show_action_bar").getAsBoolean();
		} else {
			// default to true if unset
			return true;
		}
	}

	public void setShowActionBar(boolean show) {
		this.preferences.addProperty("show_action_bar", show);
		this.asyncSavePlayerPreferences();
	}

	/**
	 * Initializes player profile
	 * @param uuid Player UUID
	 * @param playerPreferences Player's preferences
	 */
	public BoroughPlayer(UUID uuid, JsonObject playerPreferences) {
		this.uuid = uuid;
		this.preferences = playerPreferences;
	}

	/**
	 * @param p
	 */
	public void linkPlayer(Player p) {
		this.user = p;
	}

	public void asyncSavePlayerPreferences() {
		Borough._this().getServer().getScheduler().runTaskAsynchronously(Borough._this(), () -> {
			SQLQueries.saveBoroughPlayerPreferences(this.uuid, this.preferences);
		});
	}
}
