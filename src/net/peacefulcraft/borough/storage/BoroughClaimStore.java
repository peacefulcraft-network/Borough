package net.peacefulcraft.borough.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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
	protected Map<String, BoroughClaim> claimCache;
	protected Map<String, BoroughChunk> chunkCache;
	protected Map<UUID, List<String>> claimMembershipCache;
	protected Map<UUID, List<BoroughChunkPermissionLevel>> claimPermissionsCache;

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
		this.chunkCache.remove(getChunkKey(world, x, z));
	}

	public void evictCachedUserData(UUID user) {
		this.claimMembershipCache.remove(user);
		this.claimPermissionsCache.remove(user);
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

		this.chunkCache.put(
			getChunkKey(bc), 
			bc
		);
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

			// TODO: Make async safe.
			this.claimMembershipCache.get(owner).add(getClaimKey(username, claimName));
			this.claimPermissionsCache.get(owner).add(BoroughChunkPermissionLevel.OWNER);
		}

		return claim;
	}

	/**
	 * Get the BuroughClaim meta object by the given claim name. Performs blocking SQL work.
	 * @param name Claim name
	 * @return Claim meta object or NULL of no such claim exists
	 */
	public BoroughClaim getClaim(String name) throws IllegalArgumentException {
		BoroughClaim claim = this.claimCache.get(name);
		if (claim == null) {
			String[] nameParts = splitClaimKey(name);
			UUID owner = Borough.getUUIDCache().usernameToUUID(nameParts[0]);
			if (owner == null) {
				throw new IllegalArgumentException("No known user " + nameParts[0] + ".");
			}

			claim = SQLQueries.getBoroughClaim(nameParts[1], owner);
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
		BoroughClaim claim = this.claimCache.get(name);
		if (claim == null) {
			throw new IllegalArgumentException("Unknown claim " + name + ". Delete failed");
		}

		SQLQueries.deleteClaim(claim);
		claim.getChunks().forEach((chunk) -> {
			chunk.setClaimMeta(null);
		});
		this.claimCache.remove(name).getChunks().clear();
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

		if (chunk.getClaimMeta() == null) {
		// Unclaimed, claim it and return
			SQLQueries.claimChunk(claim, chunk);
			claim.getChunks().add(chunk);
			chunk.setClaimMeta(claim);

		} else if (chunk.getClaimMeta() != claim) {
		// Claimed by another claim zone
			throw new IllegalArgumentException("Chunk is already claimed by claim " + claim.getClaimName());
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
		BoroughChunk chunk = this.chunkCache.get(getChunkKey(world, x, z));
		
		// Check DB
		if (chunk == null) {
			chunk = SQLQueries.getBoroughChunk(world, x, z);
		}

		// Doesn't exist. Return wrapper with null claim meta.
		if (chunk == null) {
			chunk = new BoroughChunk(null, world, x, z);
			this.chunkCache.put(getChunkKey(world, x, z), chunk);
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
		if (chunk.getClaimMeta() == null) { return; }
		else {
			// claimed. Delete it.
			SQLQueries.unclaimChunk(chunk);
			chunk.getClaimMeta().getChunks().remove(chunk);
			chunk.setClaimMeta(null);
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
		List<String> claimNames = this.claimMembershipCache.get(user);
		List<BoroughChunkPermissionLevel> claimPerms = this.claimPermissionsCache.get(user);
		
		// Ask SQL if not in cache
		if (claimNames == null || claimPerms == null) {
			claimNames = new ArrayList<String>();
			claimPerms = new ArrayList<BoroughChunkPermissionLevel>();
			SQLQueries.getClaimsByUser(user, claimNames, claimPerms);
		}

		this.claimMembershipCache.put(user, Collections.synchronizedList(claimNames));
		this.claimPermissionsCache.put(user, Collections.synchronizedList(claimPerms));

		List<String> results = new ArrayList<String>();

		for (int i=0; i<claimNames.size(); i++) {
			Borough._this().logDebug("Considering claim " + claimNames.get(i) + " with permission " + claimPerms.get(i));
			if (claimPerms.get(i).compareTo(permissionFilter) >= 0) {
				Borough._this().logDebug("...cleared");
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
