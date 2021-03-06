package net.peacefulcraft.borough.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;

import net.peacefulcraft.borough.Borough;
import net.peacefulcraft.borough.event.executor.BoroughActionExecutor;
import net.peacefulcraft.borough.storage.BoroughChunk;
import net.peacefulcraft.borough.utilities.ItemLists;

/**
 * Reworked player cache event listeners for block events 1/14/22
 * 
 * Modeled after Towny block listeners for security of chunk permissions
 * and to fit the existing Borough model
 */
public class PlayerBlockListener implements Listener {
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent ev) {
		Block block = ev.getBlock();
		ev.setCancelled(!BoroughActionExecutor.canBreak(ev.getPlayer(), block.getLocation(), block.getType()));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent ev) {
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
		if (!canBlockMove(ev.getBlock(), ev.isSticky() ? ev.getBlock().getRelative(ev.getDirection().getOppositeFace()) : ev.getBlock().getRelative(ev.getDirection()), false, MovementReason.PISTON)) {
			ev.setCancelled(true);
		}

		List<Block> blocks = ev.getBlocks();
		if (!blocks.isEmpty()) {
			blocks.forEach((block) -> {
				if (!canBlockMove(block, block.getRelative(ev.getDirection()), false, MovementReason.PISTON)) {
					ev.setCancelled(true);
				}
			});
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockPistonExtend(BlockPistonExtendEvent ev) {
		if (!canBlockMove(ev.getBlock(), ev.getBlock().getRelative(ev.getDirection()), true, MovementReason.PISTON)) {
			ev.setCancelled(true);
		}

		List<Block> blocks = ev.getBlocks();
		if (!blocks.isEmpty()) {
			blocks.forEach((block) -> {
				if (!canBlockMove(block, block.getRelative(ev.getDirection()), true, MovementReason.PISTON)) {
					ev.setCancelled(true);
				}
			});
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onCreateExplosion(BlockExplodeEvent ev) {
		Material mat = ev.getBlock().getType();

		List<Block> blocks = BoroughActionExecutor.filterExplodeableBlocks(ev.blockList(), mat, null, ev);
		ev.blockList().clear();
		ev.blockList().addAll(blocks);
		if (ev.blockList().isEmpty()) { return; }

		// TODO: Further processing for location specifics and config
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onFrostWalker(EntityBlockFormEvent ev) {
		if (ev.getEntity() instanceof Player) {
			ev.setCancelled(!BoroughActionExecutor.canBuild((Player)ev.getEntity(), ev.getBlock().getLocation(), ev.getBlock().getType()));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent ev) {
		if (ev.getBlock().getType() == Material.DRAGON_EGG) { return; }

		if (ev.getBlock().isLiquid() && !canBlockMove(ev.getBlock(), ev.getToBlock(), true, MovementReason.FLUID)) {
			ev.setCancelled(true);
		} 

		if (!ev.getBlock().isLiquid() && !canBlockMove(ev.getBlock(), ev.getToBlock(), true, MovementReason.OTHER)) {
			ev.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockDispense(BlockDispenseEvent ev) {
		if (ev.getBlock().getType() != Material.DISPENSER) { return; }

		Material mat = ev.getItem().getType();

		if (ItemLists.BUCKETS.contains(mat.name())) { return; }

		if (!ItemLists.BUCKETS.contains(mat.name()) && mat != Material.BONE_MEAL && mat != Material.HONEYCOMB) { return; }

		if (!canBlockMove(ev.getBlock(), ev.getBlock().getRelative(((Directional) ev.getBlock().getBlockData()).getFacing()), true, MovementReason.OTHER)) {
			ev.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onFertilize(BlockFertilizeEvent ev) {
		List<BlockState> allowed = new ArrayList<>();
		Player p = ev.getPlayer();

		ev.getBlocks().forEach((state) -> {
			Block block = state.getBlock();
			BoroughChunk chunk = Borough.getClaimStore().getChunk(block.getLocation());

			// We need to allow players to fertilize in the wild and protect their claims
			if (!chunk.isChunkClaimed() || BoroughActionExecutor.canBuild(p, block.getLocation(), block.getType())) {
				allowed.add(state);
			}
		});

		ev.getBlocks().clear();
		ev.getBlocks().addAll(allowed);
	}

	/**
	 * Determines if block can move between locations with chunk permissions
	 * 
	 * @param block Block that is moving
	 * @param blockTo To location of block
	 * @param allowWild If block is allowed to move into the wilderness
	 * @return True if allowed, false otherwise
	 */
	private boolean canBlockMove(Block block, Block blockTo, boolean allowWild, MovementReason reason) {
		BoroughBlock from = new BoroughBlock(block);
		BoroughBlock to = new BoroughBlock(blockTo);

		// If block is the same, wild is allowed and to chunk is not claimed, neither are claimed
		if (from.equals(to) || (allowWild && !to.isClaimed()) || (!to.isClaimed() && !from.isClaimed())) { return true; }

		BoroughChunk fromChunk = Borough.getClaimStore().getChunk(block.getLocation());
		BoroughChunk toChunk = Borough.getClaimStore().getChunk(blockTo.getLocation());

		/**
		 * If both chunks are claimed we compare against their rulesets
		 * BOTH claims must have permissions for this event
		 */
		if (fromChunk.isChunkClaimed() && toChunk.isChunkClaimed() ) {
			if (reason == MovementReason.FLUID && fromChunk.doesAllowFluidMovement() && toChunk.doesAllowFluidMovement()) { return true; }
			if (reason == MovementReason.PISTON && fromChunk.doesAllowPistonMovement() && toChunk.doesAllowPistonMovement()) { return true; }
			// For other we want to ensure the OTHER movement occurs only if the chunks share a claim
			// This is to prevent OTHER movement from one persons chunk to anothers
			// Since we know both are claimed we can check their IDs directly
			if (reason == MovementReason.OTHER && fromChunk.getClaimMeta().getClaimId() == toChunk.getClaimMeta().getClaimId()) { return true; }
		}

		/**
		 * From chunk is wild. To Chunk is claimed
		 * We compare against to chunk permissions
		 * Since we are not moving INTO the wild we do not need allow wild
		 */
		if (!fromChunk.isChunkClaimed() && toChunk.isChunkClaimed()) {
			if (reason == MovementReason.FLUID && toChunk.doesAllowFluidMovement()) { return true; }
			if (reason == MovementReason.PISTON && toChunk.doesAllowPistonMovement()) { return true;}
			// In general we do not want OTHER block movements to go from unclaimed to claimed
			if (reason == MovementReason.OTHER) { return false; }
		}

		/**
		 * If from chunk is claimed we check against from chunk permissions
		 * allowWild must be true and fromChunk must have permission for this event
		 */
		if (fromChunk.isChunkClaimed() && !toChunk.isChunkClaimed()) {
			if (reason == MovementReason.FLUID && fromChunk.doesAllowFluidMovement()) { return true; }
			if (reason == MovementReason.PISTON && fromChunk.doesAllowPistonMovement()) { return true; }
			// We allow OTHER block movements from claimed to unclaimed
			if (reason == MovementReason.OTHER) { return true; }
		}		
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

	/**
	 * Enum to provide clarity on fromTo events
	 */
	private enum MovementReason {
		PISTON, FLUID, OTHER;
	}

}
