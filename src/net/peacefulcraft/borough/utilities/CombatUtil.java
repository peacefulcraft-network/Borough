package net.peacefulcraft.borough.utilities;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.BlockProjectileSource;

import net.peacefulcraft.borough.Borough;
import net.peacefulcraft.borough.event.executor.BoroughActionExecutor;
import net.peacefulcraft.borough.storage.BoroughChunk;

/**
 * Holds relevant methods for determining
 * the permissions of combat near and around claims
 */
public class CombatUtil {
	
	/**
	 * Tests the entities involved in damage to determine cancelling event
	 * due to PvP, friendly fire, etc.
	 * 
	 * Disallows strangers harming pets within boundaries of a chunk
	 * 
	 * @param attacker Attacking entity
	 * @param defender Defending entity
	 * @param cause Cause of damage
	 * @return True if event should be cancelled. False otherwise
	 */
	public static boolean preventDamageCall(Entity attacker, Entity defender, DamageCause cause) {

		Player attackingPlayer = null;
		Player defendingPlayer = null;

		Entity source = attacker;

		if (attacker instanceof Projectile) {
			Object projSource = ((Projectile)attacker).getShooter();
			if (projSource instanceof Entity) {
				source = (Entity)projSource; 
			} else if (projSource instanceof BlockProjectileSource) {
				if (CombatUtil.preventDispenserDamage(((BlockProjectileSource) projSource).getBlock(), defender, cause)) {
					return true;
				}
			}
		}

		if (source instanceof Player) {
			attackingPlayer = (Player)source;
		}
		if (defender instanceof Player) {
			defendingPlayer = (Player)defender;
		}

		// Allow self damage
		if (attackingPlayer == defendingPlayer && attackingPlayer != null && defendingPlayer != null) {
			return false;
		}

		return preventDamageCall(attacker, defender, attackingPlayer, defendingPlayer, cause);
	}

