package com.quince.framework.core.healing;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.Optional;

/**
 * Interface for locator healing strategies.
 * Used when a locator fails to find an element.
 */
public interface HealingStrategy {

    /**
     * Attempts to heal a failed locator.
     *
     * @param original    the original By locator that failed
     * @param driver      the WebDriver instance
     * @paramelementName  human-readable element name for logging
     * @return healed locator if successful
     */
    Optional<By> heal(By original,
                      WebDriver driver,
                      ElementIntent intent);

    String getName();
}