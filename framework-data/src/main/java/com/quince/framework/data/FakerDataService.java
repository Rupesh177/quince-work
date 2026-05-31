package com.quince.framework.data;

import com.github.javafaker.Faker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.lang.reflect.Field;
import java.util.Locale;

/**
 * DataService implementation using Java Faker for random data generation.
 */
public class FakerDataService implements DataService {
    private static final Logger logger = LogManager.getLogger(FakerDataService.class);
    
    private final Faker faker = new Faker(new Locale("en-US"));

    @Override
    public <T> T create(Class<T> type) {
        logger.debug("Creating fake data for: {}", type.getSimpleName());
        
        try {
            T instance = type.getDeclaredConstructor().newInstance();
            
            // Populate annotated fields
            for (Field field : type.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(FakerField.class)) {
                    FakerField annotation = field.getAnnotation(FakerField.class);
                    Object value = generateValue(field.getType(), annotation.value());
                    field.set(instance, value);
                }
            }
            
            logger.info("Created fake {} instance", type.getSimpleName());
            return instance;
        } catch (Exception e) {
            logger.error("Error creating fake data", e);
            throw new RuntimeException("Fake data creation failed", e);
        }
    }

    @Override
    public void cleanup(String id) {
        logger.debug("Cleanup requested for: {}", id);
        // Faker doesn't create persistent data, so cleanup is no-op
    }

    /**
     * Generates value based on faker type.
     */
    private Object generateValue(Class<?> fieldType, String fakerType) {
        return switch (fakerType) {
            case "name" -> faker.name().fullName();
            case "email" -> faker.internet().emailAddress();
            case "phone" -> faker.phoneNumber().cellPhone();
            case "address" -> faker.address().fullAddress();
            case "zipcode" -> faker.address().zipCode();
            case "uuid" -> faker.idNumber().valid();
            case "text" -> faker.lorem().sentence();
            case "number" -> faker.number().numberBetween(1, 1000);
            default -> "default_" + System.currentTimeMillis();
        };
    }
}
