package net.peacefulcraft.borough.listeners;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.peacefulcraft.borough.Borough;
import net.peacefulcraft.borough.storage.BoroughChunk;

public class PlayerMovementListener implements Listener {
	
	private HashMap<UUID, Long> playerMap = new HashMap<>();

	@EventHandler
	public void MovementListener(PlayerMoveEvent ev) {
		Long currentTime = System.currentTimeMillis();
		Player p = ev.getPlayer();
		UUID id = p.getUniqueId();

		// If key exists and time difference is > 2 seconds
		// OR if key does not exist
		// We process lookup
		if (!playerMap.containsKey(id) || (playerMap.containsKey(id) && (playerMap.get(id) - currentTime) > 2000)) {
			Borough._this().logDebug("[PlayerMovement] Player Map pass.");
			// Async task call to lookup cache
			Borough._this().getServer().getScheduler().runTaskAsynchronously(Borough._this(), () -> {
				BoroughChunk fromChunk = Borough.getClaimStore().getChunk(ev.getFrom());
				BoroughChunk toChunk = Borough.getClaimStore().getChunk(ev.getTo());

				if (!fromChunk.isChunkClaimed() && toChunk.isChunkClaimed()) {
					// 1. From not claimed. To claimed
					playerMap.put(id, System.currentTimeMillis());
					sendAction(p, "Entered " + toChunk.getClaimMeta().getClaimName());
				} else if(fromChunk.isChunkClaimed() && toChunk.isChunkClaimed() && fromChunk.getClaimMeta().getClaimId() != toChunk.getClaimMeta().getClaimId()) {
					// 2. Both claimed. Compare ids to confirm no match
					playerMap.put(id, System.currentTimeMillis());
					sendAction(p, "Entered " + toChunk.getClaimMeta().getClaimName());
				}
			});
		} 
	}

	private void sendAction(Player p, String message) {
		p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
	}
}
