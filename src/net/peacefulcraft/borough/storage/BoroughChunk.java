package net.peacefulcraft.borough.storage;

import java.util.UUID;

import net.peacefulcraft.borough.Borough;

public class BoroughChunk {

	// claim key is a string (immutable) so synchronized blocks are not needed.
	// We only need to sync (this) so the chunk doesn't get moved around during claim checks
	private String claimKey;
		/**
		 * @return Associated claim information or NULL of chunk is unclaimed
		 */
		public synchronized BoroughClaim getClaimMeta() {
			if (this.claimKey.length() == 0) { return null; }
			return Borough.getClaimStore().getClaim(this.claimKey);
		}
		public void clearClaimMeta() { this.claimKey = ""; }
		public void setClaimMeta(String claimKey) { this.claimKey = claimKey; }

	// Immutable. Don't need to sync
	private String world;
		public String getWorld() { return this.world; }
	private int x;
		public int getChunkX() { return this.x; }
	private int z;
		public int getChunkZ() { return this.z; }

	public BoroughChunk(String claimKey, String world, int x, int z) {
		this.claimKey = claimKey;
		this.world = world;
		this.x = x;
		this.z = z;
	}

	public synchronized boolean isChunkClaimed() {
		return this.claimKey.length() > 0;
	}

	public synchronized boolean canUserBuild(UUID user) {
		return (
			this.getClaimMeta() == null // User can build in the wilderness
			/*this.getClaimMeta().isPublic()*/
			|| this.getClaimMeta().getBuilders().contains(user)
			|| this.getClaimMeta().getModerators().contains(user)
			|| this.getClaimMeta().getOwners().contains(user)
		);
	}

	public synchronized boolean doesAllowBlockDamage() {
		return this.getClaimMeta().doesAllowBlockDamage();
	}

	public synchronized boolean doesAllowFluidMovement() {
		return this.getClaimMeta().doesAllowFluidMovement();
	}

	public synchronized boolean doesAllowPVP() {
		return this.getClaimMeta().doesAllowPVP();
	}

	public synchronized boolean doesAllowPistonMovement() {
		return this.getClaimMeta().doesAllowPistonMovement();
	}

	public synchronized boolean doesAllowTeleport() {
		return this.getClaimMeta().doesAllowTeleport();
	}
}
