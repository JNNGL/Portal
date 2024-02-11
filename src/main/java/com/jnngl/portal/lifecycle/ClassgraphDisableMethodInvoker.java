package com.jnngl.portal.lifecycle;

import com.jnngl.portal.dependency.DependencyInjector;
import com.jnngl.portal.dependency.annotation.Component;
import com.jnngl.portal.lifecycle.annotation.OnDisable;

@Component
public class ClassgraphDisableMethodInvoker extends ClassgraphPriorityMethodInvoker<OnDisable> implements DisableMethodInvoker {

  public ClassgraphDisableMethodInvoker(DependencyInjector dependencyInjector) {
    super(dependencyInjector, OnDisable::priority, OnDisable.class);
  }
}
