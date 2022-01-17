package net.peacefulcraft.borough.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import net.peacefulcraft.borough.event.actions.ActionType;
import net.peacefulcraft.borough.event.executor.BoroughActionExecutor;
import net.peacefulcraft.borough.utilities.EntityTypeLists;
import net.peacefulcraft.borough.utilities.ItemLists;

public class PlayerListener implements Listener {
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent ev) {
		// Process via normal building permissions
		ev.setCancelled(!BoroughActionExecutor.canBuild(ev.getPlayer(), ev.getBlockClicked().getRelative(ev.getBlockFace()).getLocation(), ev.getBucket()));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerBucketFill(PlayerBucketFillEvent ev) {
		// Process via normal breaking permissions
		ev.setCancelled(!BoroughActionExecutor.canBreak(ev.getPlayer(), ev.getBlockClicked().getRelative(ev.getBlockFace()).getLocation(), ev.getBucket()));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent ev) {
		Player player = ev.getPlayer();
		Block clickedBlock = ev.getClickedBlock();

		// Player is using an item
		if (ev.hasItem()) {
			Material mat = ev.getItem().getType();
			Location loc = clickedBlock != null ? clickedBlock.getLocation() : player.getLocation();

			//ev.setCancelled(!BoroughActionExecutor.canInteract(player, loc, mat));

			if (clickedBlock != null) {
				Material clickedMat = clickedBlock.getType();

				// Checking action and processing via break permissions
				if (isActionProtected(mat, clickedMat)) {
					ev.setCancelled(!BoroughActionExecutor.canBreak(player, loc, clickedMat));
				}

				// Process bone meal with build permissions
				if (mat == Material.BONE_MEAL) {
					ev.setCancelled(!BoroughActionExecutor.canBuild(player, loc, mat));
				}

				// Process candles with build permissions
				if (ItemLists.CANDLES.contains(mat.name()) && clickedMat == Material.CAKE) {
					ev.setCancelled(!BoroughActionExecutor.canBuild(player, loc, mat));
				}

				// Process wax interactions with build permissions
				if (mat == Material.HONEYCOMB && ItemLists.WEATHERABLE_BLOCKS.contains(clickedMat.name())) {
					ev.setCancelled(!BoroughActionExecutor.canBuild(player, loc, mat));
				}

				// Spawning some sort of entity. Process with build permissions
				if (mat == Material.ARMOR_STAND || mat == Material.END_CRYSTAL) {
					ev.setCancelled(!BoroughActionExecutor.canBreak(player, clickedBlock.getRelative(ev.getBlockFace()).getLocation(), mat));
				}
			}
		}

		// Player did not use an item
		if (clickedBlock != null) {
			Material clickedMat = clickedBlock.getType();

			if (ItemLists.isSwitch(clickedMat.name())) {
				ev.setCancelled(!BoroughActionExecutor.canInteract(player, clickedBlock.getLocation(), clickedMat));
				return;
			}

			/**
			 * Test other interactables. This action can be considered to be
			 * destructive so we process via break permissions
			 */
			if (ItemLists.POTTED_PLANTS.contains(clickedMat.name()) ||
				ItemLists.HARVESTABLE_BERRIES.contains(clickedMat.name()) ||
				ItemLists.REDSTONE_INTERACTABLES.contains(clickedMat.name()) ||
				ItemLists.CANDLES.contains(clickedMat.name()) ||
				clickedMat == Material.BEACON || clickedMat == Material.DRAGON_EGG ||
				clickedMat == Material.COMMAND_BLOCK) {

				ev.setCancelled(!BoroughActionExecutor.canBreak(player, clickedBlock.getLocation(), clickedMat));
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInteractArmorStand(PlayerArmorStandManipulateEvent ev) {
		// Process via break permissions
		ev.setCancelled(!BoroughActionExecutor.canBreak(ev.getPlayer(), ev.getRightClicked().getLocation(), Material.ARMOR_STAND));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInteractEntity(PlayerInteractAtEntityEvent ev) {
		
		// An entity was right clicked
		if (ev.getRightClicked() != null) {
			Player p = ev.getPlayer();
			Material mat = null;
			Material item = p.getPlayer().getInventory().getItemInMainHand().getType();

			ActionType action = ActionType.BREAK;

			switch (ev.getRightClicked().getType()) {
				case PUFFERFISH:
				case TROPICAL_FISH:
				case SALMON:
				case COD:
				case ITEM_FRAME:
				case GLOW_ITEM_FRAME:
				case PAINTING:
				case LEASH_HITCH:
				case MINECART_COMMAND:
				case MINECART_TNT:
				case MINECART_MOB_SPAWNER:
				case AXOLOTL:
					mat = EntityTypeLists.parseEntityToMaterial(ev.getRightClicked().getType());
					break;
				case SHEEP:
				case WOLF:
				// Block dying of sheep and wolves
					if (item != null && ItemLists.DYES.contains(item.name())) {
						mat = item;
						break;
					}
				case MINECART_CHEST:
				case MINECART_FURNACE:
				case MINECART_HOPPER:
					mat = EntityTypeLists.parseEntityToMaterial(ev.getRightClicked().getType());
					action = ActionType.INTERACT;
					break;
				default:
			}

			// material was processed and action was committed
			if (mat != null) {
				if (action == ActionType.BREAK) {
					ev.setCancelled(!BoroughActionExecutor.canBreak(p, ev.getRightClicked().getLocation(), mat));
					return;
				}

				if (action == ActionType.INTERACT) {
					ev.setCancelled(!BoroughActionExecutor.canInteract(p, ev.getRightClicked().getLocation(), mat));
					return;
				}
			}

			if (item == Material.NAME_TAG) {
				ev.setCancelled(!BoroughActionExecutor.canBreak(p, ev.getRightClicked().getLocation(), mat));
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerTeleport(PlayerTeleportEvent ev) {

		Player p = ev.getPlayer();
		if (ev.getCause() == TeleportCause.ENDER_PEARL) {
			ev.setCancelled(!BoroughActionExecutor.canItemUse(p, ev.getTo(), Material.ENDER_PEARL));
		} else if (ev.getCause() == TeleportCause.CHORUS_FRUIT) {
			ev.setCancelled(!(BoroughActionExecutor.canItemUse(p, ev.getTo(), Material.CHORUS_FRUIT)));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerTakeLecturnBook(PlayerTakeLecternBookEvent ev) {
		ev.setCancelled(!BoroughActionExecutor.canBreak(ev.getPlayer(), ev.getLectern().getLocation(), Material.LECTERN));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerEggThrow(PlayerEggThrowEvent ev) {
		ev.setHatching(!BoroughActionExecutor.canItemUse(ev.getPlayer(), ev.getEgg().getLocation(), Material.EGG));
	}

	/**
	 * Basic checks for non-permitted actions within a claim. 
	 * NOTE: Keep this check updated as interactable items are added
	 * 
	 * @param mat Material in a players hand
	 * @param clickedMat Material the player clicked on
	 * @return True if action is a protected action. False otherwise
	 */
	private boolean isActionProtected(Material mat, Material clickedMat) {
		return ((ItemLists.AXES.contains(mat.name()) && Tag.LOGS.isTagged(clickedMat)) || // Protecting against stripping logs
		(ItemLists.AXES.contains(mat.name()) && ItemLists.WAXED_BLOCKS.contains(clickedMat.name())) ||// Preventing removing wax from copper
		(ItemLists.AXES.contains(mat.name()) && ItemLists.WEATHERABLE_BLOCKS.contains(clickedMat.name())) || //Preventing removal of oxidation
		(ItemLists.DYES.contains(mat.name()) && Tag.SIGNS.isTagged(clickedMat)) || // Preventing dying signs
		(mat == Material.FLINT_AND_STEEL && clickedMat == Material.TNT) || // Preventing using tnt
		((mat == Material.GLASS_BOTTLE || mat == Material.SHEARS) && (clickedMat == Material.BEE_NEST || clickedMat == Material.BEEHIVE || clickedMat == Material.PUMPKIN)));
	}

}
