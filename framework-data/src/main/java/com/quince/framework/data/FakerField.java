package com.quince.framework.data;

import java.lang.annotation.*;

/**
 * Annotation for marking fields to be populated with Faker data.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FakerField {
    /**
     * Faker type: name, email, phone, address, zipcode, uuid, text, number
     */
    String value();
}
