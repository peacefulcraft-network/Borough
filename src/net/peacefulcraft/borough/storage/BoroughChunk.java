package net.peacefulcraft.borough.storage;

import java.util.UUID;

import net.peacefulcraft.borough.Borough;

public class BoroughChunk {

	private String claimKey;
		/**
		 * @return Associated claim information or NULL of chunk is unclaimed
		 */
		public synchronized BoroughClaim getClaimMeta() {
			synchronized(this.claimKey) {
				if (this.claimKey.length() == 0) { return null; }
				return Borough.getClaimStore().getClaim(this.claimKey);
			}
		}
		public synchronized void clearClaimMeta() { this.claimKey = ""; }
		public synchronized void setClaimMeta(String claimKey) { this.claimKey = claimKey; }

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
		synchronized(this.claimKey) {
			return (
				this.getClaimMeta().getBuilders().contains(user)
				|| this.getClaimMeta().getModerators().contains(user)
				|| this.getClaimMeta().getOwners().contains(user)
			);
		}
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
}
