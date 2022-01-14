package net.peacefulcraft.borough.event.actions;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import net.peacefulcraft.borough.storage.BoroughChunk;

/**
 * Used in player action events such as: build, destroy/break, itemuse
 */
public class BoroughActionEvent extends Event implements Cancellable {

	protected final Player player;
	protected final Location loc;
	protected final Material mat;
	protected final BoroughChunk chunk;
	protected boolean cancelled;
	protected String message;

	public BoroughActionEvent(Player p, Location loc, Material mat, BoroughChunk chunk, boolean cancelled) {
		this.player = p;
		this.loc = loc;
		this.mat = mat;
		this.chunk = chunk;
		setCancelled(cancelled);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}

	@Override
	public HandlerList getHandlers() {
		return null;
	}	

	/**
	 * @return player involved in action event
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * @return Location of action event
	 */
	public Location getLocation() {
		return loc;
	}

	/**
	 * @return Material of block involved in action event
	 */
	public Material getMaterial() {
		return mat;
	}

	/**
	 * @return The chunk involved in action. Null if occurred in unclaimed chunk
	 */
	@Nullable
	public BoroughChunk getBoroughChunk() {
		return chunk;
	}

	/**
	 * @return True if occurred in claimed chunk, false otherwise
	 */
	public boolean isClaimed() {
		return chunk == null;
	}

	/**
	 * @return Message of cancellation shown to players if event is cancelled
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message Message shown to players if event is cancelled
	 */
	public void setMessage(String message) {
		this.message = message;
	}
}
