package com.quince.tests;

import com.quince.framework.core.driver.DriverManager;
import com.quince.framework.experiment.ExperimentContext;
import com.quince.framework.experiment.helpers.ExperimentSetupHelper;
import com.quince.framework.ui.actions.PDPActions;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PDPVariantTest extends BaseTest {

    private static final String PDP_CTA_FLAG = "pdp_cta_position";

    @Test(groups = {"smoke", "experiment"})
    @Story("Injected Variant")
    @Description("Validate framework can inject a variant signal and resolve it without relying on Optimizely bucketing")
    public void testAddToCart_WithInjectedTreatmentAVariant() {
        String flagKey = "pdp_cta_position";

        ExperimentSetupHelper setupHelper =
                new ExperimentSetupHelper(variantResolver.getFlagProvider());

        setupHelper.pinVariant("pdp_cta_position", testUserId, "control");

        DriverManager.getDriver().get(
                config.get("base.url", "http://localhost:5173") + "/product/DEMO_SKU"
        );

        ExperimentContext context = variantResolver.resolve(
                flagKey,
                testUserId,
                DriverManager.getDriver().getUnderlyingDriver()
        );

        Assert.assertEquals(
                context.variationKey(),
                "control",
                "Injected variant should be resolved by framework"
        );

        PDPActions pdp = new PDPActions(DriverManager.getDriver());

        Assert.assertTrue(
                pdp.isAddToCartPresent(),
                "Add to cart should be visible for injected variant"
        );

        pdp.addToCart();

        logger.info(
                "Injected variant test passed | Flag={}, InjectedVariant={}, ResolvedVariant={}",
                flagKey,
                "control",
                context.variationKey()
        );

        // Cleanup
        setupHelper.clearForcedVariant("pdp_cta_position", testUserId);
    }

    @Test(groups = {"smoke", "experiment"})
    @Story("Variant Detection Fallback")
    @Description("Validate cookie-based variant detection when Optimizely is down as part of fallback validation")
    public void testCookieVariantFallbackDetection() {
        String expectedVariation = "control";

        // Opens PDP for UI validation.
        openPDP();

        ExperimentSetupHelper setupHelper =
                new ExperimentSetupHelper(variantResolver.getFlagProvider());

        // Injects experiment variant cookie to simulate application-provided fallback signal.
        setupHelper.setCookieVariant(
                (WebDriver) DriverManager.getDriver().getUnderlyingDriver(),
                PDP_CTA_FLAG,
                expectedVariation
        );

        // Resolves variation using fallback detector chain.
        ExperimentContext context = experiment.resolve(
                PDP_CTA_FLAG,
                testUserId,
                DriverManager.getDriver().getUnderlyingDriver()
        );

        Assert.assertEquals(
                context.variationKey(),
                expectedVariation,
                "Variant should be resolved from cookie fallback"
        );

        Assert.assertEquals(
                context.detectionSource(),
                "CookieVariantDetector",
                "Detection source should be CookieVariantDetector"
        );

        logger.info(
                "Cookie fallback validation successful | Flag={}, Variation={}, Source={}",
                PDP_CTA_FLAG,
                context.variationKey(),
                context.detectionSource()
        );
    }
//    healingManager.heal(CTA_TOP, webDriver, ADD_TO_CART_INTENT);
//    public void addToCartWithHealing() {
//        clickWithHealing(CTA_TOP, ADD_TO_CART_INTENT);
//    }
}
