package com.quince.framework.core.healing.strategies;


import com.quince.framework.core.healing.ElementIntent;
import com.quince.framework.core.healing.HealingStrategy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.Map;
import java.util.Optional;

public class XPathHealingStrategy implements HealingStrategy {

    @Override
    public Optional<By> heal(By original, WebDriver driver, ElementIntent intent) {
        if (intent == null) {
            return Optional.empty();
        }

        String tag = isBlank(intent.tagName()) ? "*" : intent.tagName();

        Optional<By> byRoleAndText = byRoleAndText(driver, tag, intent);
        if (byRoleAndText.isPresent()) {
            return byRoleAndText;
        }

        Optional<By> byAttributesAndText = byAttributesAndText(driver, tag, intent);
        if (byAttributesAndText.isPresent()) {
            return byAttributesAndText;
        }

        return Optional.empty();
    }

    @Override
    public String getName() {
        return "XPathHealingStrategy";
    }

    private Optional<By> byRoleAndText(WebDriver driver, String tag, ElementIntent intent) {
        if (isBlank(intent.role()) || isBlank(intent.expectedText())) {
            return Optional.empty();
        }

        By locator = By.xpath(
                "//%s[@role='%s' and contains(normalize-space(),'%s')]"
                        .formatted(tag, escapeXpath(intent.role()), escapeXpath(intent.expectedText()))
        );

        return isUniqueVisible(driver, locator) ? Optional.of(locator) : Optional.empty();
    }

    private Optional<By> byAttributesAndText(WebDriver driver, String tag, ElementIntent intent) {
        if (intent.stableAttributes() == null || intent.stableAttributes().isEmpty()) {
            return Optional.empty();
        }

        for (Map.Entry<String, String> entry : intent.stableAttributes().entrySet()) {
            if (isBlank(entry.getKey()) || isBlank(entry.getValue()) || isBlank(intent.expectedText())) {
                continue;
            }

            By locator = By.xpath(
                    "//%s[@%s='%s' and contains(normalize-space(),'%s')]"
                            .formatted(
                                    tag,
                                    entry.getKey(),
                                    escapeXpath(entry.getValue()),
                                    escapeXpath(intent.expectedText())
                            )
            );

            if (isUniqueVisible(driver, locator)) {
                return Optional.of(locator);
            }
        }

        return Optional.empty();
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
