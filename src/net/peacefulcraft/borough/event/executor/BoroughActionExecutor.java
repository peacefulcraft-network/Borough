package net.peacefulcraft.borough.event.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import net.peacefulcraft.borough.Borough;
import net.peacefulcraft.borough.event.actions.ActionType;
import net.peacefulcraft.borough.event.actions.BoroughActionEvent;
import net.peacefulcraft.borough.event.actions.BoroughBreakEvent;
import net.peacefulcraft.borough.event.actions.BoroughBuildEvent;
import net.peacefulcraft.borough.event.actions.BoroughInteractEvent;
import net.peacefulcraft.borough.event.actions.BoroughItemUseEvent;
import net.peacefulcraft.borough.storage.BoroughChunk;
import net.peacefulcraft.borough.utilities.BoroughMessanger;
import net.peacefulcraft.borough.utilities.ItemLists;

/**
 * Borough's Action Executor that checks player
 * permissions in the event against chunk
 */
public class BoroughActionExecutor {
	
	/**
	 * Checks if player is allowed to execute an action within a chunk
	 * 
	 * @param player Player involved in event
	 * @param loc Location of event
	 * @param mat Material involved in event
	 * @param action Action player is taking
	 * @param event ActionEvent
	 * @return True if action is allowed, false otherwise. 
	 */
	private static boolean isAllowedAction(Player player, Location loc, Material mat, ActionType action, BoroughActionEvent event) {
		
		/**
		 * Checking the chunk permissions of player
		 * 
		 * All permissions are currently based on their Build Permissions.
		 * // TODO: Look into a way to break this up further by actionType
		 */
		if (event.isClaimed() && !event.getBoroughChunk().canUserBuild(player.getUniqueId())) {
			event.setCancelled(true);
			//TODO: Set more specific error message here
			event.setMessage("You do not have permission to do that in this claim!");
		}

		if (event.isCancelled() && event.getMessage() != null) {
			BoroughMessanger.sendErrorMessage(player, event.getMessage());
		}

		Borough._this().logDebug("[ActionExecutor] Action Event " + player.getName() + ":" + (!event.isCancelled()));

		// If event is cancelled then action was not allowed
		return !event.isCancelled();
	}

	/**
	 * Checks permissions for explosions in a borough chunk
	 * 
	 * @param loc Location of explosion damage
	 * @return True if explosion is allowed at location
	 */
	private static boolean isAllowedExplosion(Location loc) {
		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);
		if (!chunk.isChunkClaimed()) { return true; }

