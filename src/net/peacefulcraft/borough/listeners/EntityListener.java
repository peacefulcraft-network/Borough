package net.peacefulcraft.borough.listeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PigZapEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.BlockProjectileSource;

import net.peacefulcraft.borough.Borough;
import net.peacefulcraft.borough.event.executor.BoroughActionExecutor;
import net.peacefulcraft.borough.storage.BoroughChunk;
import net.peacefulcraft.borough.utilities.CombatUtil;
import net.peacefulcraft.borough.utilities.EntityTypeLists;
import net.peacefulcraft.borough.utilities.ItemLists;
import net.peacefulcraft.borough.utilities.PotionTypeLists;

public class EntityListener implements Listener {
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent ev) {
		Entity damager = ev.getDamager();
		Entity victim = ev.getEntity();

		// Pre-process entity related explosions: Creeper, etc.
		if (ev.getCause() == DamageCause.ENTITY_EXPLOSION && !(damager instanceof Projectile)) {
			boolean cancelDamage = false;

			// Protect via action executor
			if (!BoroughActionExecutor.canExplosionDamageEntities(victim.getLocation(), victim, ev.getCause())) {
				cancelDamage = true;
			}

			if (victim instanceof Player && EntityTypeLists.isPVPExplosive(damager.getType())) {
				cancelDamage = CombatUtil.preventPVP(Borough.getClaimStore().getChunk(victim.getLocation()));
			}

			if (cancelDamage) {
				ev.setDamage(0);
				ev.setCancelled(true);
				return;
			}
		}

		// Processing remaining non-explosive damage
		if (CombatUtil.preventDamageCall(damager, victim, ev.getCause())) {
			// Delete those nasty projectiles
			if (damager instanceof Projectile && !damager.getType().equals(EntityType.TRIDENT)) {
				damager.remove();
			}

			ev.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent ev) {
		if (ev.getTarget() instanceof Player) {
			if (ev.getReason().equals(EntityTargetEvent.TargetReason.TEMPT)) {
				Location loc = ev.getEntity().getLocation();

				BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);
				if (chunk == null) { return; }

				ev.setCancelled(!BoroughActionExecutor.canBreak((Player) ev.getTarget(), loc, Material.DIRT));
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityTakesBlockExplosion(EntityDamageEvent ev) {
		if ((ev.getCause() == DamageCause.BLOCK_EXPLOSION || ev.getCause() == DamageCause.LIGHTNING)  &&
		!BoroughActionExecutor.canExplosionDamageEntities(ev.getEntity().getLocation(), ev.getEntity(), ev.getCause())) {
			ev.setDamage(0);
			ev.setCancelled(true);
		}

		/**
		 * The HangingBreakEvent does not prevent the item in the frame
		 * from being destroyed / popping out.
		 * We address in two locations.
		 */
		if (ev.getEntity() instanceof ItemFrame && ev instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent evv = (EntityDamageByEntityEvent)ev;

			Object remover = evv.getDamager() instanceof Projectile ? ((Projectile)evv.getDamager()).getShooter() : evv.getDamager();
			if (remover instanceof Player) {
				ev.setCancelled(!BoroughActionExecutor.canBreak((Player)remover, ev.getEntity().getLocation(), Material.ITEM_FRAME));
			} else {
				BoroughChunk chunk = Borough.getClaimStore().getChunk(ev.getEntity().getLocation());
				if (chunk != null) { ev.setCancelled(true); }
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onLingeringPotionEvent(LingeringPotionSplashEvent ev) {
		ThrownPotion pot = ev.getEntity();
		Location loc = pot.getLocation();

		float radius = ev.getAreaEffectCloud().getRadius();
		List<Block> blocks = new ArrayList<>();

		// We want a list of blocks in the radius
		for (double x = loc.getX() - radius; x < loc.getX() + radius; x++) {
			for (double z = loc.getZ() - radius; z < loc.getZ() + radius; z++) {
				Location temp = new Location(pot.getWorld(), x, loc.getY(), z);
				Block block = temp.getBlock();
				if (block.getType().equals(Material.AIR)) { blocks.add(block); }
			}
		}

		List<PotionEffect> effects = (List<PotionEffect>) pot.getEffects();
		boolean negativeEffect = false;

		for (PotionEffect effect : effects) {
			if (PotionTypeLists.isNegativeEffect(effect.getType())) {
				negativeEffect = true;
			}
		}

		for (Block block : blocks) {
			BoroughChunk chunk = Borough.getClaimStore().getChunk(block.getLocation());
			if (chunk != null && CombatUtil.preventPVP(chunk) && negativeEffect) {
				ev.setCancelled(true);
				break;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPotionSplash(PotionSplashEvent ev) {
		List<LivingEntity> affected = (List<LivingEntity>)ev.getAffectedEntities();
		ThrownPotion potion = ev.getPotion();
		Entity attacker = null;

		List<PotionEffect> effects = (List<PotionEffect>) potion.getEffects();
		boolean negativeEffects = false;

		for (PotionEffect effect : effects) {
			if (PotionTypeLists.isNegativeEffect(effect.getType())) {
				negativeEffects = true;
			}
		}

		Object shooter = potion.getShooter();
		Block dispenser = null;

		if (shooter instanceof BlockProjectileSource) {
			dispenser = ((BlockProjectileSource)shooter).getBlock();
		} else {
			attacker = (Entity) shooter;
		}

		for (LivingEntity victim : affected) {
			if (dispenser != null) {
				if (CombatUtil.preventDispenserDamage(dispenser, victim, DamageCause.MAGIC) && negativeEffects) {
					ev.setIntensity(victim, -1.0);
				}
			} else {
				if (attacker != victim) {
					if (CombatUtil.preventDamageCall(attacker, victim, DamageCause.MAGIC) && negativeEffects) {
						ev.setIntensity(victim, -1.0);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityInteract(EntityInteractEvent ev) {
		/**
		 * We want to handle interact events involving
		 * pressure plates and non-players
		 */

		Block block = ev.getBlock();
		Entity entity = ev.getEntity();
		List<Entity> passengers = entity.getPassengers();

		// Allow players in vehicles to trigger pressure plates via permissions
		if (passengers != null) {
			for (Entity passenger : passengers) {
				if (!(passenger instanceof Player)) { return; }

				// If block is some sort of redstone switch
				String matName = block.getType().name();
				if (ItemLists.isSwitch(matName)) {
					ev.setCancelled(!BoroughActionExecutor.canInteract((Player)passenger, block.getLocation(), block.getType()));
					return;
				}
			}
		}

		if (Borough.getConfiguration().isCreatureTriggeringPressurPlateEnabled()) {
			if (block.getType() == Material.STONE_PRESSURE_PLATE) {
				if (entity instanceof Creature) {
					ev.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityChangeBlock(EntityChangeBlockEvent ev) {
		/**
		 * Handles entity performing a change in a protected chunk
		 * 	Enderman, Ravagers, Withers, water on campfires, crops
		 */

		// Crop trampling
		if (ev.getBlock().getType().equals(Material.FARMLAND)) {
			// Non-player crop trampling
			if (!ev.getEntityType().equals(EntityType.PLAYER) && Borough.getConfiguration().isCreatureTrampleEnabled()) {
				ev.setCancelled(true);
				return;
			}

			// Player crop trampling via permissions
			if (ev.getEntityType().equals(EntityType.PLAYER) && Borough.getConfiguration().isPlayerTrampleEnabled()) {
				ev.setCancelled(BoroughActionExecutor.canBreak((Player)ev.getEntity(), ev.getBlock().getLocation(), ev.getBlock().getType()));
				return;
			}
		}

		switch (ev.getEntity().getType()) {
			case ENDERMAN:
				if (!Borough.getConfiguration().isEndermanGriefEnabled()) {
					ev.setCancelled(false);
				}
				break;
			case RAVAGER:
				if (!Borough.getConfiguration().isCreatureTrampleEnabled()) {
					ev.setCancelled(true);
				}
				break;
			case WITHER:
				List<Block> allowed = BoroughActionExecutor.filterExplodeableBlocks(Collections.singletonList(ev.getBlock()), ev.getBlock().getType(), ev.getEntity(), ev);
				ev.setCancelled(allowed.isEmpty());
				break;
			case SPLASH_POTION:
				if (ev.getBlock().getType() != Material.CAMPFIRE && ((ThrownPotion)ev.getEntity()).getShooter() instanceof Player) {
					return;
				}
				ev.setCancelled(!BoroughActionExecutor.canBreak((Player) ((ThrownPotion)ev.getEntity()).getShooter(), ev.getBlock().getLocation(), ev.getBlock().getType()));
				break;
			default:		
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent ev) {
		List<Block> blocks = BoroughActionExecutor.filterExplodeableBlocks(ev.blockList(), null, ev.getEntity(), ev);
		ev.blockList().clear();
		ev.blockList().addAll(blocks);

		if (ev.blockList().isEmpty()) { return; }
		// TODO: Look into some sort of regen service
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityCombustByEntity(EntityCombustByEntityEvent ev) {
		Entity combuster = ev.getCombuster();
		Entity defender = ev.getEntity();

		LivingEntity attacker = null;
		if (combuster instanceof Projectile) {
			Projectile projectile = (Projectile)combuster;

			Object source = projectile.getShooter();
			if (source instanceof BlockProjectileSource) {
				if (CombatUtil.preventDispenserDamage(((BlockProjectileSource)source).getBlock(), defender, DamageCause.PROJECTILE)) {
					combuster.remove();
					ev.setCancelled(true);
					return;
				}
			} else {
				attacker = (LivingEntity) source;
			}

			if (attacker != null) {
				if (CombatUtil.preventDamageCall(attacker, defender, DamageCause.PROJECTILE)) {
					combuster.remove();
					ev.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onHangingBreak(HangingBreakEvent ev) {
		Entity hang = ev.getEntity();

		// If the object was removed by physics and it is a registered hanging entity
		if (ev.getCause().equals((RemoveCause.PHYSICS)) && ItemLists.HANGING_ENTITIES.contains(hang.getType().name())) {
			Location loc = hang.getLocation().add(hang.getFacing().getOppositeFace().getDirection());
			
			BoroughChunk chunk = Borough.getClaimStore().getChunk(loc);
			if (chunk == null) { return; }

			// If the chunk is claimed and prevents block damage cancel event
			if (chunk.isChunkClaimed() && !chunk.doesAllowBlockDamage()) {
				ev.setCancelled(true);
				return;
			}
		}

		if (ev.getCause().equals(RemoveCause.PHYSICS) && ItemLists.ITEM_FRAMES.contains(hang.getType().name())) {
			Block block = hang.getLocation().add(hang.getFacing().getOppositeFace().getDirection()).getBlock();

			if (block.isLiquid() || block.isEmpty()) { return; }

			// Preventing boats from breaking entities
			for (Entity entity : hang.getNearbyEntities(0.5, 0.5, 0.5)) {
				if (entity instanceof Vehicle) {
					ev.setCancelled(true);
					return;
				}
			}
		}

		// Object broken by another entity. Projectile, player, non-player, etc.
		if (ev instanceof HangingBreakByEntityEvent) {
			HangingBreakByEntityEvent evv = (HangingBreakByEntityEvent)ev;
			Object remover = evv.getRemover();

			// Pre-process projectiles to get origin
			if (remover instanceof Projectile) {
				remover = ((Projectile)remover).getShooter();
			}

			// If they are a player we process via permissions
			if (remover instanceof Player) {
				Material mat = EntityTypeLists.parseEntityToMaterial(ev.getEntity().getType(), Material.GRASS_BLOCK);
				ev.setCancelled(!BoroughActionExecutor.canBreak((Player)remover, hang.getLocation(), mat));
			} else if (remover instanceof Entity) {
				// Protecting a claimed chunk from entity attacks. I.e. skeleton
				BoroughChunk chunk = Borough.getClaimStore().getChunk(hang.getLocation());
				if (chunk != null) {
					ev.setCancelled(true);
				}
			}

			if (ev.getCause() == RemoveCause.EXPLOSION) {
				if (!BoroughActionExecutor.canExplosionDamageEntities(hang.getLocation(), hang, DamageCause.ENTITY_EXPLOSION)) {
					ev.setCancelled(true);
				} 
			}
		} else {
			if (ev.getCause() == RemoveCause.EXPLOSION) {
				if (!BoroughActionExecutor.canExplosionDamageEntities(ev.getEntity().getLocation(), ev.getEntity(), DamageCause.BLOCK_EXPLOSION)) {
					ev.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)	
	public void onHangingPlace(HangingPlaceEvent ev) {
		Material mat = EntityTypeLists.parseEntityToMaterial(ev.getEntity().getType(), Material.GRASS_BLOCK);
		
		ev.setCancelled(!BoroughActionExecutor.canBuild(ev.getPlayer(), ev.getEntity().getLocation(), mat));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPigHitByLightning(PigZapEvent ev) {
		// This event for some reason needs to be specifically monitored
		if (!BoroughActionExecutor.canExplosionDamageEntities(ev.getEntity().getLocation(), ev.getEntity(), DamageCause.LIGHTNING)) {
			ev.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onProjectileHitSwitch(ProjectileHitEvent ev) {
		// We don't care if no block is hit or if shooter is not player
		if (ev.getHitBlock() == null || !(ev.getEntity().getShooter() instanceof Player)) { return; } 

		Block block = ev.getHitBlock().getRelative(ev.getHitBlockFace());

		/**
		 * Protecting against unwanted switches and chorus flower breaking
		 */
		if ((ItemLists.PROJECTILE_TRIGGERED_REDSTONE.contains(block.getType().name()) && ItemLists.isSwitch(block.getType().name())) || block.getType().equals(Material.CHORUS_FLOWER)) {
			if (!BoroughActionExecutor.canInteract((Player) ev.getEntity().getShooter(), block.getLocation(), block.getType())) {
				/**
				 * Not possible to cancel the event using normal means.
				 * TODO: Look into a replacement because replacing a block
				 * in this manner can be disgusting
				 */
				BlockData data = block.getBlockData();
				block.setType(Material.AIR);
				Borough._this().getServer().getScheduler().runTask(Borough._this(), () -> {
					block.setBlockData(data);
				});
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityBreakDoor(EntityBreakDoorEvent ev) {
		BoroughChunk chunk = Borough.getClaimStore().getChunk(ev.getBlock().getLocation());
		if (chunk == null) { return; }

		if (!chunk.doesAllowBlockDamage()) {
			ev.setCancelled(true);
		}
	}

}
