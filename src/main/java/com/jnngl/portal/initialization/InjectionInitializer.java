package com.jnngl.portal.initialization;

import com.jnngl.portal.CorePlugin;
import com.jnngl.portal.injection.Injector;
import com.jnngl.portal.injection.NettyInjectorResolver;
import com.jnngl.portal.lifecycle.annotation.OnEnable;
import com.jnngl.portal.listener.ClasspathListenerResolver;
import java.util.logging.Logger;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;

public class InjectionInitializer {

  @OnEnable
  @SneakyThrows
  public static void injectListeners(ClasspathListenerResolver listenerResolver, CorePlugin plugin) {
    listenerResolver.resolve("com.jnngl")
        .forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, plugin));
  }

  @OnEnable
  @SneakyThrows
  public static void injectNetty(Logger logger, Injector injector, NettyInjectorResolver resolver) {
    injector.addInjector(resolver.resolve("com.jnngl"));
    injector.inject();

    logger.info("Successfully injected.");
  }
}
