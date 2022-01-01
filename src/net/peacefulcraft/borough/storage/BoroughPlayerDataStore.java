package net.peacefulcraft.borough.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonObject;

import org.bukkit.entity.Player;

public class BoroughPlayerDataStore {
	
	private Map<UUID, BoroughPlayer> preprocessedPlayers;
	private Map<UUID, BoroughPlayer> players;

	public BoroughPlayerDataStore() {
		this.preprocessedPlayers = new HashMap<UUID, BoroughPlayer>();
		this.players = new HashMap<UUID, BoroughPlayer>();
	}

	public BoroughPlayer preProccessPlayerJoin(UUID uuid) {
		JsonObject preferences = SQLQueries.loadBoroughPlayerPreferences(uuid);
		if (preferences == null) {
			preferences = new JsonObject();
		}

		BoroughPlayer b = new BoroughPlayer(uuid, preferences);
		synchronized(this.preprocessedPlayers) {
			this.preprocessedPlayers.put(uuid, b);
		}
		return b;
	}

	public BoroughPlayer linkPlayer(Player p) {
		BoroughPlayer b;
		synchronized(this.preprocessedPlayers) {
			b = this.preprocessedPlayers.remove(p.getUniqueId());
		}

		b.linkPlayer(p);
		synchronized(this.players) {
			this.players.put(p.getUniqueId(), b);
		}

		return b;
	}

	/**
	 * Returns the BoroughPlayer object for user with UUID.
	 * @param p
	 * @return BoroughPlayer
	 * @return NULL If an error prevented user data fetching.
	 */
	public BoroughPlayer getBoroughPlayer(Player p ) {
		return this.getBoroughPlayer(p.getUniqueId());
	}

	/**
	 * Returns the BoroughPlayer object for user with UUID.
	 * @param uuid
	 * @return BoroughPlayer
	 * @return NULL If an error prevented user data fetching.
	 */
	public BoroughPlayer getBoroughPlayer(UUID uuid) {
		BoroughPlayer b;
		synchronized(this.players) {
			b = this.players.get(uuid);
		}

		return b;
	}

	public void evictBoroughPlayerFromCache(UUID uuid) {
		synchronized(this.players) {
			this.players.remove(uuid);
		}
		synchronized(this.preprocessedPlayers) {
			this.preprocessedPlayers.remove(uuid);
		}
	}
}
