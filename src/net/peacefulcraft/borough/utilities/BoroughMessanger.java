package net.peacefulcraft.borough.utilities;

import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.peacefulcraft.borough.Borough;

public class BoroughMessanger {
	
	/**
	 * Sends formatted error message to player
	 * 
	 * @param player Player to send message to
	 * @param message Error message
	 */
	public static void sendErrorMessage(Player player, String message) {
		player.sendMessage(Borough.messagingPrefix + ChatColor.RED + message);
	}

}
