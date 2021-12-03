package net.peacefulcraft.borough.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import net.peacefulcraft.borough.Borough;

public class PlayerCacheEventListeners implements Listener {
	
	/**
	 * Cache player UUIDs when they login.
	 * @param ev
	 */
	@EventHandler
	public void onPlayerJoin(AsyncPlayerPreLoginEvent ev) {
		Borough.getUUIDCache().cacheUUIDUsernameMapping(ev.getUniqueId(), ev.getName());
	}
}
