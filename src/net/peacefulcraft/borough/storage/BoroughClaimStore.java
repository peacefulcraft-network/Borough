package net.peacefulcraft.borough.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import net.peacefulcraft.borough.Borough;

public class BoroughClaimStore {
	
	/*
		Populated by SQLQueries because BoroughChunk objects and BoroughClaim objects
		are tightly coupled so it is easier to construct + cache them together and then
		return whatever the query is supposed to provide.
	*/
	private Map<String, BoroughClaim> claimCache;
		public synchronized int getClaimCacheSize() { return claimCache.size(); }
		public synchronized Set<String> getClaimCacheKeys() { return claimCache.keySet(); }
	private Map<String, BoroughChunk> chunkCache;
	private Map<UUID, List<String>> claimMembershipCache;
		public synchronized int getMemberCacheSize() { return claimMembershipCache.size(); }
		public synchronized Set<UUID> getMemberCacheKeys() { return claimMembershipCache.keySet(); }
	private Map<UUID, List<BoroughChunkPermissionLevel>> claimPermissionsCache;
		public synchronized int getPermissionCacheSize() { return claimPermissionsCache.size(); }
		public synchronized Set<UUID> getPermissionCacheKeys() { return claimPermissionsCache.keySet(); }

	/*
		Player profile maps
	*/
	protected Map<UUID, BoroughPlayer> preprocessedPlayers;
	protected Map<UUID, BoroughPlayer> players;

	public BoroughClaimStore() {
		this.claimCache = Collections.synchronizedMap(new HashMap<String, BoroughClaim>());
		this.chunkCache = Collections.synchronizedMap(new HashMap<String, BoroughChunk>());
		this.claimMembershipCache = Collections.synchronizedMap(new HashMap<UUID, List<String>>());
		this.claimPermissionsCache = Collections.synchronizedMap(new HashMap<UUID, List<BoroughChunkPermissionLevel>>());

		this.preprocessedPlayers = Collections.synchronizedMap(new HashMap<UUID, BoroughPlayer>());
		this.players = Collections.synchronizedMap(new HashMap<UUID, BoroughPlayer>());
	}

	protected static String getClaimKey(String owner, String claimName) {
		return owner + ":" + claimName;
	}

	protected static String[] splitClaimKey(String claimKey) {
		String[] parts = claimKey.split(":");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Malformed claim key " + claimKey);
		}

