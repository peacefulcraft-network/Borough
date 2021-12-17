package net.peacefulcraft.borough.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import net.peacefulcraft.borough.Borough;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.peacefulcraft.borough.storage.BoroughChunk;
import net.peacefulcraft.borough.storage.BoroughChunkPermissionLevel;
import net.peacefulcraft.borough.utilities.EntityHandler;

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
	}
	
	public void onPlayerLeave(PlayerQuitEvent ev) {
		Borough._this().logDebug("Processing logout for user " + ev.getPlayer().getName() + ". Requesting cache eviction.");
		Borough.getClaimStore().evictCachedUserData(ev.getPlayer().getUniqueId());
	}

	@EventHandler
	public void BlockBreakEventListener(BlockBreakEvent ev) {
		Location loc = ev.getBlock().getLocation();
		Player p = ev.getPlayer();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (!p.hasPermission("pcn.staff") && chunk.isChunkClaimed() && !chunk.canUserBuild(p.getUniqueId())) {
			Borough._this().logDebug("[PlayerCache] Cancel BlockBreakEvent.");
			ev.setCancelled(true);
		}
	}

	@EventHandler
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

	@EventHandler
	public void PlayerInteractEventListener(PlayerInteractEvent ev) {
		if (ev.getClickedBlock() == null) { return; }

		Location loc = ev.getClickedBlock().getLocation();
		Player p = ev.getPlayer();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (!p.hasPermission("pcn.staff") && chunk.isChunkClaimed() && !chunk.canUserBuild(p.getUniqueId())) {
			Borough._this().logDebug("[PlayerCache] Cancel PlayerInteractEvent.");
			ev.setCancelled(true);
		}
	}

	@EventHandler
	public void EntityDamageByEntityEventListener(EntityDamageByEntityEvent ev) {
		Entity e = ev.getDamager();
		Entity vic = ev.getEntity();
		Location loc = e.getLocation();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if ((e instanceof Player) && (vic instanceof Player)) {
			// PVP event
			if (chunk.isChunkClaimed() && !chunk.doesAllowPVP()) {
				Borough._this().logDebug("[PlayerCache] Cancel PlayerDamagePlayerEvent.");
				ev.setCancelled(true);
			}
		} else if ((e instanceof Player) && !(vic instanceof Player)) {
			if (chunk.isChunkClaimed() && EntityHandler.isPassive(vic.getType())) {
				Borough._this().logDebug("[PlayerCache] Cancel EntityDamagePassiveEvent.");
				ev.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void BlockDamageEventListener(BlockIgniteEvent ev) {
		Location loc = ev.getBlock().getLocation();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (chunk.isChunkClaimed() && !chunk.doesAllowBlockDamage()) {
			Borough._this().logDebug("[PlayerCache] Cancel BlockIgniteEvent.");
			ev.setCancelled(true);
		}
	}

	@EventHandler
	public void ExplodeEventListener(EntityExplodeEvent ev) {
		Location loc = ev.getLocation();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (chunk.isChunkClaimed() && !chunk.doesAllowBlockDamage()) {
			Borough._this().logDebug("[PlayerCache] Cancel EntityExplodeEvent.");
			ev.setCancelled(true);
		}
	}

	@EventHandler
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
}
