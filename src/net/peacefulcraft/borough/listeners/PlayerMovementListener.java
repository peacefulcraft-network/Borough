package net.peacefulcraft.borough.listeners;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.peacefulcraft.borough.Borough;
import net.peacefulcraft.borough.storage.BoroughChunk;

public class PlayerMovementListener implements Listener {
	
	private Map<UUID, HistoricalMoveEvent> playerMap = Collections.synchronizedMap(new HashMap<UUID, HistoricalMoveEvent>());

	@EventHandler
	public void MovementListener(PlayerMoveEvent ev) {
		Long currentTime = System.currentTimeMillis();
		Player p = ev.getPlayer();
		UUID id = p.getUniqueId();
 
		// Check if we've dealth with this player before
		if (playerMap.containsKey(id)) {
			// Check if it has been sufficiently long since we've check player movement
			if ((playerMap.get(id).lastCheckRun - currentTime) < 2000) {
				Borough.mysqlThreadPool.submit(() -> {
					HistoricalMoveEvent from = playerMap.get(id);
					BoroughChunk toChunk = Borough.getClaimStore().getChunk(ev.getTo());

					// Check if player has moved between claims since the last time we told them their location.
					if (!from.fromChunk.isChunkClaimed() && toChunk.isChunkClaimed()) {
						// 1. From not claimed. To claimed
						sendAction(p, ChatColor.GREEN + "Entered " + toChunk.getClaimMeta().getCreatorUsername() + "'s claim: " + toChunk.getClaimMeta().getClaimName());
					} else if (from.fromChunk.isChunkClaimed() && !toChunk.isChunkClaimed()) {
						// 1.5 Exiting claim, going into unclaimed land.
						sendAction(p, ChatColor.GRAY + "Entered Unclaimed Territory");
					} else if(from.fromChunk.isChunkClaimed() && toChunk.isChunkClaimed() && 
						from.fromChunk.getClaimMeta() != null && toChunk.getClaimMeta() != null &&
						from.fromChunk.getClaimMeta().getClaimId() != toChunk.getClaimMeta().getClaimId()) {
						// 2. Both claimed. Compare ids to confirm no match
						sendAction(p, ChatColor.GREEN + "Entered " + toChunk.getClaimMeta().getCreatorUsername() + "'s claim: " + toChunk.getClaimMeta().getClaimName());
					}

					from.lastCheckRun = currentTime;
					from.fromChunk = toChunk;
				});
			}
		// new player, initialize datastructures
		} else {
			Borough.mysqlThreadPool.submit(() -> {
				BoroughChunk fromChunk = Borough.getClaimStore().getChunk(ev.getFrom());
				playerMap.put(id, new HistoricalMoveEvent(fromChunk, currentTime));

				// Player probably just appeared in world. Tell them where they are.
				BoroughChunk toChunk = Borough.getClaimStore().getChunk(ev.getTo());
				if (toChunk.isChunkClaimed()) {
					sendAction(p, ChatColor.GREEN + "Entered " + toChunk.getClaimMeta().getCreatorUsername() + "'s claim: " + toChunk.getClaimMeta().getClaimName());
				}
			});
		}
	}

	private void sendAction(Player p, String message) {
		p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
	}

	private class HistoricalMoveEvent {
		public BoroughChunk fromChunk;
		public Long lastCheckRun;

		public HistoricalMoveEvent(BoroughChunk fromChunk, Long lastCheckRun) {
			this.fromChunk = fromChunk;
			this.lastCheckRun = lastCheckRun;
		}
	}

	@EventHandler
	public void PlayerDisconnectListener(PlayerQuitEvent ev) {
		// Remove player from map so the UUID + Chunk can be garbage collected.
		playerMap.remove(ev.getPlayer().getUniqueId());
	}
}