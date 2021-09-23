package com.bolo.downloader.respool.nio.http.controller.annotate;


import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
    /**
     * value 别名
     */

    String[] path() default {};

    /**
     * path 别名
     */
    String[] value() default {};

    RequestMethod[] method() default {};
}
