package com.jnngl.portal.injection;

public interface NettyInjectorResolver {

  ChannelInjector resolve(String base) throws ReflectiveOperationException;
}
