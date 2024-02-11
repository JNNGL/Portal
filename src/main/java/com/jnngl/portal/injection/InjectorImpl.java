package com.jnngl.portal.injection;

import com.jnngl.portal.dependency.annotation.Component;
import com.jnngl.portal.lifecycle.annotation.OnDisable;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Server;

@Component
public class InjectorImpl implements Injector {

  private final List<ChannelInjector> injectors = Collections.synchronizedList(new ArrayList<>());
  private final ChannelInjectionHandler injectionHandler = new ChannelInjectionHandler(this.injectors);
  private final Field openChannelsField;
  private final Object connection;
  private List<? extends ChannelFuture> openChannels;

  @SuppressWarnings("unchecked")
  public InjectorImpl() {
    try {
      Server server = Bukkit.getServer();
      Field consoleField = server.getClass().getDeclaredField("console");
      consoleField.setAccessible(true);
      Object minecraftServer = consoleField.get(server);
      Field connectionField = findField(
          minecraftServer.getClass(),
          field -> field.getType().getSimpleName().equals("ServerConnection")
      );
      this.connection = connectionField.get(minecraftServer);

      for (Field field : this.connection.getClass().getDeclaredFields()) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType type)) {
          continue;
        }

        if (type.getRawType() != List.class) {
          continue;
        }

        Type firstParameter = type.getActualTypeArguments()[0];
        if (!firstParameter.getTypeName().endsWith("ChannelFuture")) {
          continue;
        }

        field.setAccessible(true);
        this.openChannelsField = field;
        this.openChannels = (List<? extends ChannelFuture>) field.get(this.connection);
        return;
      }

      throw new InjectionException("Couldn't inject.");
    } catch (ReflectiveOperationException e) {
      throw new InjectionException(e);
    }
  }

  private static Field findField(Class<?> cls, Predicate<Field> predicate) throws NoSuchMethodException {
    for (Field field : cls.getDeclaredFields()) {
      if (predicate.test(field)) {
        field.setAccessible(true);
        return field;
      }
    }

    Class<?> superclass = cls.getSuperclass();
    if (superclass != null) {
      return findField(superclass, predicate);
    } else {
      throw new NoSuchMethodException("in class " + cls.getName());
    }
  }

  @Override
  public void inject() {
    this.ensureNotInjected();
    this.openChannels = new InjectedList(this.openChannels);
    this.openChannels.forEach(channelFuture -> this.inject(channelFuture.channel()));

    try {
      this.openChannelsField.set(this.connection, Collections.synchronizedList(this.openChannels));
    } catch (ReflectiveOperationException e) {
      throw new InjectionException(e);
    }
  }

  private void inject(Channel channel) {
    channel.pipeline().addFirst(this.injectionHandler);
  }

  @Override
  @OnDisable
  public void deject() {
    if (!(this.openChannels instanceof InjectedList)) {
      throw new InjectionException("Not injected.");
    }

    this.openChannels = new ArrayList<>(this.openChannels);
    this.injectionHandler.deject();

    try {
      this.openChannelsField.set(this.connection, Collections.synchronizedList(this.openChannels));
    } catch (ReflectiveOperationException e) {
      throw new InjectionException(e);
    }
  }

  @Override
  public void addInjector(ChannelInjector injector) {
    this.ensureNotInjected();
    this.injectors.add(injector);
  }

  @Override
  public void removeInjector(ChannelInjector injector) {
    this.ensureNotInjected();
    this.injectors.remove(injector);
  }

  @Override
  public List<ChannelInjector> getInjectors() {
    return this.injectors;
  }

  @Override
  public List<? extends ChannelFuture> getOpenChannels() {
    return this.openChannels;
  }

  private void ensureNotInjected() {
    if (this.openChannels instanceof InjectedList) {
      throw new InjectionException("Already injected.");
    }
  }

  private final class InjectedList extends ArrayList<ChannelFuture> {

    private InjectedList(List<? extends ChannelFuture> originalList) {
      super(originalList);
    }

    @Override
    public boolean add(ChannelFuture channelFuture) {
      InjectorImpl.this.inject(channelFuture.channel());
      return super.add(channelFuture);
    }

    @Override
    public void add(int index, ChannelFuture element) {
      InjectorImpl.this.inject(element.channel());
      super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends ChannelFuture> c) {
      c.forEach(channelFuture -> InjectorImpl.this.inject(channelFuture.channel()));
      return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends ChannelFuture> c) {
      c.forEach(channelFuture -> InjectorImpl.this.inject(channelFuture.channel()));
      return super.addAll(index, c);
    }
  }
}
