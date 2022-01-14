package net.peacefulcraft.borough.listeners;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import net.peacefulcraft.borough.Borough;
import net.peacefulcraft.borough.event.executor.BoroughActionExecutor;
import net.peacefulcraft.borough.storage.BoroughChunk;

public class PlayerCacheBlockListeners implements Listener {
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent ev) {
		if (ev.getPlayer().hasPermission("pcn.staff")) { return; }

		Block block = ev.getBlock();
		ev.setCancelled(!BoroughActionExecutor.canBreak(ev.getPlayer(), block.getLocation(), block.getType()));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent ev) {
		if (ev.getPlayer().hasPermission("pcn.staff")) { return; }

		Block block = ev.getBlock();

		// Allowing portals to be placed
		if (block.getType() == Material.FIRE && block.getRelative(BlockFace.DOWN).getType() == Material.OBSIDIAN) { return; }
		
		if (!BoroughActionExecutor.canBuild(ev.getPlayer(), block.getLocation(), block.getType())) {
			ev.setBuild(false);
			ev.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockCanBuild(BlockCanBuildEvent ev) {
		// TODO: Adjust this event
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent ev) {
		ev.setCancelled(!BoroughActionExecutor.CanBurn(ev.getBlock()));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockIgnite(BlockIgniteEvent ev) {
		ev.setCancelled(!BoroughActionExecutor.CanBurn(ev.getBlock()));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockPistonRetract(BlockPistonRetractEvent ev) {
		if (!canBlockMove(ev.getBlock(), ev.isSticky() ? ev.getBlock().getRelative(ev.getDirection().getOppositeFace()) : ev.getBlock().getRelative(ev.getDirection()), false)) {
			ev.setCancelled(true);
		}

		List<Block> blocks = ev.getBlocks();
		if (!blocks.isEmpty()) {
			blocks.forEach((block) -> {
				if (!canBlockMove(block, block.getRelative(ev.getDirection()), false)) {
					ev.setCancelled(true);
				}
			});
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockPistonExtend(BlockPistonExtendEvent ev) {
		if (!canBlockMove(ev.getBlock(), ev.getBlock().getRelative(ev.getDirection()), true)) {
			ev.setCancelled(true);
		}

		List<Block> blocks = ev.getBlocks();
		if (!blocks.isEmpty()) {
			blocks.forEach((block) -> {
				if (!canBlockMove(block, block.getRelative(ev.getDirection()), true)) {
					ev.setCancelled(true);
				}
			});
		}
	}

	/**
	 * Determines if block can move between locations with chunk permissions
	 * 
	 * @param block Block that is moving
	 * @param blockTo To location of block
	 * @param allowWild If block is allowed to move into the wilderness
	 * @return True if allowed, false otherwise
	 */
	private boolean canBlockMove(Block block, Block blockTo, boolean allowWild) {
		BoroughBlock from = new BoroughBlock(block);
		BoroughBlock to = new BoroughBlock(blockTo);

		if (from.equals(to) || (allowWild && !to.isClaimed()) || (!to.isClaimed() && !from.isClaimed())) { return true; }

		BoroughChunk fromChunk = Borough.getClaimStore().getChunk(block.getLocation());
		BoroughChunk toChunk = Borough.getClaimStore().getChunk(blockTo.getLocation());

		if (fromChunk.getClaimMeta().getClaimId() == toChunk.getClaimMeta().getClaimId()) { return true; }
		return false;
	}

	/**
	 * Simple wrapper class to associate a block with 
	 * a chunks permissions
	 */
	private class BoroughBlock {
		private final Block block;
		private final BoroughChunk chunk;

		public BoroughBlock(Block block) {
			this.block = block;
			this.chunk = Borough.getClaimStore().getChunk(block.getLocation());
		}

		/**
		 * @return Is Block claimed
		 */
		public boolean isClaimed() { 
			return chunk.isChunkClaimed(); 
		}

		public Block getBlock() {
			return this.block;
		}

		@Override
		public boolean equals(Object o) {
			// lol
			return (o instanceof BoroughBlock) ? this.block.equals(((BoroughBlock)o).getBlock()) : false;
		}
	}

}
