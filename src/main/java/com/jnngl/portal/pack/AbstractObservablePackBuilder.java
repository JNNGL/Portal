package com.jnngl.portal.pack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractObservablePackBuilder<T extends AbstractObservablePackBuilder<T>>
    extends AbstractPackBuilder<T> implements ObservablePackBuilder<T> {

  private final List<PackObserver> observers;

  public AbstractObservablePackBuilder(List<PackObserver> observers) {
    this.observers = observers;
  }

  public AbstractObservablePackBuilder() {
    this(new ArrayList<>());
  }

  @Override
  public void addObserver(PackObserver observer) {
    this.observers.add(observer);
  }

  @Override
  public void removeObserver(PackObserver observer) {
    this.observers.remove(observer);
  }

  @Override
  public Collection<PackObserver> getObservers() {
    return this.observers;
  }

  @Override
  public void notifyObservers(ResourcePack pack) {
    this.observers.forEach(observer -> observer.updatePack(pack));
  }

  @Override
  public ResourcePack build() throws IOException {
    boolean shouldNotify = this.isDirty();
    ResourcePack pack = super.build();
    if (shouldNotify) {
      this.notifyObservers(pack);
    }
    return pack;
  }
}
