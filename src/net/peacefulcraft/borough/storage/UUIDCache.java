package net.peacefulcraft.borough.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.peacefulcraft.borough.Borough;

public class UUIDCache {
	
	private Map<UUID, String> uuidMap;
	private Map<String, UUID> usernameMap;

	public UUIDCache() {
		this.uuidMap = Collections.synchronizedMap(new HashMap<UUID, String>());
		this.usernameMap = Collections.synchronizedMap(new HashMap<String, UUID>());
	}

	/**
	 * Database operations are automatically performed asychroniously. Function is main-thread safe.
	 * Commits a UUID to memory and durable cache
	 * 
	 * @param uuid
	 * @param username
	 */
	public void cacheUUIDUsernameMapping(UUID uuid, String username) {
		if (this.uuidMap.containsKey(uuid)) { return; }
		
		this.uuidMap.put(uuid, username);
		this.usernameMap.put(username, uuid);
		Borough._this().getServer().getScheduler().runTaskAsynchronously(Borough._this(), () -> {
			SQLQueries.storeUUIDUsernameMapping(uuid, username);
		});
	}

	/**
	 * Performs blocking database operations. Not main-thread safe.
	 * 
	 * @param uuid UUID to resolve to a username
	 * @return NULL if no mapping is known. String if username is found.
	 */
	public String uuidToUsername(UUID uuid) {
		String username = uuidMap.get(uuid); 
		
		if (username == null) {
			username = SQLQueries.getUsernameByUUID(uuid);
			this.uuidMap.put(uuid, username);
			this.usernameMap.put(username, uuid);
		}

		return username; 
	}

	public UUID usernameToUUID(String username) {
		UUID uuid = usernameMap.get(username); 
		
		if (username == null) {
			uuid = SQLQueries.getUUIDByUsername(username);
			this.usernameMap.put(username, uuid);
			this.uuidMap.put(uuid, username);
		}

		return uuid; 
	}
}
