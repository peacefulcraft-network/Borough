package net.peacefulcraft.borough.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import net.peacefulcraft.borough.event.executor.BoroughActionExecutor;
import net.peacefulcraft.borough.utilities.EntityTypeLists;

public class VehicleListener implements Listener {
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onVehicleDamage(VehicleDamageEvent ev) {
		
		// Projectile, tnt, anchor, bed, etc.
		if (ev.getAttacker() == null) {
			ev.setCancelled(!BoroughActionExecutor.canExplosionDamageEntities(ev.getVehicle().getLocation(), ev.getVehicle(), DamageCause.ENTITY_EXPLOSION));
			return;
		}

		if (ev.getAttacker() instanceof Player) {
			Material vehicle = switch (ev.getVehicle().getType()) {
				case MINECART:
				case MINECART_FURNACE:
				case MINECART_HOPPER:
				case MINECART_CHEST:
				case MINECART_MOB_SPAWNER:
				case MINECART_TNT:
				case BOAT:
					yield EntityTypeLists.parseEntityToMaterial(ev.getVehicle().getType());
				default:
					yield null;
			};

			if (vehicle != null) {
				ev.setCancelled(!BoroughActionExecutor.canBreak((Player)ev.getAttacker(), ev.getVehicle().getLocation(), vehicle));
			}
		} else {
			if (EntityTypeLists.isExplosive(ev.getAttacker().getType()) && !BoroughActionExecutor.canExplosionDamageEntities(ev.getVehicle().getLocation(), ev.getVehicle(), DamageCause.ENTITY_EXPLOSION)) {
				ev.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onVehicleEnter(VehicleEnterEvent ev) {
		if (ev.getEntered() instanceof Player) {
			Material vehicle = switch (ev.getVehicle().getType()) {
				case MINECART:
				case BOAT:
					yield EntityTypeLists.parseEntityToMaterial(ev.getVehicle().getType());
				case HORSE:
				case STRIDER:
				case PIG:
				case DONKEY:
				case MULE:
					yield Material.SADDLE;
				default:
					yield null;
			};

			if (vehicle != null) {
				ev.setCancelled(!BoroughActionExecutor.canInteract((Player)ev.getEntered(), ev.getVehicle().getLocation(), vehicle));
			}
		}
	}
}
