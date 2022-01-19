package net.peacefulcraft.borough.listeners;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.peacefulcraft.borough.Borough;
import net.peacefulcraft.borough.storage.BoroughChunkPermissionLevel;

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
}
