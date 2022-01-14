package net.peacefulcraft.borough.event.actions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import net.peacefulcraft.borough.storage.BoroughChunk;

public class BoroughBuildEvent extends BoroughActionEvent {
	
	private static final HandlerList handlers = new HandlerList();
	private final Block block;

	/**
	 * Build event thrown in the event of player placing blocks into the world.
	 * 
	 * @param player involved in the build event
	 * @param loc location of block being placed
	 * @param mat material of block being placed
	 * @param block block being placed
	 * @param chunk BoroughChunk this is occurring in
	 * @param cancelled true if Borough has determined this is cancelled
	 */
	public BoroughBuildEvent(Player player, Location loc, Material mat, Block block, BoroughChunk chunk, boolean cancelled) {
		super(player, loc, mat, chunk, cancelled);
		this.block = block;
	}

	/**
	 * @return Block being built upon
	 */
	public Block getBlock() {
		return block;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
