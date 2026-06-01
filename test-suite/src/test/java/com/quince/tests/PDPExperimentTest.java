package com.quince.tests;

import com.quince.framework.core.driver.DriverManager;
import com.quince.framework.data.FakerDataService;
import com.quince.framework.experiment.ExperimentContext;
import com.quince.framework.experiment.helpers.ExperimentCoverageHelper;
import com.quince.framework.experiment.ExperimentUserService;
import com.quince.framework.ui.actions.PDPActions;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Test class demonstrating experiment/variant handling.
 * Showcases variant detection, setup helpers, and variant-aware assertions.
 */

@Epic("Experiments")
@Feature("PDP Variant Testing")
public class PDPExperimentTest extends BaseTest {

    private static final String PDP_CTA_FLAG = "pdp_cta_position";
    private static final String PDP_PRICE_FLAG = "pdp_price_display";

    @Test(groups = {"smoke", "experiment"})
    @Story("Add to Cart")
    @Description("Validate Add to Cart for control variation using cta_position variable")
    public void testAddToCart_ControlVariant() {
        String expectedVariation = "control";

        // Finds a generated user bucketed into control.
        String userId = experiment.findUserForVariation(PDP_CTA_FLAG, expectedVariation);

        // Opens PDP for UI validation.
        openPDP();

        // Resolves Optimizely decision for the discovered user.
        ExperimentContext context = experiment.resolve(
                PDP_CTA_FLAG,
                userId,
                DriverManager.getDriver().getUnderlyingDriver()
        );

        Assert.assertEquals(context.variationKey(), expectedVariation, "Expected control variant");
        Assert.assertTrue(context.enabled(), "Feature flag should be enabled");

        // Reads cta_position variable from Optimizely.
        String ctaPosition = experiment.getStringVariable(PDP_CTA_FLAG, "cta_position", userId);

        Assert.assertEquals(ctaPosition, "top", "Control variant should have cta_position=top");

        logger.info(
                "Resolved AB state | User={}, Variation={}, cta_position={}",
                userId,
                context.variationKey(),
                ctaPosition
        );

        // Keeps generic CTA availability check because AB layouts can move CTA position.
        Assert.assertTrue(pdp.isAddToCartPresent(), "Add to cart button should be visible");

        // Clicks CTA using fallback chain in PDPActions.
        pdp.addToCart();

        logger.info("Add to cart successful");
    }

    @Test(groups = {"smoke", "experiment"})
    @Story("PDP CTA Control Variant")
    @Description("Validate CTA label for control variation using Optimizely cta_label variable")
    public void testControlVariantCtaLabel() {
        String expectedVariation = "control";

        // Finds a generated user bucketed into control.
        String userId = experiment.findUserForVariation(PDP_CTA_FLAG, expectedVariation);

        // Opens PDP for UI validation.
        openPDP();

        // Resolves Optimizely decision for the discovered user.
        ExperimentContext context = experiment.resolve(
                PDP_CTA_FLAG,
                userId,
                DriverManager.getDriver().getUnderlyingDriver()
        );

        Assert.assertEquals(
                context.variationKey(),
                expectedVariation,
                "Resolved variation should be control"
        );

        Assert.assertTrue(
                context.enabled(),
                "Feature flag should be enabled"
        );

        // Reads expected CTA label from Optimizely.
        String expectedCtaLabel = experiment.getStringVariable(
                PDP_CTA_FLAG,
                "cta_label",
                userId
        );

        // Reads actual CTA label from UI.
        String actualCtaLabel = pdp.getCtaLabel();

        logger.info(
                "CTA Label Validation | Variation={}, User={}, ExpectedLabel={}, ActualLabel={}",
                context.variationKey(),
                userId,
                expectedCtaLabel,
                actualCtaLabel
        );

        Assert.assertEquals(
                actualCtaLabel,
                expectedCtaLabel,
                "CTA label should match Optimizely variable for control"
        );
    }

