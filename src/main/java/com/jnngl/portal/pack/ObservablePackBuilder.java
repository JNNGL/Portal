package com.jnngl.portal.pack;

import java.util.Collection;

public interface ObservablePackBuilder<T extends ObservablePackBuilder<T>> extends PackBuilder<T> {

  void addObserver(PackObserver observer);

  void removeObserver(PackObserver observer);

  Collection<PackObserver> getObservers();

  void notifyObservers(ResourcePack pack);
}
