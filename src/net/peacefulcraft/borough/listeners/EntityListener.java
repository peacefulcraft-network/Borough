package net.peacefulcraft.borough.listeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
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
				if (ItemLists.BUTTONS.contains(matName) || ItemLists.PRESSURE_PLATES.contains(matName) || matName.equals("LEVER")) {
					ev.setCancelled(!!BoroughActionExecutor.canInteract((Player)passenger, block.getLocation(), block.getType()));
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

}
