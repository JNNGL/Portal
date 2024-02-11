package com.jnngl.portal.pack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public abstract class AbstractPackSender implements PackSender {

  protected final List<Player> trackingPlayers;
  protected Component defaultMessage = null;

  public AbstractPackSender(List<Player> tackingPlayers) {
    this.trackingPlayers = tackingPlayers;
  }

  public AbstractPackSender() {
    this(new ArrayList<>());
  }

  @Override
  public void setDefaultMessage(Component component) {
    this.defaultMessage = component;
  }

  @Override
  public Component getDefaultMessage() {
    return this.defaultMessage;
  }

  @Override
  public void addTrackingPlayer(Player player) {
    this.trackingPlayers.add(player);
  }

  @Override
  public void removeTrackingPlayer(Player player) {
    this.trackingPlayers.remove(player);
  }

  @Override
  public Collection<Player> getTrackingPlayers() {
    return this.trackingPlayers;
  }

  @Override
  public void trackingSend(UploadedResourcePack pack, Component message) {
    this.trackingPlayers.forEach(player -> send(player, pack, message));
  }
}
