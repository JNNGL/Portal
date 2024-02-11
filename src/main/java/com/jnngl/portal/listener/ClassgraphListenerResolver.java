package com.jnngl.portal.listener;

import com.jnngl.portal.dependency.DependencyInjector;
import com.jnngl.portal.dependency.annotation.Component;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Listener;

@Component
@RequiredArgsConstructor
public class ClassgraphListenerResolver implements ClasspathListenerResolver {

  private final DependencyInjector dependencyInjector;

  @Override
  @SuppressWarnings("unchecked")
  public Collection<Listener> resolve(String base) throws ReflectiveOperationException {
    try (ScanResult result = new ClassGraph()
        .acceptPackages(base)
        .enableAnnotationInfo()
        .enableClassInfo()
        .scan()) {
      ClassInfoList listeners = result
          .getClassesWithAnnotation(InjectListener.class.getName())
          .filter(info -> !info.isAbstract());

      List<Listener> resolved = new ArrayList<>();
      for (ClassInfo classInfo : listeners) {
        var listenerClass = (Class<?>) Class.forName(classInfo.getName());
        if (!Listener.class.isAssignableFrom(listenerClass)) {
          throw new IllegalStateException("Class " + classInfo.getName() + " is not implementing Bukkit Listener but annotated with @InjectListener");
        }

        Listener listener = this.dependencyInjector.compute((Class<? extends Listener>) listenerClass);
        resolved.add(listener);
      }

      return resolved;
    }
  }
}