    @Test(groups = {"smoke", "experiment"})
    @Story("PDP CTA Treatment A Variant")
    @Description("Validate cart button visibility using cart_button variable")
    public void testTreatmentAVariantCartButtonVisibility() {
        String expectedVariation = "treatment_a";

        // Finds a generated user bucketed into treatment_a.
        String userId = experiment.findUserForVariation(PDP_CTA_FLAG, expectedVariation);

        // Opens PDP for UI validation.
        openPDP();

        // Resolves Optimizely decision for the discovered user.
        ExperimentContext context = experiment.resolve(
                PDP_CTA_FLAG,
                userId,
                DriverManager.getDriver().getUnderlyingDriver()
        );

        Assert.assertEquals(context.variationKey(), expectedVariation, "Expected treatment_a variant");
        Assert.assertTrue(context.enabled(), "Feature flag should be enabled");

        // Reads cart_button boolean variable from Optimizely.
        Boolean expectedCartButton = experiment.getBooleanVariable(PDP_CTA_FLAG, "cart_button", userId);

        Assert.assertNotNull(expectedCartButton, "cart_button variable should not be null");

        // Reads actual cart button visibility from UI.
        boolean actualCartButton = pdp.isCartPresent();

        logger.info(
                "Cart button validation | User={}, Variation={}, ExpectedCartButton={}, ActualCartButton={}",
                userId,
                context.variationKey(),
                expectedCartButton,
                actualCartButton
        );

        Assert.assertEquals(
                actualCartButton,
                expectedCartButton,
                "Cart button visibility should match Optimizely cart_button variable"
        );
    }

//    @Test(groups = {"regression", "experiment"}, dataProvider = "allPriceDisplayVariants")
//    @Story("Pricing Display")
//    @Description("Validate price display variable behavior across pdp_price_display variants")
//    public void testPricingDisplay_AllVariants(String expectedVariation) {
//
//        // Finds a generated user bucketed into the expected price-display variation.
//        String userId = experiment.findUserForVariation(PDP_PRICE_FLAG, expectedVariation);
//
//        // Opens PDP for UI validation.
//        openPDP();
//
//        // Resolves Optimizely decision for the discovered user.
//        ExperimentContext context = experiment.resolve(
//                PDP_PRICE_FLAG,
//                userId,
//                DriverManager.getDriver().getUnderlyingDriver()
//        );
//
//        Assert.assertEquals(
//                context.variationKey(),
//                expectedVariation,
//                "Resolved variation should match expected variation"
//        );
//
//        Assert.assertTrue(
//                context.enabled(),
//                "Price display feature flag should be enabled"
//        );
//
//        // Reads Optimizely variables for price display behavior.
//        String displayMode = experiment.getStringVariable(
//                PDP_PRICE_FLAG,
//                "display_mode",
//                userId
//        );
//
//        String pricePosition = experiment.getStringVariable(
//                PDP_PRICE_FLAG,
//                "price_position",
//                userId
//        );
//
//        logger.info(
//                "Price Display Validation | Variation={}, User={}, display_mode={}, price_position={}",
//                context.variationKey(),
//                userId,
//                displayMode,
//                pricePosition
//        );
//
//        Assert.assertNotNull(
//                displayMode,
//                "display_mode should not be null"
//        );
//
//        Assert.assertNotNull(
//                pricePosition,
//                "price_position should not be null"
//        );
//
//        Assert.assertTrue(
//                List.of("inline", "modal", "treatment_a").contains(displayMode),
//                "display_mode should be valid"
//        );
//
//        Assert.assertTrue(
//                List.of("below_title", "above_cta").contains(pricePosition),
//                "price_position should be valid"
//        );
//
//        // Reads price using variant-aware fallback logic in PDPActions.
//        String price = pdp.getPrice();
//
//        Assert.assertFalse(
//                price.isEmpty(),
//                "Price should be available for variant: " + expectedVariation
//        );
//
//        logger.info(
//                "Price validation successful | Variation={}, Price={}",
//                expectedVariation,
//                price
//        );
//    }
//
//    @DataProvider(name = "allPriceDisplayVariants")
//    public Object[][] allPriceDisplayVariants() {
//        return new Object[][]{
//                {"control"},
//                {"treatment_a"},
//                {"treatment_b"}
//        };
//    }
}
