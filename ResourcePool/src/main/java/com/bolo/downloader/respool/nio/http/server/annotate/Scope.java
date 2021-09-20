package com.bolo.downloader.respool.nio.http.server.annotate;

import java.lang.annotation.*;

/**
 * 声明实例的生命周期
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {
    String SCOPE_SINGLETON = "singleton";
    String SCOPE_PROTOTYPE = "prototype";

    String value() default SCOPE_SINGLETON;
}
