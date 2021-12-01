package net.peacefulcraft.borough.storage;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

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

	private List<UUID> owners;
		public List<UUID> getOwners() { return Collections.unmodifiableList(this.owners); }
	private List<UUID> moderators;
		public List<UUID> getModerators() { return Collections.unmodifiableList(this.moderators); }
	private List<UUID> builders;
		public List<UUID> getBuilders() { return Collections.unmodifiableList(this.builders); }

	public BoroughClaim(int claimId, String claimName) {
		this.claimId = claimId;
		this.claimName = claimName;

		this.owners = Collections.synchronizedList(owners);
		this.moderators = Collections.synchronizedList(moderators);
		this.builders = Collections.synchronizedList(builders);
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
	}
}
