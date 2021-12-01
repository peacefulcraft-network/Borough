package net.peacefulcraft.templateus.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.peacefulcraft.templateus.Templateus;

public class PlayerJoinListener implements Listener {
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent ev) {
    ev.getPlayer().sendMessage(Templateus.messagingPrefix + "Welcome to the server! -Templateus");
  }
}