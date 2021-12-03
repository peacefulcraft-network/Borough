package net.peacefulcraft.borough.listeners;

import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import net.peacefulcraft.borough.Borough;
import net.peacefulcraft.borough.storage.BoroughChunk;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class PlayerCacheEventListeners implements Listener {
	
	@EventHandler
	public void BlockBreakEventListener(BlockBreakEvent ev) {
		Location loc = ev.getBlock().getLocation();
		Player p = ev.getPlayer();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (!chunk.isChunkClaimed() || !chunk.canUserBuild(p.getUniqueId())) {
			ev.setCancelled(true);
		}
	}

	@EventHandler
	public void BlockPlaceEventListener(BlockPlaceEvent ev) {
		Location loc = ev.getBlock().getLocation();
		Player p = ev.getPlayer();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (ev.getBlock().getType().equals(Material.FIRE) && !chunk.doesAllowBlockDamage()) {
			ev.setCancelled(true);
		}

		if (!chunk.isChunkClaimed() || !chunk.canUserBuild(p.getUniqueId())) {
			ev.setCancelled(true);
		}
	}

	@EventHandler
	public void PlayerInteractEventListener(PlayerInteractEvent ev) {
		Location loc = ev.getClickedBlock().getLocation();
		Player p = ev.getPlayer();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (!chunk.isChunkClaimed() || !chunk.canUserBuild(p.getUniqueId())) {
			ev.setCancelled(true);
		}
	}

	@EventHandler
	public void EntityDamageByEntityEventListener(EntityDamageByEntityEvent ev) {
		Entity e = ev.getEntity();
		Location loc = e.getLocation();

		Entity d = ev.getDamager();
		if (!(d instanceof Player)) { return; }
		Player p = (Player)d;

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (!chunk.isChunkClaimed() || !chunk.canUserBuild(p.getUniqueId())) {
			ev.setCancelled(true);
		}
	}

	@EventHandler
	public void BlockDamageEventListener(BlockIgniteEvent ev) {
		Location loc = ev.getBlock().getLocation();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (!chunk.isChunkClaimed() || !chunk.doesAllowBlockDamage()) {
			ev.setCancelled(true);
		}
	}

	@EventHandler
	public void ExplodeEventListener(EntityExplodeEvent ev) {
		Location loc = ev.getLocation();

		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (!chunk.isChunkClaimed() || !chunk.doesAllowBlockDamage()) {
			ev.setCancelled(true);
		}
	}

	@EventHandler
	public void FluidMovementListener(BlockFromToEvent ev) {
		Location loc = ev.getToBlock().getLocation();
		
		BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);

		if (!chunk.isChunkClaimed() || !chunk.doesAllowFluidMovement()) {
			ev.setCancelled(true);
		}
	}

}