		return chunk.doesAllowBlockDamage();
	}

	/**
	 * Filters blocks of explosion via permissions
	 * 
	 * @param blocks List of blocks effected by explosion
	 * @return Approved blocks for explosion damage
	 */
	private static List<Block> filterExplosion(List<Block> blocks) {
		List<Block> approved = new ArrayList<Block>();
		blocks.forEach((block) -> {
			if (isAllowedExplosion(block.getLocation())) { approved.add(block); } 
		});
		return approved;
	}

	/**
	 * Filters a burn event via chunk permissions
	 * @param block Block in burn event
	 * @return True if burn is allowed, false otherwise
	 */
	private static boolean isAllowedBurn(Block block) {
		BoroughChunk chunk = Borough.getClaimStore().getChunk(block.getLocation());
		if (!chunk.isChunkClaimed()) { return true; }

		// If chunk allows block damage we pass first
		if (chunk.doesAllowBlockDamage()) { return true; }

		// Specific permissions allowances within a chunk that does not allow block damage
		// Burn effect is portal
		if (block.getRelative(BlockFace.DOWN).getType() == Material.OBSIDIAN) { return true; }
		// Burn effect is on a compfire
		if (ItemLists.CAMPFIRES.contains(block.getType().name())) { return true; }
		// Burn effect is on a candle
		if (ItemLists.CANDLES.contains(block.getType().name())) { return true; }
		
		return true;
	}

	/**
	 * Can a player build with a material at this location
	 *  
	 * @param player Player involved in event
	 * @param loc Location of attempted event
	 * @param mat Material involved in event
	 * @return True if allowed, false otherwise
	 */
	public static boolean canBuild(Player player, Location loc, Material mat) {
		BoroughBuildEvent event = new BoroughBuildEvent(player, loc, mat, loc.getBlock(), Borough.getClaimStore().getChunk(loc), false);
		return isAllowedAction(player, loc, mat, ActionType.BUILD, event);
	}

	/**
	 * Can a player break this material at this location
	 * 
	 * @param player Player involved in event
	 * @param loc Location of attempted event
	 * @param mat Material involved in event
	 * @return True if allowed, false otherwise
	 */
	public static boolean canBreak(Player player, Location loc, Material mat) {
		BoroughBreakEvent event = new BoroughBreakEvent(player, loc, mat, loc.getBlock(), Borough.getClaimStore().getChunk(loc), false);
		return isAllowedAction(player, loc, mat, ActionType.BREAK, event);
	}

	/**
	 * Can a player interact with this material at this location
	 * 
	 * @param player Player involved in event
	 * @param loc Location of attempted event
	 * @param mat Material involved in event
	 * @return True if allowed, false otherwise
	 */
	public static boolean canInteract(Player player, Location loc, Material mat) {
		BoroughInteractEvent event = new BoroughInteractEvent(player, loc, mat, loc.getBlock(), Borough.getClaimStore().getChunk(loc), false);
		return isAllowedAction(player, loc, mat, ActionType.INTERACT, event);
	}

	/**
	 * Can a player use items of this material at this location
	 * 
	 * @param player Player involved in this event
	 * @param loc Location of attempted event
	 * @param mat Material involved in this event
	 * @return True if allowed, false otherwise
	 */
	public static boolean canItemUse(Player player, Location loc, Material mat) {
		BoroughItemUseEvent event = new BoroughItemUseEvent(player, loc, mat, Borough.getClaimStore().getChunk(loc), false);
		return isAllowedAction(player, loc, mat, ActionType.ITEMUSE, event);
	}

	/**
	 * Can a player teleport at this location.
	 * Player needs build permissions and claim must allow teleporting
	 * 
	 * @param player Player involved in this event
	 * @param loc Location of this event
	 * @return True if allowed, false otherwise
	 */
	public static boolean canTeleport(Player player, Location loc) {
		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);
		if (chunk == null) { return true; }

		boolean allowed = (chunk.isChunkClaimed() && chunk.canUserBuild(player.getUniqueId()) && chunk.doesAllowTeleport());
		if (!allowed) { BoroughMessanger.sendErrorMessage(player, "You are not allowed to teleport in this claim!"); }
		return allowed;
	}

	/**
	 * Filters out blocks which should not be exploded from an event list
	 * 
	 * @param blocks List of blocks involved in explode event
	 * @param mat Material which caused a block explosion
	 * @param entity Entity which caused an entity explosion
	 * @param bukkitExplosionEvent The bukkit explosion which caused this explosion
	 * @return Filtered blocks which are allowed to explode
	 */
	public static List<Block> filterExplodeableBlocks(List<Block> blocks, Material mat, Entity entity, Event bukkitExplosionEvent) {
		// Sorting by Y level
		Collections.sort(blocks, new Comparator<Block>(){
			@Override
			public int compare(Block o1, Block o2) {
				return o1.getY() - o2.getY();
			}
		});

		// Filter out blocks that are not allowed to explode
		List<Block> filteredBlocks = filterExplosion(blocks);
		return filteredBlocks;
	}

	/**
	 * Determines if explosions can hurt entities at given location
	 * 
	 * @param loc Location to check permissions
	 * @param victim Entity harmed by explosion
	 * @param cause Cause of damage
	 * @return True if allowed, false otherwise
	 */
	public static boolean canExplosionDamageEntities(Location loc, Entity victim, DamageCause cause) {

		// Initial response of permissions
		boolean cancelled = !isAllowedExplosion(loc);

		//TODO: Additional checking against event using cancelled?

		return !cancelled;
	}

	/**
	 * Determines if fire can burn this block
	 * 
	 * @param block Block involved in event
	 * @return True if allowed, false otherwise
	 */
	public static boolean CanBurn(Block block) {
		return isAllowedBurn(block);
	}
}
