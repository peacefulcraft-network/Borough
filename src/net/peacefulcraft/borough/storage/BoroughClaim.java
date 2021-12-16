package net.peacefulcraft.borough.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.peacefulcraft.borough.Borough;

public class BoroughClaim {
	
	private int claimId;
		/**
		 * @return Id of claim group or -1 if unclaimed
		 */
		public int getClaimId() { return this.claimId; }

	private String claimName;
		/**
		 * @return String: claim name or NULL: if unclaimed
		 */
		public String getClaimName() { return this.claimName; }

	private List<BoroughChunk> chunks;
		/**
		 * @return List of chunks in this claim
		 */
		public List<BoroughChunk> getChunks() { return this.chunks; }

	private List<UUID> owners;
		public List<UUID> getOwners() { return Collections.unmodifiableList(this.owners); }
	private List<UUID> moderators;
		public List<UUID> getModerators() { return Collections.unmodifiableList(this.moderators); }
	private List<UUID> builders;
		public List<UUID> getBuilders() { return Collections.unmodifiableList(this.builders); }

	private Boolean allowBlockDamage;
		public Boolean doesAllowBlockDamage() { return this.allowBlockDamage; }
		public void setBlockDamage(Boolean b) {
			this.allowBlockDamage = b;
			SQLQueries.setClaimFlag(this, BoroughClaimFlag.ALLOW_BLOCK_DAMAGE, b);
		}
	private Boolean allowFluidMovement;
		public Boolean doesAllowFluidMovement() { return this.allowFluidMovement; }
		public void setFluidMovement(Boolean b) {
			this.allowFluidMovement = b;
			SQLQueries.setClaimFlag(this, BoroughClaimFlag.ALLOW_FLUID_MOVEMENT, b);
		}
	private Boolean allowPVP;
		public Boolean doesAllowPVP() { return this.allowPVP; }
		public void setPVP(Boolean b) {
			this.allowPVP = b;
			SQLQueries.setClaimFlag(this, BoroughClaimFlag.ALLOW_PVP, b);
		}

	public BoroughClaim(int claimId, String claimName, List<UUID> owners, List<UUID> moderators, List<UUID> builders) {
		this.claimId = claimId;
		this.claimName = claimName;

		this.chunks = Collections.synchronizedList(new ArrayList<BoroughChunk>());
		this.owners = Collections.synchronizedList(owners);
		this.moderators = Collections.synchronizedList(moderators);
		this.builders = Collections.synchronizedList(builders);

		this.allowBlockDamage = true;
		this.allowFluidMovement = true;
		this.allowPVP = true;
	}

	public BoroughClaim(int claimId, String claimName, List<UUID> owners, List<UUID> moderators, List<UUID> builders, Boolean allowBlockDamage, Boolean allowFluidMovement, Boolean allowPvP) {
		this.claimId = claimId;
		this.claimName = claimName;

		this.chunks = Collections.synchronizedList(new ArrayList<BoroughChunk>());
		this.owners = Collections.synchronizedList(owners);
		this.moderators = Collections.synchronizedList(moderators);
		this.builders = Collections.synchronizedList(builders);

		this.allowBlockDamage = allowBlockDamage;
		this.allowFluidMovement = allowFluidMovement;
		this.allowPVP = allowPvP;
	}

	/**
	 * Add the provider user as a chunk owner.
	 * 
	 * @param owner UUID of user to add
	 * 
	 * @throws IllegalArgumentException If user is already a chunk owner.
	 */
	public void addOwner(UUID owner) {
		if (this.owners.contains(owner)) {
			throw new IllegalArgumentException("User is already a chunk owner.");
		}

		this.owners.add(owner);
		SQLQueries.setPermissionsOnClaim(this, owner, BoroughChunkPermissionLevel.OWNER);

		// Trigger cache update
		Borough.getClaimStore().getClaimNamesByUser(owner, BoroughChunkPermissionLevel.BUILDER);
	}

	/**
	 * Remove the provider user as a chunk owner.
	 * 
	 * @param owner UUID of user to add
	 * 
	 * @throws IllegalArgumentException If user is already a chunk owner.
	 */
	public void removeOwner(UUID owner) {
		if (this.owners.contains(owner)) {
			throw new IllegalArgumentException("User is not a chunk owner.");
		}
		SQLQueries.unsetPermissionsOnClaim(this, owner);

		// Trigger cache update
		Borough.getClaimStore().getClaimNamesByUser(owner, BoroughChunkPermissionLevel.BUILDER);
	}

	/**
	 * Add the provider user as a chunk moderator.
	 * 
	 * @param owner UUID of user to add
	 * 
	 * @throws IllegalArgumentException If user is already a chunk moderator.
	 */
	public void addModerator(UUID moderator) {
		if (this.moderators.contains(moderator)) {
			throw new IllegalArgumentException("User is already a chunk moderator.");
		}
		SQLQueries.setPermissionsOnClaim(this, moderator, BoroughChunkPermissionLevel.MODERATOR);

		// Trigger cache update
		Borough.getClaimStore().getClaimNamesByUser(moderator, BoroughChunkPermissionLevel.BUILDER);
	}

	/**
	 * Remove the provider user as a chunk moderator.
	 * 
	 * @param owner UUID of user to add
	 * 
	 * @throws IllegalArgumentException If user is already a chunk moderator.
	 */
	public void removeModerator(UUID moderator) {
		if (this.moderators.contains(moderator)) {
			throw new IllegalArgumentException("User is not a chunk moderator.");
		}
		SQLQueries.unsetPermissionsOnClaim(this, moderator);

		// Trigger cache update
		Borough.getClaimStore().getClaimNamesByUser(moderator, BoroughChunkPermissionLevel.BUILDER);
	}

	/**
	 * Add the provider user as a chunk builder.
	 * 
	 * @param builder UUID of user to add
	 * 
	 * @throws IllegalArgumentException If user is already a chunk builder.
	 */
	public void addBuilder(UUID builder) {
		if (this.builders.contains(builder)) {
			throw new IllegalArgumentException("User is already a chunk builder.");
		}
		SQLQueries.setPermissionsOnClaim(this, builder, BoroughChunkPermissionLevel.BUILDER);

		// Trigger cache update
		Borough.getClaimStore().getClaimNamesByUser(builder, BoroughChunkPermissionLevel.BUILDER);
	}

	/**
	 * Remove the provider user as a chunk builder.
	 * 
	 * @param builder UUID of user to add
	 * 
	 * @throws IllegalArgumentException If user is already a chunk builder.
	 */
	public void removeBuilder(UUID builder) {
		if (this.builders.contains(builder)) {
			throw new IllegalArgumentException("User is not a chunk builder.");
		}
		SQLQueries.unsetPermissionsOnClaim(this, builder);

		// Trigger cache update
		Borough.getClaimStore().getClaimNamesByUser(builder, BoroughChunkPermissionLevel.BUILDER);
	}
}
