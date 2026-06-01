package com.quince.framework.core.healing.strategies;

import com.quince.framework.core.healing.ElementIntent;
import com.quince.framework.core.healing.HealingStrategy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.Map;
import java.util.Optional;

public class AttributeHealingStrategy implements HealingStrategy {

    @Override
    public Optional<By> heal(By original, WebDriver driver, ElementIntent intent) {
        if (intent == null || intent.stableAttributes() == null || intent.stableAttributes().isEmpty()) {
            return Optional.empty();
        }

        for (Map.Entry<String, String> entry : intent.stableAttributes().entrySet()) {
            String attr = entry.getKey();
            String value = entry.getValue();

            if (isBlank(attr) || isBlank(value)) {
                continue;
            }

            By healed = By.xpath("//*[@%s='%s']".formatted(attr, escapeXpath(value)));

            if (isUniqueVisible(driver, healed)) {
                return Optional.of(healed);
            }
        }

        return Optional.empty();
    }

    @Override
    public String getName() {
        return "AttributeHealingStrategy";
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