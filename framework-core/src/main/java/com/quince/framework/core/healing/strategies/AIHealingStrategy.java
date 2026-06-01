package com.quince.framework.core.healing.strategies;

import com.quince.framework.core.ai.AIHealingProvider;
import com.quince.framework.core.ai.OpenAIHealingProvider;
import com.quince.framework.core.healing.ElementIntent;
import com.quince.framework.core.healing.HealingStrategy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Optional;

public class AIHealingStrategy
        implements HealingStrategy {

    private final AIHealingProvider provider;


    public AIHealingStrategy() {
        this(
                new OpenAIHealingProvider(
                        System.getenv("OPENAI_API_KEY")
                )
        );
    }

    public AIHealingStrategy(AIHealingProvider provider) {
        this.provider = provider;
    }

    @Override
    public Optional<By> heal(
            By original,
            WebDriver driver,
            ElementIntent intent) {

        try {

            String domSnapshot =
                    driver.getPageSource();

            Optional<By> healed =
                    provider.heal(
                            original,
                            intent,
                            domSnapshot
                    );

            if (healed.isPresent()) {

                List<WebElement> matches =
                        driver.findElements(
                                healed.get()
                        );

                if (matches.size() == 1 &&
                        matches.get(0).isDisplayed()) {

                    return healed;
                }
            }

        } catch (Exception ignored) {
        }

        return Optional.empty();
    }

    @Override
    public String getName() {
        return "AIHealingStrategy";
    }
}