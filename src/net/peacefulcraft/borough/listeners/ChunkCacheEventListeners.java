package net.peacefulcraft.borough.listeners;

import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import net.peacefulcraft.borough.Borough;
import net.peacefulcraft.borough.storage.BoroughChunk;

public class ChunkCacheEventListeners implements Listener {
	
	@EventHandler
	public void ChunkLoadListener(ChunkLoadEvent ev) {
		Chunk chunk = ev.getChunk();
		String world = chunk.getWorld().getName();
		int x = chunk.getX();
		int z = chunk.getZ();

		BoroughChunk bc = Borough.getClaimStore().getChunk(
			world,
			x,
			z
		);

		Borough.getClaimStore().insertChunkCache(bc);
	}

	@EventHandler
	public void ChunkUnloadListener(ChunkUnloadEvent ev) {
		Chunk chunk = ev.getChunk();

		//TODO: Evict
	}

}
