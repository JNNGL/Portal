package com.jnngl.portal;

import com.jnngl.portal.dependency.ClassgraphComponentDependencyResolver;
import com.jnngl.portal.dependency.DependencyInjector;
import com.jnngl.portal.dependency.ReflectiveDependencyInjector;
import com.jnngl.portal.injection.PassengerRewriter;
import com.jnngl.portal.injection.PassengerRewriterHandler;
import com.jnngl.portal.lifecycle.ClassgraphEnableMethodInvoker;
import com.jnngl.portal.lifecycle.ClassgraphReloadMethodInvoker;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CorePlugin extends JavaPlugin {

  private static final String LOOKUP_PACKAGE = CorePlugin.class.getPackageName();

  private static final AtomicReference<CorePlugin> PLUGIN_REFERENCE = new AtomicReference<>();

  public static CorePlugin getInstance() {
    return PLUGIN_REFERENCE.get();
  }

  @Getter
  private final DependencyInjector dependencyInjector = new ReflectiveDependencyInjector();

  @Override
  @SneakyThrows
  public void onEnable() {
    PLUGIN_REFERENCE.set(this);

    this.dependencyInjector.bind(CorePlugin.class, this);
    this.dependencyInjector.bind(Logger.class, getLogger());
    new ClassgraphComponentDependencyResolver(this.dependencyInjector).resolve(LOOKUP_PACKAGE);

    this.dependencyInjector.get(ClassgraphEnableMethodInvoker.class).invokeAll(LOOKUP_PACKAGE);
    this.triggerReload();
  }

  @SneakyThrows
  public void triggerReload() {
    this.dependencyInjector.get(ClassgraphReloadMethodInvoker.class).invokeAll(LOOKUP_PACKAGE);
  }

  public <T> T get(Class<T> cls) {
    return this.dependencyInjector.get(cls);
  }

  public PassengerRewriter getPassengerRewriter(Player player) {
    return this.get(PassengerRewriterHandler.class).getRewriter(player);
  }
}
