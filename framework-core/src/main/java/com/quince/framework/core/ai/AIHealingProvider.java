package com.quince.framework.core.ai;

import com.quince.framework.core.healing.ElementIntent;
import org.openqa.selenium.By;

import java.util.Optional;

public interface AIHealingProvider {

    Optional<By> heal(
            By original,
            ElementIntent intent,
            String domSnapshot
    );
}