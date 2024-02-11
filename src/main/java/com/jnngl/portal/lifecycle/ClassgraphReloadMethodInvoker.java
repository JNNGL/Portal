package com.jnngl.portal.lifecycle;

import com.jnngl.portal.dependency.DependencyInjector;
import com.jnngl.portal.dependency.annotation.Component;
import com.jnngl.portal.lifecycle.annotation.OnReload;

@Component
public class ClassgraphReloadMethodInvoker extends ClassgraphPriorityMethodInvoker<OnReload> implements ReloadMethodInvoker {

  public ClassgraphReloadMethodInvoker(DependencyInjector dependencyInjector) {
    super(dependencyInjector, OnReload::priority, OnReload.class);
  }
}
