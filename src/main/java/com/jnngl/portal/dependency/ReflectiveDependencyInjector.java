package com.jnngl.portal.dependency;

import com.jnngl.portal.dependency.annotation.Optional;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectiveDependencyInjector implements DependencyInjector {

  private final Map<Class<?>, Object> bindings = new ConcurrentHashMap<>();

  private <T> Constructor<?> findConstructor(Class<T> clazz) throws ReflectiveOperationException {
    Constructor<?>[] constructors = clazz.getConstructors();
    for (Constructor<?> constructor : constructors) {
      boolean suitable = true;
      AnnotatedType[] types = constructor.getAnnotatedParameterTypes();
      for (AnnotatedType parameter : types) {
        if (parameter.isAnnotationPresent(Optional.class)) {
          continue;
        }

        Class<?> cls = null;
        Type type = parameter.getType();
        if (parameter.getType() instanceof ParameterizedType parameterizedType) {
          type = parameterizedType.getRawType();
        }

        if (type instanceof Class<?> typeClass) {
          cls = typeClass;
        }

        if (cls == null || !this.bindings.containsKey(cls)) {
          suitable = false;
          break;
        }
      }

      if (suitable) {
        return constructor;
      }
    }

    return null;
  }

  @Override
  public <T> void bind(Class<T> clazz, T k) {
    this.bindings.put(clazz, k);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T instantiate(Class<T> clazz) {
    try {
      Constructor<?> constructor = this.findConstructor(clazz);
      if (constructor == null) {
        return null;
      }

      Object[] parameters = new Object[constructor.getParameterCount()];
      var parameterTypes = constructor.getParameterTypes();
      for (int i = 0; i < parameters.length; i++) {
        parameters[i] = this.bindings.get(parameterTypes[i]);
      }

      return (T) constructor.newInstance(parameters);
    } catch (ReflectiveOperationException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(Class<T> clazz) {
    return (T) this.bindings.get(clazz);
  }

  @Override
  public <T> T require(Class<T> clazz) {
    if (this.bindings.containsKey(clazz)) {
      return this.get(clazz);
    } else {
      return this.instantiate(clazz);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T compute(Class<T> clazz) {
    return (T) this.bindings.computeIfAbsent(clazz, k -> this.instantiate(clazz));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T invoke(Method method, Object instance) {
    try {
      Object[] parameters = new Object[method.getParameterCount()];
      var parameterTypes = method.getParameterTypes();
      var annotatedTypes = method.getAnnotatedParameterTypes();
      for (int i = 0; i < parameters.length; i++) {
        if ((parameters[i] = this.bindings.get(parameterTypes[i])) == null
            && !annotatedTypes[i].isAnnotationPresent(Optional.class)) {
          throw new IllegalStateException("No binding for class " + parameterTypes[i] + " for method " + method + " argument [" + i + "] ");
        }
      }

      return (T) method.invoke(instance, parameters);
    } catch (ReflectiveOperationException e) {
      e.printStackTrace();
      return null;
    }
  }
}
