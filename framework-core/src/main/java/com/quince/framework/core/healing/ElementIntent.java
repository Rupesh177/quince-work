package com.quince.framework.core.healing;

import java.util.Map;

public record ElementIntent(
        String elementName,
        String expectedText,
        String tagName,
        String role,
        Map<String, String> stableAttributes
) {
}