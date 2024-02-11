package com.jnngl.portal.pack;

import com.jnngl.portal.CorePlugin;
import com.jnngl.portal.dependency.annotation.Component;
import com.jnngl.portal.lifecycle.annotation.OnEnable;
import com.jnngl.portal.lifecycle.annotation.OnReload;
import com.jnngl.portal.listener.InjectListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Component
@InjectListener
@RequiredArgsConstructor
public class ResourcePackListener implements Listener {

  private final ObservablePackBuilder<? extends ObservablePackBuilder<?>> packBuilder;
  private final CachingPackUploader packUploader;
  private final PackSender packSender;
  private final Logger logger;

  @OnEnable
  public void initialize() {
    this.packBuilder.addObserver(this::updatePack);
  }

  @OnReload(priority = 300)
  public void clearPack() {
    this.packBuilder.clear();
  }

  @OnReload(priority = 200)
  public void loadPack(CorePlugin plugin) {
    this.packBuilder.withPackMeta(new PackMeta(15, "Portal Pack"));
    try {
      Path staticPackPath = plugin.getDataFolder().toPath().resolve("pack/static");
      Files.createDirectories(staticPackPath);
      try (Stream<Path> stream = Files.walk(staticPackPath)) {
        stream.filter(Files::isRegularFile)
            .forEach(path -> {
              try {
                byte[] data = Files.readAllBytes(path);
                String zipPath = staticPackPath.relativize(path).toString();
                this.packBuilder.addFile(zipPath, data);
              } catch (IOException e) {
                throw new RuntimeException("Couldn't read static pack file.", e);
              }
            });
      }
    } catch (IOException e) {
      throw new RuntimeException("Couldn't load static pack files.", e);
    }
  }

  @OnReload(priority = 0)
  @SneakyThrows
  public void build() {
    this.packBuilder.build();
  }

  public void sendPack(Player player) throws IOException {
    UploadedResourcePack cache = this.packUploader.getCache();
    if (cache != null) {
      this.packSender.send(player, cache);
    } else {
      this.forceRebuildPack();
    }
  }

  public void forceUpdatePack() throws IOException {
    boolean needUpdate = !this.packBuilder.isDirty();
    ResourcePack pack = this.packBuilder.build();
    if (needUpdate) {
      this.updatePack(pack);
    }
  }

  public void forceRebuildPack() throws IOException {
    this.packBuilder.makeDirty();
    this.updatePack();
  }

  private void updatePack(ResourcePack pack) {
    try {
      UploadedResourcePack uploaded = this.packUploader.upload(pack);
      this.logger.info("Uploaded pack to " + uploaded.url());
      this.packSender.trackingSend(uploaded);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't apply resource pack", e);
    }
  }

  public void updatePack() throws IOException {
    this.packBuilder.build();
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    try {
      this.packSender.addTrackingPlayer(player);
      this.sendPack(player);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    this.packSender.removeTrackingPlayer(event.getPlayer());
  }
}
