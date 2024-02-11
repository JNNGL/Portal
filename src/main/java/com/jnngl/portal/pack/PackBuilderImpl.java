package com.jnngl.portal.pack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jnngl.portal.dependency.annotation.Component;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.codehaus.plexus.util.FileUtils;

@Component(of = {PackBuilder.class, ObservablePackBuilder.class})
public class PackBuilderImpl extends AbstractObservablePackBuilder<PackBuilderImpl> {

  private static final Gson GSON = new Gson();

  private final Map<String, byte[]> rawEntries = new HashMap<>();

  @Override
  public PackBuilderImpl addRawEntry(String path, byte[] data) {
    this.rawEntries.put(path, data);
    this.makeDirty();
    return this;
  }

  @Override
  public PackBuilderImpl withIcon(RenderedImage image) throws IOException {
    this.addImage("pack.png", image);
    return this;
  }

  @Override
  public PackBuilderImpl withPackMeta(PackMeta packMeta) {
    JsonObject json = new JsonObject();
    json.add("pack", GSON.toJsonTree(packMeta));
    this.addText("pack.mcmeta", GSON.toJson(json));
    return this;
  }

  @Override
  public PackBuilderImpl addFile(String path, byte[] data) {
    this.addRawEntry(path, data);
    return this;
  }

  @Override
  public byte[] getFile(String path) {
    return this.rawEntries.get(path);
  }

  @Override
  public PackBuilderImpl addImage(String path, RenderedImage image) throws IOException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      ImageIO.write(image, "PNG", outputStream);
      this.addFile(path, outputStream.toByteArray());
      return this;
    }
  }

  @Override
  public PackBuilderImpl addText(String path, String text) {
    this.addFile(path, text.getBytes(StandardCharsets.UTF_8));
    return this;
  }

  private void mergeJson(JsonObject destination, JsonObject source) {
    for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
      String key = entry.getKey();
      JsonElement value = entry.getValue();
      if (destination.has(key)) {
        JsonElement currentValue = destination.get(key);
        if (value.isJsonArray() && currentValue.isJsonArray()) {
          JsonArray currentArray = currentValue.getAsJsonArray();
          JsonArray arrayToMerge = value.getAsJsonArray();
          currentArray.addAll(arrayToMerge);
        } else if (value.isJsonObject() && currentValue.isJsonObject()) {
          this.mergeJson(currentValue.getAsJsonObject(), value.getAsJsonObject());
        } else {
          destination.add(key, value);
        }
      } else {
        destination.add(key, value);
      }
    }
  }

  @Override
  public PackBuilderImpl mergeJson(String path, String data) {
    byte[] current = this.getFile(path);
    if (current == null) {
      return this.addText(path, data);
    }

    JsonObject currentJson = GSON.fromJson(new String(current, StandardCharsets.UTF_8), JsonObject.class);
    JsonObject toMerge = GSON.fromJson(data, JsonObject.class);

    this.mergeJson(currentJson, toMerge);

    this.addText(path, GSON.toJson(currentJson));
    return this;
  }

  @Override
  public void clear() {
    this.rawEntries.clear();
  }

  @Override
  public Map<String, byte[]> getRawEntries() {
    return this.rawEntries;
  }

  @Override
  protected ResourcePack buildPack() throws IOException {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ZipOutputStream zos = new ZipOutputStream(baos)) {
      Set<String> seenDirectories = new HashSet<>();
      this.rawEntries.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> {
            try {
              String path = entry.getKey();
              String dirname = FileUtils.dirname(path);
              if (seenDirectories.add(dirname)) {
                zos.putNextEntry(new ZipEntry(dirname.endsWith("/") ? dirname : dirname + "/"));
                zos.closeEntry();
              }
              ZipEntry zipEntry = new ZipEntry(path);
              zipEntry.setLastModifiedTime(FileTime.fromMillis(0));
              zos.putNextEntry(zipEntry);
              zos.write(entry.getValue());
              zos.closeEntry();
            } catch (IOException e) {
              throw new RuntimeException("Couldn't add zip entry.", e);
            }
          });

      zos.finish();
      return new ResourcePack(baos.toByteArray());
    }
  }
}
