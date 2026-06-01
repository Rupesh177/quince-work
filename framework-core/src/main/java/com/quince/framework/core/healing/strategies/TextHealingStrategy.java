package com.quince.framework.core.healing.strategies;

import com.quince.framework.core.healing.ElementIntent;
import com.quince.framework.core.healing.HealingStrategy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.Optional;

public class TextHealingStrategy implements HealingStrategy {

    @Override
    public Optional<By> heal(By original, WebDriver driver, ElementIntent intent) {
        if (intent == null || isBlank(intent.expectedText())) {
            return Optional.empty();
        }

        String tag = isBlank(intent.tagName()) ? "*" : intent.tagName();

        By healed = By.xpath(
                "//%s[normalize-space()='%s']".formatted(tag, escapeXpath(intent.expectedText()))
        );

        if (isUniqueVisible(driver, healed)) {
            return Optional.of(healed);
        }

        By containsText = By.xpath(
                "//%s[contains(normalize-space(),'%s')]".formatted(tag, escapeXpath(intent.expectedText()))
        );

        if (isUniqueVisible(driver, containsText)) {
            return Optional.of(containsText);
        }

        return Optional.empty();
    }

    @Override
    public String getName() {
        return "TextHealingStrategy";
    }

    private boolean isUniqueVisible(WebDriver driver, By locator) {
        try {
            var elements = driver.findElements(locator);
            return elements.size() == 1 && elements.get(0).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String escapeXpath(String value) {
        return value.replace("'", "\\'");
    }
}
