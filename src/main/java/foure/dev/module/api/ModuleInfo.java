package foure.dev.module.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleInfo {
   String name();
   String desc() default "";
   Category category();
   boolean visual() default false;
   String icon() default ""; // calea spre iconita in assets
}
