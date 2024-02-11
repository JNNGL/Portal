package com.jnngl.portal.pack;

import com.google.common.hash.Hashing;
import com.jnngl.portal.CorePlugin;
import com.jnngl.portal.dependency.annotation.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.SneakyThrows;

@Component(of = {PackUploader.class, CachingPackUploader.class})
public class PackUploaderImpl extends AbstractCachingPackUploader {

  private final Path distPath;

  @SneakyThrows
  public PackUploaderImpl(CorePlugin plugin) {
    this.distPath = plugin.getDataFolder().toPath().resolve("pack/dist");
  }

  private PackUploaderImpl(Path distPath) {
    this.distPath = distPath;
  }

  @Override
  public UploadedResourcePack uploadPack(ResourcePack pack) throws IOException {
    @SuppressWarnings("deprecation")
    String hash = Hashing.sha1().hashBytes(pack.data()).toString();
    Files.write(this.distPath.resolve("pack.zip"), pack.data());
    Files.writeString(this.distPath.resolve("pack.zip.sha1"), hash, StandardCharsets.UTF_8);
    return new UploadedResourcePack("http://127.0.0.1:8002/v1/pack/" + hash, hash); // TODO: Configurable host
  }

  public static PackUploaderImpl withDistPath(Path path) {
    return new PackUploaderImpl(path);
  }
}
