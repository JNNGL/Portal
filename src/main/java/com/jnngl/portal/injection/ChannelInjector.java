package com.jnngl.portal.injection;

import io.netty.channel.Channel;

public interface ChannelInjector {

  void inject(Channel channel);

  void deject(Channel channel);
}
