package net.peacefulcraft.borough.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.peacefulcraft.borough.Borough;

public class BoroughAdmin implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command label, String alias, String[] args) {
		if (!sender.hasPermission("pcn.staff")) { return true; }
		
		if (args.length == 0) { return true; }

		String subCommand = args[0].toLowerCase();
		switch (subCommand) {
			case "cache":
				if (args.length > 1) {
					String cacheType = args[1].toLowerCase();
					switch (cacheType) {
						case "claim":
							sender.sendMessage(Borough.messagingPrefix + " Claim cache has " +  Borough.getClaimStore().getClaimCacheSize() + " keys");
							Borough.getClaimStore().getClaimCacheKeys().forEach((key) -> {
								sender.sendMessage("- " + key);
							});
							break;
						case "member":
							sender.sendMessage(Borough.messagingPrefix + " Member cache has " +  Borough.getClaimStore().getMemberCacheSize() + " keys");
							Borough.getClaimStore().getMemberCacheKeys().forEach((key) -> {
								sender.sendMessage("- " + key.toString());
							});
							break;
						case "permission":
							sender.sendMessage(Borough.messagingPrefix + " Permission cache has " +  Borough.getClaimStore().getPermissionCacheSize() + " keys");
							Borough.getClaimStore().getPermissionCacheKeys().forEach((key) -> {
								sender.sendMessage("- " + key.toString());
							});
							break;
					}
				}
				break;
		}
		
		return true;
	}
	
}
