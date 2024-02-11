package com.jnngl.portal.injection;

import com.jnngl.portal.listener.InjectListener;
import io.netty.channel.Channel;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@InjectListener
public class PassengerRewriterHandler implements Listener {

  private final Map<Player, PassengerRewriter> passengerRewriters = new HashMap<>();

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerJoin(PlayerJoinEvent event) {
    PassengerRewriter rewriter = new PassengerRewriter();
    Channel channel = ((CraftPlayer) event.getPlayer()).getHandle().connection.connection.channel;
    channel.pipeline().addBefore("packet_handler", "passenger_rewriter", rewriter);
    this.passengerRewriters.put(event.getPlayer(), rewriter);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerQuit(PlayerQuitEvent event) {
    this.passengerRewriters.remove(event.getPlayer());
  }

  public PassengerRewriter getRewriter(Player player) {
    return this.passengerRewriters.get(player);
  }
}
