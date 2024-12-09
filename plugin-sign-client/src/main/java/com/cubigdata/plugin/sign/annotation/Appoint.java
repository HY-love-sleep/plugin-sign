package com.cubigdata.plugin.sign.annotation;

import java.lang.annotation.*;

@Inherited
@Documented
@Target({ElementType.METHOD,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Appoint {
}
