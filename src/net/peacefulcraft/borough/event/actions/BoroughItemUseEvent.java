package net.peacefulcraft.borough.event.actions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import net.peacefulcraft.borough.storage.BoroughChunk;

public class BoroughItemUseEvent extends BoroughActionEvent {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * ItemUse event is thrown when a player attempts to use an item
	 * within the world. This item is filtered via config of 
	 * allowed items (item_use_filter).
	 * 
	 * @param p Player involved in event
	 * @param loc Location of event
	 * @param mat Material of item in event
	 * @param chunk BoroughChunk of this event
	 * @param cancelled True if event has been cancelled
	 */
	public BoroughItemUseEvent(Player p, Location loc, Material mat, BoroughChunk chunk, boolean cancelled) {
		super(p, loc, mat, chunk, cancelled);
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
}
