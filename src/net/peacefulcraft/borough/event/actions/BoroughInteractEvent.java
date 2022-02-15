package net.peacefulcraft.borough.event.actions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import net.peacefulcraft.borough.storage.BoroughChunk;

public class BoroughInteractEvent extends BoroughActionEvent {

	private static final HandlerList handlers = new HandlerList();
	private final Block block;

	/**
	 * Interact Event is thrown when a player attempts to interact or use
	 * a block within a claim. I.e. levers, doors, buttons
	 * 
	 * @param p
	 * @param loc
	 * @param mat
	 * @param block
	 * @param chunk
	 * @param cancelled
	 */
	public BoroughInteractEvent(Player p, Location loc, Material mat, Block block, BoroughChunk chunk, boolean cancelled) {
		super(p, loc, mat, chunk, cancelled);
		this.block = block;
	}

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
