package com.jnngl.portal.dependency;

import java.util.Map;

public interface ComponentDependencyResolver {

  Map<Class<?>, Object> resolve(String base) throws ReflectiveOperationException;
}
