package net.peacefulcraft.borough.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import net.peacefulcraft.borough.Borough;

import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import net.peacefulcraft.borough.storage.BoroughChunk;
import net.peacefulcraft.borough.storage.BoroughChunkPermissionLevel;
import net.peacefulcraft.borough.utilities.EntityHandler;
import net.peacefulcraft.borough.utilities.ItemClassifier;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PlayerCacheEventListeners implements Listener {
	
	/**
	 * Cache player UUIDs when they login.
	 * @param ev
	 */
	@EventHandler
	public void onPlayerJoin(AsyncPlayerPreLoginEvent ev) {
		// Store UUID:username
		Borough._this().logDebug("Processing login for user " + ev.getUniqueId() + ". Requesting UUID mapping.");
		Borough.getUUIDCache().cacheUUIDUsernameMapping(ev.getUniqueId(), ev.getName());

		// Fetch claims to populate cache
		Borough._this().logDebug("Processing login for user " + ev.getUniqueId() + ". Populating claim TC cache.");
		List<String> claims = Borough.getClaimStore().getClaimNamesByUser(ev.getUniqueId(), BoroughChunkPermissionLevel.BUILDER);
		claims.forEach((claim) -> { Borough._this().logDebug(claim); });

		// TODO: Handle this
		//Borough.getClaimStore().preProcessPlayerJoin(ev.getUniqueId(), playerRegistryId);
	}
	
	public void onPlayerLeave(PlayerQuitEvent ev) {
		Borough._this().logDebug("Processing logout for user " + ev.getPlayer().getName() + ". Requesting cache eviction.");
		Borough.getClaimStore().evictCachedUserData(ev.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void BlockBreakEventListener(BlockBreakEvent ev) {
		Location loc = ev.getBlock().getLocation();
		Player p = ev.getPlayer();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (!p.hasPermission("pcn.staff") && chunk.isChunkClaimed() && !chunk.canUserBuild(p.getUniqueId())) {
			Borough._this().logDebug("[PlayerCache] Cancel BlockBreakEvent.");
			ev.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void BlockPlaceEventListener(BlockPlaceEvent ev) {
		Location loc = ev.getBlock().getLocation();
		Player p = ev.getPlayer();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (ev.getBlock().getType().equals(Material.FIRE) && chunk.isChunkClaimed() && !chunk.doesAllowBlockDamage()) {
			Borough._this().logDebug("[PlayerCache] Cancel BlockPlaceEvent, Fire.");
			ev.setCancelled(true);
		}

		if (!p.hasPermission("pcn.staff") && chunk.isChunkClaimed() && !chunk.canUserBuild(p.getUniqueId())) {
			Borough._this().logDebug("[PlayerCache] Cancel BlockBreakEvent.");
			ev.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void PlayerInteractEventListener(PlayerInteractEvent ev) {
		if (ev.getClickedBlock() == null) { return; }

		Location loc = ev.getClickedBlock().getLocation();
		Player p = ev.getPlayer();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		// Checking if player is eating.
		// If right clicking and held item in hand is food.
		if (isEating(ev)) { return; }

		// Check if player is equiping armor/elytra
		if (isEquiping(ev)) { return; }

		if (!p.hasPermission("pcn.staff") && chunk.isChunkClaimed() && !chunk.canUserBuild(p.getUniqueId())) {
			Borough._this().logDebug("[PlayerCache] Cancel PlayerInteractEvent.");
			ev.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void PlayerInteractEntityEventListener(PlayerInteractEntityEvent ev) {
		Entity ent = ev.getRightClicked();
		Location loc = ent.getLocation();
		Player p = ev.getPlayer();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (!p.hasPermission("pcn.staff") && chunk.isChunkClaimed() && !chunk.canUserBuild(p.getUniqueId()) && EntityHandler.isPassive(ev.getRightClicked().getType())) {
			Borough._this().logDebug("[PlayerCache] Cancel PlayerInteractEntityEvent.");
			ev.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void PlayerInteractEntityListener(PlayerInteractAtEntityEvent ev) {
		Entity ent = ev.getRightClicked();
		Location loc = ent.getLocation();
		Player p = ev.getPlayer();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (!p.hasPermission("pcn.staff") && chunk.isChunkClaimed() && !chunk.canUserBuild(p.getUniqueId())) {
			Borough._this().logDebug("[PlayerCache] Cancel PlayerInteractAtEntityEvent.");
			ev.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void EntityDamageByEntityEventListener(EntityDamageByEntityEvent ev) {
		Entity e = ev.getDamager();
		Entity vic = ev.getEntity();
		Location loc = vic.getLocation();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);
		
		if ((e instanceof Player) && (vic instanceof Player)) {
			// PVP event
			if (chunk.isChunkClaimed() && !chunk.doesAllowPVP()) {
				Borough._this().logDebug("[PlayerCache] Cancel PlayerDamagePlayerEvent.");
				ev.setCancelled(true);
			}
		} else if ((e instanceof Player) && !(vic instanceof Player)) {
			if (!e.hasPermission("pcn.staff") && chunk.isChunkClaimed() && !chunk.canUserBuild(e.getUniqueId()) && EntityHandler.isPassive(vic.getType())) {
				Borough._this().logDebug("[PlayerCache] Cancel EntityDamagePassiveEvent.");
				ev.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void BlockDamageEventListener(BlockIgniteEvent ev) {
		Location loc = ev.getBlock().getLocation();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (chunk.isChunkClaimed() && !chunk.doesAllowBlockDamage()) {
			Borough._this().logDebug("[PlayerCache] Cancel BlockIgniteEvent.");
			ev.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void ExplodeEventListener(EntityExplodeEvent ev) {
		Location loc = ev.getLocation();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (chunk.isChunkClaimed() && !chunk.doesAllowBlockDamage()) {
			Borough._this().logDebug("[PlayerCache] Cancel EntityExplodeEvent.");
			ev.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void FluidMovementListener(BlockFromToEvent ev) {
		Location loc = ev.getToBlock().getLocation();
		
		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		Block b = ev.getBlock();
		if (!b.getType().equals(Material.LAVA) && !b.getType().equals(Material.WATER)) {
			return;
		}

		if (chunk.isChunkClaimed() && !chunk.doesAllowFluidMovement()) {
			Borough._this().logDebug("[PlayerCache] Cancel BlockFromToEvent, Water/Lava.");
			ev.setCancelled(true);
		}
	}

	/**
	 * Checks if player is eating food or not
	 * @param ev Interact Event we are processing
	 * @return True if eating. False otherwise
	 */
	private boolean isEating(PlayerInteractEvent ev) {
		// Right clicking food in hand
		if (ev.getAction().equals(Action.RIGHT_CLICK_AIR) || ev.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			ItemStack item = ev.getItem();
			if (item == null || item.getType().equals(Material.AIR)) { return false; }

			return item.getType().isEdible();
		}

		return false;
	}

	/**
	 * Checks whether player is attempting to equip armor/wearable
	 * @param ev Interact Event we are processing.
	 * @return True if equiping. False otherwise.
	 */
	private boolean isEquiping(PlayerInteractEvent ev) {
		if (ev.getAction().equals(Action.RIGHT_CLICK_AIR) || ev.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			ItemStack item = ev.getItem();
			if (item == null || item.getType().equals(Material.AIR)) { return false; }

			return ItemClassifier.isEquipable(item.getType());
		}

		return false;
	}
}
