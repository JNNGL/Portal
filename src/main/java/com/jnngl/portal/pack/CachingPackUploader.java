package com.jnngl.portal.pack;

public interface CachingPackUploader extends PackUploader {

  void cache(UploadedResourcePack pack);

  UploadedResourcePack getCache();
}
