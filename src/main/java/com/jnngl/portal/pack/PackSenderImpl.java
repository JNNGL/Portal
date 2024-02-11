package com.jnngl.portal.pack;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@com.jnngl.portal.dependency.annotation.Component(of = {PackSender.class})
public class PackSenderImpl extends AbstractPackSender {

  public void send(Player player, UploadedResourcePack pack, Component message) {
    if (message != null) {
      player.setResourcePack(pack.url(), pack.hash(), true, message);
    } else {
      player.setResourcePack(pack.url(), pack.hash(), true);
    }
  }
}
