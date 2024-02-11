package com.jnngl.portal.lifecycle;

import com.jnngl.portal.dependency.DependencyInjector;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClassgraphPriorityMethodInvoker<A extends Annotation> implements ReloadMethodInvoker {

  protected record MethodInvocationData(Method method, Object instance) {
  }

  private final DependencyInjector dependencyInjector;
  private final Function<A, Integer> priorityFunction;
  private final Class<A> annotationClass;

  private void scanMethods(PriorityQueue<Map.Entry<MethodInvocationData, Integer>> target, Class<?> cls) {
    for (Method method : cls.getMethods()) {
      A annotation = method.getAnnotation(this.annotationClass);
      if (annotation == null) {
        continue;
      }

      Object instance = null;
      if (!Modifier.isStatic(method.getModifiers())) {
        instance = this.dependencyInjector.get(cls);
        if (instance == null) {
          continue;
        }
      }

      target.offer(new AbstractMap.SimpleImmutableEntry<>(new MethodInvocationData(method, instance), this.priorityFunction.apply(annotation)));
    }
  }

  private void invokeMethods(PriorityQueue<Map.Entry<MethodInvocationData, Integer>> methodQueue) {
    Map.Entry<MethodInvocationData, Integer> entry;
    while ((entry = methodQueue.poll()) != null) {
      this.dependencyInjector.invoke(entry.getKey().method(), entry.getKey().instance());
    }
  }

  @Override
  public void invokeAll(String base) throws ReflectiveOperationException {
    try (ScanResult result = new ClassGraph()
        .acceptPackages(base)
        .enableAnnotationInfo()
        .enableMethodInfo()
        .scan()) {
      PriorityQueue<Map.Entry<MethodInvocationData, Integer>> methodQueue =
          new PriorityQueue<>(Map.Entry.comparingByValue(Comparator.reverseOrder()));
      ClassInfoList classInfos = result.getClassesWithMethodAnnotation(this.annotationClass);
      for (ClassInfo classInfo : classInfos) {
        Class<?> cls = Class.forName(classInfo.getName());
        this.scanMethods(methodQueue, cls);
      }

      this.invokeMethods(methodQueue);
    }
  }
}
