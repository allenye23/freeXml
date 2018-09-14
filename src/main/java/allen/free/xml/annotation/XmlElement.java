package allen.free.xml.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME) @Target({FIELD, METHOD, PARAMETER})
public @interface XmlElement {

  String name();

  boolean ignoreWhenNullOrEmpty() default true;

  boolean selfClosing() default true;
}
