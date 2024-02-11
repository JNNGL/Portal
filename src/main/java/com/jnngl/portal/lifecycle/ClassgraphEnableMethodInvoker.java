package com.jnngl.portal.lifecycle;

import com.jnngl.portal.dependency.DependencyInjector;
import com.jnngl.portal.dependency.annotation.Component;
import com.jnngl.portal.lifecycle.annotation.OnEnable;

@Component
public class ClassgraphEnableMethodInvoker extends ClassgraphPriorityMethodInvoker<OnEnable> implements EnableMethodInvoker {

  public ClassgraphEnableMethodInvoker(DependencyInjector dependencyInjector) {
    super(dependencyInjector, OnEnable::priority, OnEnable.class);
  }
}