	private static boolean preventDamageCall(Entity attacker, Entity defender, Player attackingPlayer, Player defendingPlayer, DamageCause cause) {

		Projectile projectileAttacker = null;
		if (attacker instanceof Projectile) {
			projectileAttacker = (Projectile)attacker;
			if (projectileAttacker.getShooter() instanceof Entity) {
				attacker = (Entity)projectileAttacker.getShooter();
			}
		}

		BoroughChunk defenderChunk = Borough.getClaimStore().getChunk(defender.getLocation());
		BoroughChunk attackerChunk = Borough.getClaimStore().getChunk(attacker.getLocation());

		// Processing attacking player if exists
		if (attackingPlayer != null) {
			boolean cancelled = false;

			// Defender is player exists
			if (defendingPlayer != null) {
				cancelled = preventFriendlyFire(attackingPlayer, defendingPlayer);
				return cancelled;
			} else {
				// Check if defender is in claim
				if (defenderChunk != null) {

					if (defender instanceof Wolf) {
						// Check if attacking player is owner of defending wolf
						if (!isOwner((Wolf)defender, attackingPlayer)) {
							// If attacking player has break permissions then they can attack this wolf
							// TODO: Possibly change this to a form of entity damage action
							return BoroughActionExecutor.canBreak(attackingPlayer, defender.getLocation(), Material.STONE);
						}
						return false;
					}

					// Check if entity is passive and permissions for farming
					if (EntityTypeLists.isPassive(defender.getType())) {
						// TODO: Possibly change this to a form of entity damage action
						return BoroughActionExecutor.canBreak(attackingPlayer, defender.getLocation(), Material.WHEAT);
					}
				}
			}

			// We want to protect these entities within a claim
			Material material = switch (defender.getType()) {
				case ITEM_FRAME:
				case GLOW_ITEM_FRAME:
				case PAINTING:
				case ARMOR_STAND:
				case ENDER_CRYSTAL:
				case MINECART:
				case MINECART_CHEST:
				case MINECART_FURNACE:
				case MINECART_COMMAND:
				case MINECART_HOPPER:
					yield EntityTypeLists.parseEntityToMaterial(defender.getType());
				default:
					yield null;
			};

			if (material != null) {
				// Player with permissions can destroy protected entity
				return !BoroughActionExecutor.canBreak(attackingPlayer, defender.getLocation(), material);
			}
		} else {
			// Not an attacking player

			// Defender is a player
			if (defendingPlayer != null) {

				// Attacker is a wolf on PVP allowed chunk
				if (attacker instanceof Wolf && (attackerChunk.doesAllowPVP() || defenderChunk.doesAllowPVP())) {
					((Wolf) attacker).setTarget(null);
					((Wolf) attacker).setAngry(false);
					return true;
				}

				if (attacker instanceof LightningStrike && defenderChunk.doesAllowPVP()) {
					// TODO: Additional check against type or reason of lightning. I.e. trident
					return true;
				}
			} else {
				// Non-player vs. non-player damage event
				
				// Defender is in wilderness. 
				if (defenderChunk == null) { return false; }

				// Prevent projectiles fired by non-player harming non-player. Allow damage no non-passive
				if (projectileAttacker != null && !EntityTypeLists.isPassive(defender.getType()) { return true; }

				// Allow wolves to attack non-passive entities
				if (attacker instanceof Wolf && !EntityTypeLists.isPassive(defender.getType())) {
					Wolf wolf = (Wolf)attacker;
					// Wolf has owner and they are online w/ them
					if (wolf.getOwner() instanceof HumanEntity && Bukkit.getPlayer(((HumanEntity) wolf.getOwner()).getUniqueId()) != null) {
						// Prevent damage if the owner does not have permission in the chunk
						return !defenderChunk.canUserBuild(((Player)wolf.getOwner()).getUniqueId());
					} else {
						wolf.setTarget(null);
						wolf.setAngry(false);
						return true;
					}
				}

				if (attacker instanceof Axolotl && EntityTypeLists.isPassive(defender.getType())) {
					((Axolotl)attacker).setTarget(null);
				}
			}
		}
		return false;
	}

	/**
	 * Will friendly fire be prevented in their chunks
	 * @param attacker Attacking entity
	 * @param defender Defending entity
	 * @return True if we should cancel damage
	 */
	public static boolean preventFriendlyFire(Player attacker, Player defender) {
		if (attacker == defender) { return false; }

		// If attacker and defender exist we check their locations and permissions
		if (attacker != null && defender != null) {
			
			BoroughChunk attackerChunk = Borough.getClaimStore().getChunk(attacker.getLocation());
			BoroughChunk defenderChunk = Borough.getClaimStore().getChunk(defender.getLocation());

			// If both are in the wild we allow
			if (!attackerChunk.isChunkClaimed() && !defenderChunk.isChunkClaimed()) { return false; }

			// If Defender is in a chunk that disallowed PvP we prevent
			if (!defenderChunk.doesAllowPVP()) { return true; }
		}
		return false;
	}

	public static boolean preventDispenserDamage(Block dispenser, Entity entity, DamageCause cause) {
		BoroughChunk attackerchunk = Borough.getClaimStore().getChunk(dispenser.getLocation());
		BoroughChunk defenderChunk = Borough.getClaimStore().getChunk(entity.getLocation());
		
		// In general we prevent dispenser damage based on either chunk returning true for block damage
		return attackerchunk.doesAllowBlockDamage() || defenderChunk.doesAllowBlockDamage();
	}

	/**
	 * Is PvP allowed in this block?
	 * 
	 * @param chunk Chunk we are testing
	 * @return True if PvP is allowed
	 */
	public static boolean preventPVP(BoroughChunk chunk) {
		if (chunk != null) {
			// TODO: Perform additional checks against the world level permissions
			// and further against individual claim types. I.e. arena?
			return chunk.doesAllowPVP();
		}
		return true;
	}

	/**
	 * Determines if attacker is owner of wolf
	 * @return True if attacker is owner
	 */
	private static boolean isOwner(Wolf wolf, Player attacker) {
		return wolf.getOwner() instanceof HumanEntity && ((HumanEntity)attacker).getUniqueId().equals(attacker.getUniqueId());
	}

}
