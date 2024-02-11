package com.jnngl.portal.injection;

import com.google.common.collect.ImmutableMap;
import com.jnngl.portal.dependency.DependencyInjector;
import com.jnngl.portal.dependency.annotation.Component;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClassgraphNettyInjectorResolver implements NettyInjectorResolver {

  private final DependencyInjector dependencyInjector;

  @Override
  @SuppressWarnings("unchecked")
  public ChannelInjector resolve(String base) throws ReflectiveOperationException {
    try (ScanResult result = new ClassGraph()
        .enableAnnotationInfo()
        .enableClassInfo()
        .scan()) {
      ClassInfoList injectors = result
          .getClassesWithAnnotation(NettyInject.class.getName())
          .filter(info -> info.getName().startsWith(base))
          .filter(info -> !info.isAbstract());

      Map<Object, NettyInject> resolved = new HashMap<>();
      for (ClassInfo classInfo : injectors) {
        var injectorClass = (Class<?>) Class.forName(classInfo.getName());
        if (!ChannelHandler.class.isAssignableFrom(injectorClass)) {
          throw new IllegalStateException("Class " + classInfo.getName() + " is not implementing ChannelHandler but annotated with @NettyInject");
        }

        NettyInject annotation = injectorClass.getAnnotation(NettyInject.class);
        if (annotation.shared()) {
          ChannelHandler handler = this.dependencyInjector.require((Class<? extends ChannelHandler>) injectorClass);
          resolved.put(handler, annotation);
        } else {
          resolved.put(injectorClass, annotation);
        }
      }

      ImmutableMap<Object, NettyInject> immutableResolved = ImmutableMap.copyOf(resolved);
      return new ChannelInjector() {
        @Override
        public void inject(Channel channel) {
          immutableResolved.forEach((injector, annotation) -> {
            ChannelHandler handler = null;
            if (injector instanceof ChannelHandler injectHandler) {
              handler = injectHandler;
            } else if (injector instanceof Class<?> injectorClass) {
              handler = ClassgraphNettyInjectorResolver.this
                  .dependencyInjector.instantiate((Class<? extends ChannelHandler>) injectorClass);
            }

            if (handler != null) {
              switch (annotation.position()) {
                case BEFORE -> channel.pipeline().addBefore(annotation.parent(), annotation.name(), handler);
                case AFTER -> channel.pipeline().addAfter(annotation.parent(), annotation.name(), handler);
                default -> throw new IllegalStateException();
              }
            }
          });
        }

        @Override
        public void deject(Channel channel) {
          immutableResolved.values().forEach(annotation -> {
            ChannelHandler channelHandler = channel.pipeline().get(annotation.name());
            if (channelHandler != null) {
              channel.pipeline().remove(channelHandler);
            }
          });
        }
      };
    }
  }
}
