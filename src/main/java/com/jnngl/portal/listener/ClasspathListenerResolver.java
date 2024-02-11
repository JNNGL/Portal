package com.jnngl.portal.listener;

import java.util.Collection;
import org.bukkit.event.Listener;

public interface ClasspathListenerResolver {

  Collection<Listener> resolve(String base) throws ReflectiveOperationException;
}
