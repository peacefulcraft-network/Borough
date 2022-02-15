package net.peacefulcraft.borough.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;

import net.peacefulcraft.borough.Borough;
import net.peacefulcraft.borough.event.executor.BoroughActionExecutor;
import net.peacefulcraft.borough.storage.BoroughChunk;

public class WorldListeners implements Listener {
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onStructureGrow(StructureGrowEvent ev) {
		Player p = ev.getPlayer();
		if (p == null) { return; }

		List<BlockState> disallowed = new ArrayList<>();
		ev.getBlocks().forEach((state) -> {
			BoroughChunk chunk = Borough.getClaimStore().getChunk(state.getLocation());
			if (chunk != null) {
				if (!chunk.canUserBuild(p.getUniqueId())) { disallowed.add(state); }
			}
		});

		if (!disallowed.isEmpty()) {
			ev.getBlocks().removeAll(disallowed);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPortalCreate(PortalCreateEvent ev) {
		if (!(ev.getReason() == CreateReason.NETHER_PAIR) || !(ev.getEntity() instanceof Player)) { return; }

		for (BlockState state : ev.getBlocks()) {
			if (!BoroughActionExecutor.canBuild((Player) ev.getEntity(), state.getLocation(), Material.NETHER_PORTAL)) {
				ev.setCancelled(true);
				break;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent ev) {
		if (ev.getLocation() == null || ev.getSpawnReason() == null) { return; }
		// We want to allow peaceful breeding, beehives, etc.
		// Do not allow the selection between basic spawning blocking between peaceful/hostile
		ev.setCancelled(!BoroughActionExecutor.canSpawn(ev.getLocation(), ev.getSpawnReason()));
	}

}
