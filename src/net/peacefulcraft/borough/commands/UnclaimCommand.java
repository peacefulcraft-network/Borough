package net.peacefulcraft.borough.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.peacefulcraft.borough.Borough;
import net.peacefulcraft.borough.storage.BoroughChunk;

public class UnclaimCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(Borough.messagingPrefix + "This command requires location data and is only usable by players.");
			return true;
		}
		Player p = (Player) sender;

		Chunk chunk = p.getLocation().getChunk();
		// Go async because blocking SQL might happen
		CompletableFuture.runAsync(() -> {
			BoroughChunk boroughChunk = Borough.getClaimStore().getChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
			if (boroughChunk.isChunkClaimed()) {
				Borough.getClaimStore().unclaimChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());

				// Go back to Bukkit land to do Bukkit things
				Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
					sender.sendMessage(Borough.messagingPrefix + "The chunk you are standing in is not claimed.");
				});
			} else {
				// Go back to Bukkit land to do Bukkit things
				Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
					sender.sendMessage(Borough.messagingPrefix + "The chunk you are standing in is not claimed.");
				});
			}
 		});

		return true;
	}

}
