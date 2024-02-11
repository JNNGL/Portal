package com.jnngl.portal.dependency;

import java.lang.reflect.Method;

public interface DependencyInjector {

  <T> void bind(Class<T> clazz, T t);

  <T> T instantiate(Class<T> clazz);

  <T> T get(Class<T> clazz);

  <T> T require(Class<T> clazz);

  <T> T compute(Class<T> clazz);

  <T> T invoke(Method method, Object instance);

  default <T> T invoke(Method method) {
    return this.invoke(method, null);
  }
}
