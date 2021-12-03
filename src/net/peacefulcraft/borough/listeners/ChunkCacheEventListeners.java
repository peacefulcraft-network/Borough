package net.peacefulcraft.borough.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import net.peacefulcraft.borough.Borough;

public class ChunkCacheEventListeners implements Listener {
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
	public void onChukLoad(ChunkLoadEvent ev) {
		// Go async while we do SQL work
		Borough._this().getServer().getScheduler().runTaskAsynchronously(Borough._this(), () -> {
			// Fetch chunk so that it is in cache incase we need it
			Borough.getClaimStore().getChunk(ev.getWorld().getName(), ev.getChunk().getX(), ev.getChunk().getZ());
		});
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
	public void onChukUnLoad(ChunkUnloadEvent ev) {
		Borough.getClaimStore().evictCachedChunk(ev.getWorld().getName(), ev.getChunk().getX(), ev.getChunk().getZ());
	}
}