		return parts;
	}

	protected static String getChunkKey(BoroughChunk chunk) {
		return getChunkKey(chunk.getWorld(), chunk.getChunkX(), chunk.getChunkZ());
	}

	protected static String getChunkKey(String world, int x, int z) {
		return world + "_" + x + "_" + z;
	}

	public void evictCachedChunk(BoroughChunk chunk) {
		this.evictCachedChunk(chunk.getWorld(), chunk.getChunkX(), chunk.getChunkZ());
	}

	public void evictCachedChunk(String world, int x, int z) {
		synchronized(this.chunkCache) {
			this.chunkCache.remove(getChunkKey(world, x, z));
		}
	}

	public void evictCachedUserData(UUID user) {
		synchronized(this.claimMembershipCache) {
			this.claimMembershipCache.remove(user);
		}

		synchronized(this.claimPermissionsCache) {
			this.claimPermissionsCache.remove(user);
		}
	}

	/**
	 * This method should be invoked async
	 * @param uuid Player UUID
	 * @param playerRegistryId player registry in DB
	 * @throws RuntimeException
	 */
	public void preProcessPlayerJoin(UUID uuid, long playerRegistryId) throws RuntimeException {
		if (players.containsKey(uuid)) {
			throw new RuntimeException("Command executor is already in Borough");
		}

		try {
			BoroughPlayer b = new BoroughPlayer(uuid, playerRegistryId);
			preprocessedPlayers.put(uuid, b);
		} catch (Exception e) {
			e.printStackTrace();
			if (e.getCause() != null) {
				e.getCause().printStackTrace();
			}
			throw new RuntimeException("An error occured while processing player " + uuid);
		}
	}

	public void processPlayerJoin(Player p) {
		BoroughPlayer b = preprocessedPlayers.remove(p.getUniqueId());
		if (b == null) {
			p.kickPlayer("[Borough] Database error. Unable to load profile from registry.");
			return;
		}

		try {
			b.linkPlayer(p);
		} catch (RuntimeException e) {
			p.kickPlayer("[Borough] Database error occured while loading player.");
			e.printStackTrace();
			return;
		}

		players.put(p.getUniqueId(), b);
	} 

	public void leaveGame(Player p) {
		BoroughPlayer b = players.get(p.getUniqueId());
		if (b == null) {
			throw new RuntimeException("Command executor is not in Borough");
		}

		players.remove(p.getUniqueId());
	}

	/**
	 * Creates a new named claim object. Performs blocking SQL work.
	 * Inserts BoroughChunk object into cache
	 * 
	 * @param bc BoroughChunk object
	 */
	public void insertChunkCache(BoroughChunk bc) {

		if (bc == null) { return; }

		synchronized(this.chunkCache) {
			this.chunkCache.put(getChunkKey(bc), bc);
		}
	}

	/**
	 * Creates a new named claim object.
	 * 
	 * @param claimName Claim name
	 * @param owner UUID of claim owner
	 * @return BoroughClaim object. If claim already exists, the existing claim object is silently returned.
	 */
	public BoroughClaim createClaim(String claimName, UUID owner) {
		String username = Borough.getUUIDCache().uuidToUsername(owner);
		BoroughClaim claim = getClaim(getClaimKey(username, claimName));
		if (claim == null) {
			claim = SQLQueries.createClaim(claimName, owner);

			synchronized(this.claimMembershipCache) {
				List<String> claimMemberships = this.claimMembershipCache.get(owner);
				if (claimMemberships == null) {
					// pull users stuff if we somehow don't already have it
					this.getClaimNamesByUser(owner, BoroughChunkPermissionLevel.BUILDER);
					claimMemberships = this.claimMembershipCache.get(owner);
				}
			
				// Nest this sync so we ensure indexs match
				synchronized(this.claimMembershipCache) {
					List<BoroughChunkPermissionLevel> claimPermission = this.claimPermissionsCache.get(owner);
					if (claimPermission == null) {
						claimPermission = Collections.synchronizedList(new ArrayList<BoroughChunkPermissionLevel>());
						this.claimPermissionsCache.put(owner, claimPermission);
					}
					claimPermission.add(BoroughChunkPermissionLevel.OWNER);			
				}
			}
		}

		return claim;
	}

	/**
	 * Get the BuroughClaim meta object by the given claim name. Performs blocking SQL work.
	 * @param name Claim name
	 * @return Claim meta object or NULL of no such claim exists
	 */
	public BoroughClaim getClaim(String name) throws IllegalArgumentException {
		BoroughClaim claim = null;
		synchronized(this.claimCache) {
			claim = this.claimCache.get(name);
		}

		String[] nameParts = splitClaimKey(name);
		UUID owner = Borough.getUUIDCache().usernameToUUID(nameParts[0]);
		if (owner == null) {
			throw new IllegalArgumentException("No known user " + nameParts[0] + ".");
		}

		if (claim == null) {
			claim = SQLQueries.getBoroughClaim(nameParts[1], owner);
			synchronized(this.claimCache) {
				// Other threads may have been fetching while we were.
				// If they beat us, yeild and use their objects.
				BoroughClaim winner = this.claimCache.get(getClaimKey(nameParts[0], nameParts[1]));
				if (winner == null) {
					claimCache.put(getClaimKey(nameParts[0], nameParts[1]), claim);
				} else {
					return winner;
				}
			}
		} else {
			synchronized(this.claimCache) {
				this.claimCache.put(getClaimKey(nameParts[0], nameParts[1]), claim);
			}
		}

		return claim;
	}

	/**
	 * Delete the provided claim. Performs blocking SQL work.
	 * 
	 * @param name Name of claim to delete
	 * 
	 * @throws IllegalArgumentException It no claim called `name` exists.
	 */
	public void deleteClaim(String name) {
		Borough._this().logDebug("Received delete request for claim " + name);
		BoroughClaim claim;
		synchronized(this.claimCache) {
			claim = this.getClaim(name);
		}
		
		if (claim == null) {
			throw new IllegalArgumentException("Unknown claim " + name + ". Delete failed");
		}

		SQLQueries.deleteClaim(claim);
		claim.getChunks().forEach((chunk) -> {
			chunk.clearClaimMeta();
		});

		synchronized(this.claimCache) {
			this.claimCache.remove(name).getChunks().clear();
		}

		// Async refresh the users claim caches
		Borough._this().getServer().getScheduler().runTaskAsynchronously(Borough._this(), () -> {
			claim.getBuilders().forEach((uuid) -> {
				this.getClaimNamesByUser(uuid, BoroughChunkPermissionLevel.BUILDER);
			});
			claim.getBuilders().clear();;

			claim.getModerators().forEach((uuid) -> {
				this.getClaimNamesByUser(uuid, BoroughChunkPermissionLevel.BUILDER);
			});
			claim.getModerators().clear();

			claim.getOwners().forEach((uuid) -> {
				this.getClaimNamesByUser(uuid, BoroughChunkPermissionLevel.BUILDER);
			});
			claim.getOwners().clear();
		});
	}

	/**
	 * Claim the requested chunk if it is not already claimed. Performs blocking SQL work.
	 * 
	 * @param world World chunk is in
	 * @param x Chunk x coordinate. (Not world coordinates)
	 * @param z Chunk z coordinate. (Not world coordinates)
	 * @param String Claim to add this chunk too
	 * @return BoroughChunk object for the newly claimed chunk.
	 */
	public BoroughChunk claimChunk(String world, int x, int z, BoroughClaim claim) {
		BoroughChunk chunk = this.getChunk(world, x, z);

		if (chunk.isChunkClaimed()) {
		// Claimed by another claim zone
			throw new IllegalArgumentException("Chunk is already claimed by claim " + claim.getClaimName());

		} else {
		// Unclaimed, claim it and return
			SQLQueries.claimChunk(claim, chunk);
			claim.getChunks().add(chunk);
			chunk.setClaimMeta(getClaimKey(claim.getCreatorUsername(), claim.getClaimName()));
		}

		return chunk;
	}

	/**
	 * Get claim information about the requested chunk. Performs blocking SQL work.
	 * 
	 * @param world World chunk is in
	 * @param x Chunk x coordinate. (Not world coordinates)
	 * @param z Chunk z coordinate. (Not world coordinates)
	 * @return BoroughChunk object.
	 */
	public BoroughChunk getChunk(String world, int x, int z) {
		// Check cache
		BoroughChunk chunk = null;
		synchronized(this.chunkCache) {
			chunk = this.chunkCache.get(getChunkKey(world, x, z));
		}
		
		// Check DB
		if (chunk == null) {
			chunk = SQLQueries.getBoroughChunk(world, x, z);
			synchronized(this.chunkCache) {
				this.chunkCache.put(getChunkKey(world, x, z), chunk);
			}
		}

		// Doesn't exist. Return wrapper with null claim meta.
		if (chunk == null) {
			chunk = new BoroughChunk("", world, x, z);
			synchronized(this.chunkCache) {
				this.chunkCache.put(getChunkKey(world, x, z), chunk);
			}
		}

		return chunk;
	}

	/**
	 * Get claim information about requested chunk
	 * 
	 * @param loc Raw location of event
	 * @return BoroughChunk object or NULL if chunk is not claimed
	 */
	public BoroughChunk getChunk(Location loc) {
		return getChunk(
			loc.getWorld().getName(),
			loc.getChunk().getX(),
			loc.getChunk().getZ()	
		);
	}

	/**
	 * Unclaim the requested chunk if it is claimed. No effect if chunk was not claimed.
	 * 
	 * @param world World chunk is in
	 * @param x Chunk x coordinate. (Not world coordinates)
	 * @param z Chunk z coordinate. (Not world coordinates)
	 */
	public void unclaimChunk(String world, int x, int z) {
		BoroughChunk chunk = this.getChunk(world, x, z);
		if (!chunk.isChunkClaimed()) { return; }
		else {
			// claimed. Delete it.
			SQLQueries.unclaimChunk(chunk);
			chunk.getClaimMeta().getChunks().remove(chunk);
			chunk.clearClaimMeta();
		}
	}

	/**
	 * Get a list of all claims which this user has access to. Performs **BLOCKING** SQL work.
	 * 
	 * @param user
	 * @param permissionFilter Minium permission level to filter for. IE: Moderator will return all claims
	 *                         where user has moderator or ownership permissions.
	 * @return List of claim names which the user has access to at the requested permission level
	 */
	public List<String> getClaimNamesByUser(UUID user, BoroughChunkPermissionLevel permissionFilter) {
		List<String> claimNames = new ArrayList<String>();
		List<BoroughChunkPermissionLevel> claimPerms = new ArrayList<BoroughChunkPermissionLevel>();
		SQLQueries.getClaimsByUser(user, claimNames, claimPerms);

		synchronized(this.claimMembershipCache) {
			this.claimMembershipCache.put(user, Collections.synchronizedList(claimNames));
		}

		synchronized(this.claimPermissionsCache) {
			this.claimPermissionsCache.put(user, Collections.synchronizedList(claimPerms));
		}

		List<String> results = new ArrayList<String>();

		for (int i=0; i<claimNames.size(); i++) {
			if (claimPerms.get(i).compareTo(permissionFilter) >= 0) {
				results.add(claimNames.get(i));
			}
		}
		
		return results;
	}

	/**
	 * Main visualizer for chunk mapping
	 * Generates message depicting nearby
	 * claimed and unclaimed chunks
	 * 
	 * @param p Player we are fetching nearby
	 */
	public void visualizeChunks(Player p) {
		
		World world = p.getWorld();
		//int baseX = p.getLocation().getChunk().getX();
		//int baseZ = p.getLocation().getChunk().getZ();

		HashMap<Integer, ArrayList<BoroughChunk>> chunksAroundPlayer = new HashMap<>();
		HashMap<Integer, ChatColor> colorMap = new HashMap<>();
		Random r = new Random();
		String message = "";

		colorMap.put(-1, ChatColor.GRAY);

		for (int x = -5; x <= 5; x++) {
			for (int z = -5; z <= 5; z++) {
				BoroughChunk chunk = getChunk(world.getName(), x, z);
				
				int id = chunk.isChunkClaimed() ? chunk.getClaimMeta().getClaimId() : -1;

				if (!chunksAroundPlayer.containsKey(id)) {
					chunksAroundPlayer.put(id, new ArrayList<>());
				}
				chunksAroundPlayer.get(id).add(chunk);

				if (!colorMap.containsKey(id)) {
					colorMap.put(id, ChatColor.values()[r.nextInt(ChatColor.values().length - 1)]);
				}

				String c = id == -1 ? "O" : "X";
				message += colorMap.get(id) + c;

				if (z == 5) { message += "\n"; }
			}
		}

		message += "Key: \n";
		for (Entry<Integer, ArrayList<BoroughChunk>> entry : chunksAroundPlayer.entrySet()) {
			int id = entry.getKey();
			BoroughChunk chunk = entry.getValue().get(0);

			if (id == -1) { continue; }
			message += colorMap.get(id) + chunk.getClaimMeta().getClaimName() + "\n";
		}

		p.sendMessage(Borough.messagingPrefix + message);
	}
}
