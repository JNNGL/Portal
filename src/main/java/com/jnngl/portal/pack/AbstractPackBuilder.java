package com.jnngl.portal.pack;

import java.io.IOException;

public abstract class AbstractPackBuilder<T extends AbstractPackBuilder<T>> implements PackBuilder<T> {

  protected ResourcePack cache = null;

  @Override
  public boolean isDirty() {
    return this.cache == null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T makeDirty() {
    this.cache = null;
    return (T) this;
  }

  protected abstract ResourcePack buildPack() throws IOException;

  @Override
  public ResourcePack build() throws IOException {
    if (this.cache == null) {
      this.cache = this.buildPack();
    }

    return this.cache;
  }
}
