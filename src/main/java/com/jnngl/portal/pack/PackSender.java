package com.jnngl.portal.pack;

import java.util.Collection;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface PackSender {

  void setDefaultMessage(Component text);

  Component getDefaultMessage();

  void send(Player player, UploadedResourcePack pack, Component message);

  default void send(Player player, UploadedResourcePack pack) {
    this.send(player, pack, this.getDefaultMessage());
  }

  void addTrackingPlayer(Player player);

  void removeTrackingPlayer(Player player);

  Collection<Player> getTrackingPlayers();

  void trackingSend(UploadedResourcePack pack, Component message);

  default void trackingSend(UploadedResourcePack pack) {
    this.trackingSend(pack, this.getDefaultMessage());
  }
}
