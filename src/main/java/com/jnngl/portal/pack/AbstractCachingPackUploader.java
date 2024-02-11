package com.jnngl.portal.pack;

import java.io.IOException;

public abstract class AbstractCachingPackUploader implements CachingPackUploader {

  protected UploadedResourcePack cache = null;

  @Override
  public void cache(UploadedResourcePack pack) {
    this.cache = pack;
  }

  @Override
  public UploadedResourcePack getCache() {
    return this.cache;
  }

  public abstract UploadedResourcePack uploadPack(ResourcePack pack) throws IOException;

  @Override
  public UploadedResourcePack upload(ResourcePack pack) throws IOException {
    return this.cache = this.uploadPack(pack);
  }
}
