package net.peacefulcraft.borough.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.peacefulcraft.borough.Borough;
import net.peacefulcraft.borough.storage.BoroughChunk;
import net.peacefulcraft.borough.storage.BoroughChunkPermissionLevel;
import net.peacefulcraft.borough.storage.BoroughClaim;

public class ClaimCommand implements CommandExecutor, TabCompleter {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(Borough.messagingPrefix + "This command requires location data and is only usable by players.");
			return true;
		}
		Player p = (Player) sender;

		if (args.length == 0) {
			this.sendMainHelpMessage(sender);
			return true;
		}

		// String switch is case sensitive, but more efficient
		String subCommand = args[0].toLowerCase();
		switch (subCommand) {
			case "create":
				if (args.length > 1) {
					Chunk chunk = p.getLocation().getChunk();
					// Go async because blocking SQL might happen
					CompletableFuture.runAsync(() -> {
						try {
							BoroughChunk boroughChunk = Borough.getClaimStore().getChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
							if (boroughChunk.isChunkClaimed()) {
								// Go back to Bukkit land to do Bukkit things
								Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
									sender.sendMessage(Borough.messagingPrefix + "The chunk you're standing in is already claimed.");
								});
							} else {
								BoroughClaim claim = Borough.getClaimStore().createClaim(args[2], p.getUniqueId());
								Borough.getClaimStore().claimChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), claim);
		
								// Go back to Bukkit land to do Bukkit things
								Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
									sender.sendMessage(Borough.messagingPrefix + "Succesfully created claim " + args[2] + " and claimed the chunk you were standing in.");
								});
							}
						} catch (RuntimeException ex) {
							sender.sendMessage(Borough.messagingPrefix + "An error occured while trying to create claim " + args[2] + ". Please try again. Contact staff if the issue persists.");
						}
					});
				} else {
					sender.sendMessage(Borough.messagingPrefix + "'create' command expects a claim name too. Claim names cannot contain spaces.");
				}
				break;

			case "extend":
				if (args.length > 1) {
					if (!Borough.getClaimStore().getClaimsByUser(p.getUniqueId(), BoroughChunkPermissionLevel.OWNER).contains(args[2])) {
						sender.sendMessage(Borough.messagingPrefix + "Unknown claim " + args[2] + ". Do you have ownership permissions on this claim?");
					} else {
						Chunk chunk = p.getLocation().getChunk();
						// Go async because blocking SQL might happen
						CompletableFuture.runAsync(() -> {
							try {
								BoroughClaim claim = Borough.getClaimStore().getClaim(args[2]);
								BoroughChunk boroughChunk = Borough.getClaimStore().getChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
								if (boroughChunk.isChunkClaimed()) {
										// Go back to Bukkit land to do Bukkit things
										Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
											sender.sendMessage(Borough.messagingPrefix + "The chunk you're standing in is already claimed.");
										});	
								} else {
									Borough.getClaimStore().claimChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), claim);
								
									// Go back to Bukkit land to do Bukkit things
									Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
										sender.sendMessage(Borough.messagingPrefix + "Succesfully extended claim " + args[2] + " to the chunk you were standing in.");
									});	
								}
							} catch (RuntimeException ex) {
								sender.sendMessage(Borough.messagingPrefix + "An error occured while trying to extend claim " + args[2] + ". Please try again. Contact staff if the issue persists.");
							}
						});
					}
				} else {
					sender.sendMessage(Borough.messagingPrefix + "'extend' command expects a claim name too. Claim names cannot contain spaces.");
				}
				break;

			case "delete":
				if (args.length > 1) {
					if (!Borough.getClaimStore().getClaimsByUser(p.getUniqueId(), BoroughChunkPermissionLevel.OWNER).contains(args[2])) {
						sender.sendMessage(Borough.messagingPrefix + "Unknown claim " + args[2] + ". Do you have ownership permissions on this claim?");
					} else if (args.length > 2 && args[3].equalsIgnoreCase("confirm")) {
						// Go async because blocking SQL might happen
						CompletableFuture.runAsync(() -> {
							try {
								Borough.getClaimStore().deleteClaim(args[2]);

								// Go back to Bukkit land to do Bukkit things
								Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
									sender.sendMessage(Borough.messagingPrefix + "Succesfully extended claim " + args[2] + " to the chunk you were standing in.");
								});
							} catch (RuntimeException ex) {
								sender.sendMessage(Borough.messagingPrefix + "An error occured while trying to delete claim " + args[2] + ". Please try again. Contact staff if the issue persists.");
							}
						});
					} else {
						sender.sendMessage(Borough.messagingPrefix + "Will delete ALL chunk claims apart of " + args[2] + ". To confirm run " + ChatColor.GOLD + "/claim delete " + args[2] + "confirm");
					}
				} else {
					sender.sendMessage(Borough.messagingPrefix + "'extend' command expects a claim name too. Claim names cannot contain spaces.");
				}
				break;

			case "info":
				if (args.length > 1) {
					if (!Borough.getClaimStore().getClaimsByUser(p.getUniqueId(), BoroughChunkPermissionLevel.BUILDER).contains(args[2])) {
						sender.sendMessage(Borough.messagingPrefix + "Unknown claim " + args[2] + ".");
					} else {
						Chunk chunk = p.getLocation().getChunk();
						// Go async because blocking SQL might happen
						CompletableFuture.runAsync(() -> {
							try {
								BoroughClaim claim = Borough.getClaimStore().getClaim(args[2]);
								
								final StringBuilder ownerUsernames = new StringBuilder();
								claim.getOwners().forEach((uuid) -> { ownerUsernames.append(Borough.getUUIDCache().uuidToUsername(uuid) + ", "); });

								final StringBuilder moderatorUsernames = new StringBuilder();
								claim.getOwners().forEach((uuid) -> { moderatorUsernames.append(Borough.getUUIDCache().uuidToUsername(uuid) + ", "); });

								final StringBuilder builderUsernames = new StringBuilder();
								claim.getOwners().forEach((uuid) -> { builderUsernames.append(Borough.getUUIDCache().uuidToUsername(uuid) + ", "); });

								// Go back to Bukkit land to do Bukkit things
								Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
									sender.sendMessage(Borough.messagingPrefix + "------------------------------");
									sender.sendMessage(ChatColor.GRAY + "Owned by: " + ownerUsernames.toString().subSequence(0, ownerUsernames.length() - 2));
									sender.sendMessage(ChatColor.GRAY + "Moderators: " + moderatorUsernames.toString().subSequence(0, moderatorUsernames.length() - 2));
									sender.sendMessage(ChatColor.GRAY + "Builders: " + builderUsernames.toString().subSequence(0, builderUsernames.length() - 2));
								});	
							} catch (RuntimeException ex) {
								sender.sendMessage(Borough.messagingPrefix + "An error occured while trying to access information on claim " + args[2] + ". Please try again. Contact staff if the issue persists.");
							}
						});
					}
				} else {
					sender.sendMessage(Borough.messagingPrefix + "'info' command expects a claim name too. Claim names cannot contain spaces.");
				}
				break;

			case "add-builder":
				if (args.length > 1) {
					if (!Borough.getClaimStore().getClaimsByUser(p.getUniqueId(), BoroughChunkPermissionLevel.MODERATOR).contains(args[2])) {
						sender.sendMessage(Borough.messagingPrefix + "Unknown claim " + args[2] + ". Do you have moderator or greater permissions on this claim?");
					} else {
						if (args.length > 2) {
							String username = args[3];

							// Go async because blocking SQL might happen
							CompletableFuture.runAsync(() -> {
								try {
									UUID uuid = Borough.getUUIDCache().usernameToUUID(username);
									if (uuid == null) {
										Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
											sender.sendMessage(Borough.messagingPrefix + "User " + username + " is not known. Have they joined this server before?");
										});
									} else {
										BoroughClaim claim = Borough.getClaimStore().getClaim(args[2]);

										// Already have perms or add new perms does not matter. Just report success
										try { claim.addBuilder(uuid); }
										catch (IllegalArgumentException ex) {}
										
										
										Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
											sender.sendMessage(Borough.messagingPrefix + "Granted " + username + " builder permissions to claim " + args[2]);
										});
									}
								} catch (RuntimeException ex) {
									sender.sendMessage(Borough.messagingPrefix + "An error occured while trying to modify permissions on claim " + args[2] + ". Please try again. Contact staff if the issue persists.");
								}
							});
						}
					}
				} else {
					sender.sendMessage(Borough.messagingPrefix + "'add-builder' command expects a claim name too. Claim names cannot contain spaces.");
				}
				break;

			case "add-moderator":
				if (args.length > 1) {
					if (!Borough.getClaimStore().getClaimsByUser(p.getUniqueId(), BoroughChunkPermissionLevel.ADMINISTRATOR).contains(args[2])) {
						sender.sendMessage(Borough.messagingPrefix + "Unknown claim " + args[2] + ". Do you have administrative or greater permissions on this claim?");
					} else {
						if (args.length > 2) {
							String username = args[3];

							// Go async because blocking SQL might happen
							CompletableFuture.runAsync(() -> {
								try {
									UUID uuid = Borough.getUUIDCache().usernameToUUID(username);
									if (uuid == null) {
										Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
											sender.sendMessage(Borough.messagingPrefix + "User " + username + " is not known. Have they joined this server before?");
										});
									} else {
										BoroughClaim claim = Borough.getClaimStore().getClaim(args[2]);

										// Already have perms or add new perms does not matter. Just report success
										try { claim.addModerator(uuid); }
										catch (IllegalArgumentException Ex) {}

										Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
											sender.sendMessage(Borough.messagingPrefix + "Granted " + username + " moderator permissions to claim " + args[2]);
										});
									}
								} catch (RuntimeException ex) {
									sender.sendMessage(Borough.messagingPrefix + "An error occured while trying to modify permissions on claim " + args[2] + ". Please try again. Contact staff if the issue persists.");
								}
							});
						}
					}
				} else {
					sender.sendMessage(Borough.messagingPrefix + "'add-moderator' command expects a claim name too. Claim names cannot contain spaces.");
				}
				break;

			case "add-admin":
				if (args.length > 1) {
					if (!Borough.getClaimStore().getClaimsByUser(p.getUniqueId(), BoroughChunkPermissionLevel.ADMINISTRATOR).contains(args[2])) {
						sender.sendMessage(Borough.messagingPrefix + "Unknown claim " + args[2] + ". Do you have administrative permissions on this claim?");
					} else {
						if (args.length > 2) {
							String username = args[3];

							// Go async because blocking SQL might happen
							CompletableFuture.runAsync(() -> {
								try {
									UUID uuid = Borough.getUUIDCache().usernameToUUID(username);
									if (uuid == null) {
										Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
											sender.sendMessage(Borough.messagingPrefix + "User " + username + " is not known. Have they joined this server before?");
										});
									} else {
										BoroughClaim claim = Borough.getClaimStore().getClaim(args[2]);
										
										// Already have perms or add new perms does not matter. Just report success
										try { claim.addModerator(uuid); }
										catch (IllegalArgumentException ex) {}

										Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
											sender.sendMessage(Borough.messagingPrefix + "Granted " + username + " administrative permissions to claim " + args[2]);
										});
									}
								} catch (RuntimeException ex) {
									sender.sendMessage(Borough.messagingPrefix + "An error occured while trying to modify permissions on claim " + args[2] + ". Please try again. Contact staff if the issue persists.");
								}
							});
						}
					}
				} else {
					sender.sendMessage(Borough.messagingPrefix + "'add-admin' command expects a claim name too. Claim names cannot contain spaces.");
				}
				break;

			case "remove-user":
				if (args.length > 1) {
					if (!Borough.getClaimStore().getClaimsByUser(p.getUniqueId(), BoroughChunkPermissionLevel.MODERATOR).contains(args[2])) {
						sender.sendMessage(Borough.messagingPrefix + "Unknown claim " + args[2] + ". Do you have moderator permissions on this claim?");
					} else {
						if (args.length > 2) {
							String username = args[3];

							// Go async because blocking SQL might happen
							CompletableFuture.runAsync(() -> {
								try {
									UUID uuid = Borough.getUUIDCache().usernameToUUID(username);
									if (uuid == null) {
										Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
											sender.sendMessage(Borough.messagingPrefix + "User " + username + " is not known. Have they joined this server before?");
										});
									} else {
										BoroughClaim claim = Borough.getClaimStore().getClaim(args[2]);
										
										// If it's a moderator trying to remove an admin, block the request
										if (claim.getModerators().contains(p.getUniqueId())) {
											sender.sendMessage(Borough.messagingPrefix + "User " + username + " is an administrator. Only any other administrator can remove their permissions.");
										} else {
											// TOOD: Make better
											// Check them all incase the user got double added
											if (claim.getBuilders().contains(uuid)) {
												claim.removeBuilder(uuid);
											}

											if (claim.getModerators().contains(uuid)) {
												claim.removeModerator(uuid);
											}

											if (claim.getOwners().contains(uuid)) {
												claim.removeOwner(uuid);
											}
										}

										Borough._this().getServer().getScheduler().runTask(Borough._this(), ()-> {
											sender.sendMessage(Borough.messagingPrefix + "Removed " + username + " permissions to claim " + args[2]);
										});
									}
								} catch (RuntimeException ex) {
									sender.sendMessage(Borough.messagingPrefix + "An error occured while trying to modify permissions on claim " + args[2] + ". Please try again. Contact staff if the issue persists.");
								}
							});
						}
					}
				} else {
					sender.sendMessage(Borough.messagingPrefix + "'remove-user' command expects a claim name too. Claim names cannot contain spaces.");
				}
				break;

			default:
				sender.sendMessage(Borough.messagingPrefix + "Unknown option " + args[0] + ". Valid sub commands are:");
				this.sendMainHelpMessage(sender);
		}

		return true;
	}

	private void sendMainHelpMessage(CommandSender sender) {

	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		ArrayList<String> opts = new ArrayList<String>();
		if (!(sender instanceof Player)) { return opts; }
		Player p = (Player) sender;

		if (command.getName().equalsIgnoreCase("claim")) {
			switch (args.length) {
				case 1:
					opts.add("create");
					opts.add("extend");
					opts.add("delete");
					opts.add("info");
					opts.add("add-builder");
					opts.add("add-moderator");
					opts.add("add-admin");
					opts.add("remove-user");
					this.argMatch(opts, args[0]);
					break;
				case 2:
					if (args[1].equalsIgnoreCase("add-builder") || args[1].equalsIgnoreCase("remove-user")) {
						opts.addAll(Borough.getClaimStore().getClaimsByUser(p.getUniqueId(), BoroughChunkPermissionLevel.MODERATOR));
					} else if (args[1].equalsIgnoreCase("add-moderator") || args[1].equalsIgnoreCase("add-admin")) {
						opts.addAll(Borough.getClaimStore().getClaimsByUser(p.getUniqueId(), BoroughChunkPermissionLevel.ADMINISTRATOR));
					}
					this.argMatch(opts, args[1]);
					break;
			}
		}

		return opts;
	}

	/**
	 * Crude way to filter the current list of suggestions by what the user has typed so far for the given arguemnt
	 * @param suggestions Reference to the suggestions list
	 * @param arg The current arguemnt that has been typed so far
	 * No return value. Uses references.
	 */
	private void argMatch(ArrayList<String> suggestions, String arg) {
		if (arg.trim().length() == 0) { return; }
		Iterator<String> i = suggestions.iterator();
		while (i.hasNext()) {
			String opt = i.next();
			if (!opt.contains(arg)) {
				i.remove();
			}
		}
	}
}
