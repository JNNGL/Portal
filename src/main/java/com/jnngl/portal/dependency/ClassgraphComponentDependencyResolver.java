package com.jnngl.portal.dependency;

import com.jnngl.portal.dependency.annotation.Component;
import com.jnngl.portal.dependency.annotation.Optional;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClassgraphComponentDependencyResolver implements ComponentDependencyResolver {

  private final DependencyInjector injector;

  private record Dependency(Class<?> cls) {
  }

  private Class<?> checkCircularDependency(Map<Class<?>, List<Dependency>> dependencyMap, Class<?> cls, Class<?> checked) {
    List<Dependency> dependencies = dependencyMap.get(cls);
    if (dependencies == null) {
      return null;
    }

    if (dependencies.isEmpty()) {
      return null;
    }

    for (Dependency dependency : dependencies) {
      if (dependency.cls().equals(checked)) {
        return dependency.cls();
      }

      Class<?> recursiveCheck = this.checkCircularDependency(dependencyMap, dependency.cls(), checked);
      if (recursiveCheck != null) {
        return recursiveCheck;
      }
    }

    return null;
  }

  private Class<?> checkCircularDependency(Map<Class<?>, List<Dependency>> dependencyMap, Class<?> cls) {
    return this.checkCircularDependency(dependencyMap, cls, cls);
  }

  private Map<Class<?>, Class<?>> checkCircularDependencies(Map<Class<?>, List<Dependency>> dependencyMap) {
    Map<Class<?>, Class<?>> circularDependencies = new HashMap<>();
    dependencyMap.keySet().forEach(cls -> {
      Class<?> circularDependency = this.checkCircularDependency(dependencyMap, cls);
      if (circularDependency != null) {
        if (cls.equals(circularDependencies.get(circularDependency))) {
          return;
        }

        circularDependencies.put(cls, circularDependency);
      }
    });

    return circularDependencies;
  }

  private Class<?>[] resolveBindTargets(Class<?> cls) {
    Component component = cls.getAnnotation(Component.class);
    if (component != null && component.of().length != 0) {
      return component.of();
    } else {
      return cls.getInterfaces();
    }
  }

  private void buildBindings(Map<Class<?>, Class<?>> target, ClassInfoList components) throws ReflectiveOperationException {
    for (ClassInfo classInfo : components) {
      Class<?> componentClass = Class.forName(classInfo.getName());
      target.put(componentClass, componentClass);
      for (Class<?> bindingTarget : this.resolveBindTargets(componentClass)) {
        target.put(bindingTarget, componentClass);
      }
    }
  }

  private void buildDependencyMap(Map<Class<?>, List<Dependency>> target, Map<Class<?>, Class<?>> bindings,
                                  ClassInfoList components) throws ReflectiveOperationException {
    for (ClassInfo classInfo : components) {
      Class<?> componentClass = Class.forName(classInfo.getName());
      List<Dependency> dependencies = null;
      for (Constructor<?> constructor : componentClass.getConstructors()) {
        List<Dependency> current = new ArrayList<>();
        for (AnnotatedType parameter : constructor.getAnnotatedParameterTypes()) {
          Class<?> cls = null;
          Type type = parameter.getType();
          if (parameter.getType() instanceof ParameterizedType parameterizedType) {
            type = parameterizedType.getRawType();
          }

          if (type instanceof Class<?> typeClass) {
            cls = typeClass;
          }

          if (cls == null) {
            continue;
          }

          if (cls.isPrimitive()) {
            current = null;
            break;
          }

          cls = bindings.getOrDefault(cls, cls);
          if (!parameter.isAnnotationPresent(Optional.class) && cls.isAnnotationPresent(Component.class)) {
            current.add(new Dependency(cls));
          }
        }

        if (current != null && (dependencies == null || current.size() < dependencies.size())) {
          dependencies = current;
        }
      }

      if (dependencies == null) {
        dependencies = new ArrayList<>();
      }

      target.put(componentClass, dependencies);
    }
  }

  @SuppressWarnings("unchecked")
  private void resolve(Map<Class<?>, Object> target, Map<Class<?>, List<Dependency>> dependencyMap, Class<?> cls) {
    List<Dependency> dependencies = dependencyMap.get(cls);
    if (dependencies != null) {
      for (Dependency dependency : dependencies) {
        this.resolve(target, dependencyMap, dependency.cls());
      }
    }

    target.computeIfAbsent(cls, k -> {
      Object instance = this.injector.instantiate(cls);
      this.injector.bind((Class<Object>) cls, instance);
      for (Class<?> bindTarget : this.resolveBindTargets(cls)) {
        this.injector.bind((Class<Object>) bindTarget, instance);
      }
      return instance;
    });
  }

  @Override
  public Map<Class<?>, Object> resolve(String base) throws ReflectiveOperationException {
    try (ScanResult result = new ClassGraph()
        .acceptPackages(base)
        .enableAnnotationInfo()
        .enableClassInfo()
        .scan()) {
      ClassInfoList components = result
          .getClassesWithAnnotation(Component.class.getName())
          .filter(info -> !info.isAbstract());

      this.injector.bind(DependencyInjector.class, this.injector);
      this.injector.bind(ComponentDependencyResolver.class, this);

      Map<Class<?>, Object> resolved = new HashMap<>();
      Map<Class<?>, Class<?>> bindings = new HashMap<>();
      Map<Class<?>, List<Dependency>> dependencyMap = new HashMap<>();

      this.buildBindings(bindings, components);
      this.buildDependencyMap(dependencyMap, bindings, components);

      Map<Class<?>, Class<?>> circularDependencies = this.checkCircularDependencies(dependencyMap);
      if (!circularDependencies.isEmpty()) {
        throw new IllegalStateException("Found circular dependencies: " + circularDependencies);
      }

      for (Class<?> cls : dependencyMap.keySet()) {
        this.resolve(resolved, dependencyMap, cls);
      }

      return resolved;
    }
  }
}
