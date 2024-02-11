package com.jnngl.portal.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NettyInject {

  String name();

  PipelinePosition position() default PipelinePosition.BEFORE;

  String parent();

  boolean shared() default false;
}
