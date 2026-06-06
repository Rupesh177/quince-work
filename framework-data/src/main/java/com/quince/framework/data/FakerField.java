package com.quince.framework.data;

import java.lang.annotation.*;

/**
 * Annotation for marking fields to be populated with Faker data.
 */
@Retention(RetentionPolicy.RUNTIME) //Keeps annotation available at runtime for Reflection
@Target(ElementType.FIELD) //Only allowed on class fields
public @interface FakerField {
    /**
     * Faker type: name, email, phone, address, zipcode, uuid, text, number
     */
    String value();
}
