package com.quince.framework.core.healing;

import com.quince.framework.core.healing.strategies.AIHealingStrategy;
import com.quince.framework.core.healing.strategies.AttributeHealingStrategy;
import com.quince.framework.core.healing.strategies.TextHealingStrategy;
import com.quince.framework.core.healing.strategies.XPathHealingStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Optional;

public class HealingManager {
    private static final Logger logger = LogManager.getLogger(HealingManager.class);

    private final List<HealingStrategy> strategies = List.of(
            new AttributeHealingStrategy(),
            new TextHealingStrategy(),
            new XPathHealingStrategy(),
            new AIHealingStrategy()
    );

    public Optional<By> heal(By original,
                             WebDriver driver,
                             ElementIntent intent) {

        for (HealingStrategy strategy : strategies) {
            try {
                Optional<By> healed = strategy.heal(original, driver, intent);

                if (healed.isPresent()) {
                    logger.warn(
                            "Locator healed using {} | Original={} | Healed={} | Element={}",
                            strategy.getName(),
                            original,
                            healed.get(),
                            intent.elementName()
                    );
                    return healed;
                }
            } catch (Exception e) {
                logger.debug(
                        "Healing strategy {} failed for {}",
                        strategy.getName(),
                        intent != null ? intent.elementName() : original,
                        e
                );
            }
        }

        return Optional.empty();
    }
}