package com.quince.framework.ui.actions;

import com.quince.framework.core.driver.UIDriver;
import com.quince.framework.core.healing.ElementIntent;
import com.quince.framework.experiment.ExperimentContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.testng.Assert;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Actions for Product Detail Page (PDP).
 * Handles variant-aware interactions (control vs treatment).
 */
public class PDPActions extends BaseActions {
    private static final Logger logger = LogManager.getLogger(PDPActions.class);

    // Locators as private static final constants
    private static final By PRODUCT_TITLE = By.cssSelector("h1.product-title");
    private static final By PRODUCT_PRICE = By.cssSelector("[data-testid='product-price']");
    private static final By PRICE_MODAL = By.id("price-modal");
    private static final By CTA_TOP =
            By.xpath("//button[@data-testid='add-to-cart-button' and contains(normalize-space(.),'Add to Cart')]");
    private static final By CTA_BOTTOM = By.cssSelector("button.add-to-cart-bottom");
    private static final By CTA_STICKY = By.id("sticky-bar-cta");
    private static final By RECOMMENDATIONS = By.cssSelector("[data-testid='recommendations'] .product-item");
    private static final By CART_COUNT = By.cssSelector("[data-cart-count]");
    private static final By CART_BUTTON = By.xpath("//button[@data-testid='cart-button']");

    public PDPActions(UIDriver driver) {
        super(driver);
    }

    /**
     * Gets the product title.
     */
    public String getProductTitle() {
        logger.info("Getting product title");
        return getText(PRODUCT_TITLE);
    }

    /**
     * Gets product price (variant-aware).
     * Control: inline price element
     * Treatment A: modal-based price
     * Treatment B: sticky bar pricing
     */
    public String getPrice() {
        logger.info("Getting product price");

        try {
            waitForVisible(PRODUCT_PRICE);
            String price = getText(PRODUCT_PRICE);
            logger.info("Found inline price: {}", price);
            return price;
        } catch (Exception e) {
            logger.debug("Inline price not visible after wait, trying modal price");
        }

        try {
            waitForVisible(PRICE_MODAL);
            String price = getText(By.cssSelector("#price-modal .price-value"));
            logger.info("Found modal price: {}", price);
            return price;
        } catch (Exception e) {
            logger.debug("Modal price not visible after wait");
        }

        logger.warn("No price element found");
        return "";
    }
    /**
     * Adds product to cart with fallback chain.
     * Tries: top CTA → bottom CTA → sticky bar CTA
     * Logs which CTA was successful.
     */
    public void addToCart() {
        logger.info("Adding product to cart");

        // Fallback chain
        Optional<String> successfulCta = tryClick(CTA_TOP, "top-cta")
                .or(() -> tryClick(CTA_BOTTOM, "bottom-cta"))
                .or(() -> tryClick(CTA_STICKY, "sticky-cta"));

        if (successfulCta.isPresent()) {
            logger.info("Successfully clicked CTA: {}", successfulCta.get());
        } else {
            logger.error("All CTA buttons failed");
            throw new RuntimeException("Could not find clickable add-to-cart button");
        }
    }

    /**
     * Checks if add-to-cart is present.
     */
    public boolean isAddToCartPresent() {
        logger.info("Checking Add to Cart presence");

        try {
            waitForVisible(CTA_TOP);
            return true;
        } catch (Exception e) {
            logger.debug("Top CTA not visible yet, trying fallback CTAs");
        }

        return softAssertVisible(CTA_TOP) ||
                softAssertVisible(CTA_BOTTOM) ||
                softAssertVisible(CTA_STICKY);
    }

    /**
     * Checks if cart is present.
     */
    public boolean isCartPresent() {
        try {
            waitForVisible(CART_BUTTON);
            return true;
        } catch (Exception e) {
            logger.warn("Cart button not visible for locator: {}", CART_BUTTON);
            return false;
        }
    }


    /**
     * Validates pricing display for a specific variant.
     */
    public void validatePricingDisplay(ExperimentContext context) {
        logger.info("Validating pricing display for variant: {}", context.variationKey());

        switch (context.variationKey()) {
            case "control" -> {
                // Control: inline price visible
                if (!softAssertVisible(PRODUCT_PRICE)) {
                    logger.error("Control variant: inline price not visible");
                }
            }
            case "treatment_a" -> {
                // Treatment A: modal price visible
                if (!softAssertVisible(PRICE_MODAL)) {
                    logger.error("Treatment A: modal price not visible");
                }
            }
            case "treatment_b" -> {
                // Treatment B: sticky bar visible
                if (!softAssertVisible(CTA_STICKY)) {
                    logger.error("Treatment B: sticky bar not visible");
                }
            }
            default -> logger.warn("Unknown variant: {}", context.variationKey());
        }
    }

    public void validateCtaPosition(String expectedPosition) {
        logger.info("Validating CTA position from variable: {}", expectedPosition);

        switch (expectedPosition) {
            case "top" -> Assert.assertTrue(
                    driver.isDisplayed(CTA_TOP),
                    "Top CTA should be visible for cta_position=top"
            );

            case "bottom" -> Assert.assertTrue(
                    driver.isDisplayed(CTA_BOTTOM),
                    "Bottom CTA should be visible for cta_position=bottom"
            );

            case "sticky" -> Assert.assertTrue(
                    driver.isDisplayed(CTA_STICKY),
                    "Sticky CTA should be visible for cta_position=sticky"
            );

            default -> Assert.fail("Unsupported cta_position value: " + expectedPosition);
        }
    }

    /**
     * Helper: tries to click a locator, returns the locator name if successful.
     */
    private Optional<String> tryClick(By locator, String name) {
        try {
            click(locator);
            return Optional.of(name);
        } catch (Exception e) {
            logger.debug("Failed to click {}: {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    public String getCtaLabel() {
        return getText(CTA_TOP).trim();
    }

//    private static final ElementIntent ADD_TO_CART_INTENT =
//            new ElementIntent(
//                    "Add To Cart CTA",
//                    "Add to Cart",
//                    "button",
//                    "button",
//                    Map.of("data-testid", "add-to-cart-button")
//            );
}
