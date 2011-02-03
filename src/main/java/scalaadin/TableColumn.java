package scalaadin;


import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
public @interface TableColumn {
	String value();
	String alignment() default com.vaadin.ui.Table.ALIGN_LEFT;
	boolean hiddenByDefault() default false;
	boolean hasFooter() default true;
}

