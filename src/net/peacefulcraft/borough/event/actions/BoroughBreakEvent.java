package net.peacefulcraft.borough.event.actions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import net.peacefulcraft.borough.storage.BoroughChunk;

public class BoroughBreakEvent extends BoroughActionEvent {

	private static final HandlerList handlers = new HandlerList();
	private final Block block;

	/**
	 * Break event is thrown when a player attempts to break a block
	 * 
	 * @param p player involved in event
	 * @param loc Location of event
	 * @param mat Material of block in event
	 * @param block Block in event
	 * @param chunk BoroughChunk of block
	 * @param cancelled True if event has been determined to be cancelled
	 */
	public BoroughBreakEvent(Player p, Location loc, Material mat, Block block, BoroughChunk chunk, boolean cancelled) {
		super(p, loc, mat, chunk, cancelled);
		this.block = block;
	}
	
	/**
	 * @return block being broken in event
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
