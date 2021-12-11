package net.peacefulcraft.borough.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import net.peacefulcraft.borough.Borough;

public class BoroughClaimStore {
	
	/*
		Populated by SQLQueries because BoroughChunk objects and BoroughClaim objects
		are tightly coupled so it is easier to construct + cache them together and then
		return whatever the query is supposed to provide.
	*/
	protected Map<String, BoroughClaim> claimCache;
	protected Map<String, BoroughChunk> chunkCache;

	public BoroughClaimStore() {
		this.claimCache = Collections.synchronizedMap(new HashMap<String, BoroughClaim>());
		this.chunkCache = Collections.synchronizedMap(new HashMap<String, BoroughChunk>());
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

	/**
	 * Creates a new named claim object. Performs blocking SQL work.
	 * Inserts BoroughChunk object into cache
	 * 
	 * @param bc BoroughChunk object
	 */
	public void insertChunkCache(BoroughChunk bc) {

		if (bc == null) { return; }

		this.chunkCache.put(
			computeChunkHash(bc), 
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
				throw new IllegalArgumentException("No known user " + nameParts[0]);
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
			return SQLQueries.claimChunk(claim, chunk);

		} else if (chunk.getClaimMeta() != claim) {
		// Claimed by another claim zone
			throw new IllegalArgumentException("Chunk is already claimed by claim " + claim.getClaimName());
		} else {
		// Already claimed, but by same claim zone as requested. Just return
			return chunk;
		}
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
		}
	}

	/**
	 * Get a list of all claims which this user has access to. Performs blocking SQL work.
	 * 
	 * @param user
	 * @return List of claim names which the user has access to at the requested permission level
	 */
	public List<String> getClaimsByUser(UUID user, BoroughChunkPermissionLevel permissionFilter) {
		return new ArrayList<String>();
	}
}
