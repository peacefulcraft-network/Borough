package net.peacefulcraft.borough.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoroughClaimStore {
	
	private Map<String, BoroughClaim> claimCache;
	private Map<String, BoroughChunk> chunkCache;

	public BoroughClaimStore() {
		this.claimCache = Collections.synchronizedMap(new HashMap<String, BoroughClaim>());
		this.chunkCache = Collections.synchronizedMap(new HashMap<String, BoroughChunk>());
	}

	protected String computeChunkHash(BoroughChunk chunk) {
		return this.computeChunkHash(chunk.getWorld(), chunk.getChunkX(), chunk.getChunkZ());
	}

	protected String computeChunkHash(String world, int x, int z) {
		return world + "_" + x + "_" + z;
	}

	/**
	 * Creates a new named claim object.
	 * 
	 * @param name Claim name
	 * @param owner UUID of claim owner
	 * @return New BoroughClaim object
	 * 
	 * @throws IllegalArgumentException if `owner` already has a claim called `name`
	 */
	public BoroughClaim createClaim(String name, UUID owner) {
		return null;
	}

	/**
	 * Get the BuroughClaim meta object by the given claim name
	 * @param name Claim name
	 * @return Claim meta object or NULL of no such claim exists
	 */
	public BoroughClaim getClaim(String name) {
		return null;
	}

	/**
	 * Delete the provided claim
	 * 
	 * @param name Name of claim to delete
	 * 
	 * @throws IllegalArgumentException It no claim called `name` exists.
	 */
	public void deleteClaim(String name) {

	}

	/**
	 * Claim the requested chunk if it is not already claimed.
	 * 
	 * @param world World chunk is in
	 * @param x Chunk x coordinate. (Not world coordinates)
	 * @param z Chunk z coordinate. (Not world coordinates)
	 * @param String Name of the claim to add this chunk too
	 * @return BoroughClaim object for the newly claimed chunk.
	 */
	public BoroughClaim claimChunk(String world, int x, int z, BoroughClaim claim) {
		return null;
	}

	/**
	 * Get claim information about the requested chunk
	 * 
	 * @param world World chunk is in
	 * @param x Chunk x coordinate. (Not world coordinates)
	 * @param z Chunk z coordinate. (Not world coordinates)
	 * @return BoroughChunk object or NULL if chunk is not claimed.
	 */
	public BoroughChunk getChunk(String world, int x, int z) {
		return null;
	}

	/**
	 * Unclaim the requested chunk if it is claimed. No effect if chunk was not claimed.
	 * 
	 * @param world World chunk is in
	 * @param x Chunk x coordinate. (Not world coordinates)
	 * @param z Chunk z coordinate. (Not world coordinates)
	 */
	public void unclaimChunk(String world, int x, int z) {

	}
}
