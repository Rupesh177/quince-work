package com.quince.tests;

import com.quince.framework.core.driver.DriverManager;
import com.quince.framework.data.FakerDataService;
import com.quince.framework.experiment.ExperimentContext;
import com.quince.framework.experiment.ExperimentCoverageHelper;
import com.quince.framework.experiment.ExperimentSetupHelper;
import com.quince.framework.experiment.ExperimentUserService;
import com.quince.framework.ui.actions.PDPActions;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

/**
 * Test class demonstrating experiment/variant handling.
 * Showcases variant detection, setup helpers, and variant-aware assertions.
 */

@Epic("Experiments")
@Feature("PDP Variant Testing")
public class PDPExperimentTest extends BaseTest {

    @Test(groups = {"smoke", "experiment"})
    @Story("Add to Cart")
    @Description("Test add to cart with resolved variant")
    public void testAddToCart_ResolvedVariant() {
        // Navigate
        DriverManager.getDriver().get(config.get("base.url", "http://localhost:5173") + "/product/DEMO_SKU");

        // Detect variant
        ExperimentContext context = variantResolver.resolve(
                "pdp_cta_position",
                testUserId,
                DriverManager.getDriver().getUnderlyingDriver()
        );

        logger.info("Detected variant: {}", context.variationKey());
//        Assert.assertEquals(context.variationKey(), "control",
//                "Expected control variant");


        // Interact
        PDPActions pdp = new PDPActions(DriverManager.getDriver());
        Assert.assertTrue(pdp.isAddToCartPresent(),
                "Add to cart button should be visible");

        pdp.addToCart();
        logger.info("Add to cart successful");
    }

//    @Test(groups = {"smoke", "experiment"})
//    @Story("PDP CTA Control Variant")
//    @Description("Validate cta_label variable for control variant")
//    public void testControlVariantCtaLabel() {
//        String flagKey = "pdp_cta_position";
//        String expectedVariation = "control";
//
//        ExperimentCoverageHelper coverageHelper =
//                new ExperimentCoverageHelper(
//                        variantResolver,
//                        new ExperimentUserService(new FakerDataService())
//                );
//
//        String userId = coverageHelper.findUserForVariation(
//                flagKey,
//                expectedVariation,
//                100
//        );
//
//        DriverManager.getDriver().get(
//                config.get("base.url", "http://localhost:5173") + "/product/DEMO_SKU"
//        );
//
//        ExperimentContext context = variantResolver.resolve(
//                flagKey,
//                userId,
//                DriverManager.getDriver().getUnderlyingDriver()
//        );
//
//        Assert.assertEquals(
//                context.variationKey(),
//                expectedVariation,
//                "Resolved variation should be control"
//        );
//
//        String expectedCtaLabel = variantResolver.getStringVariable(
//                flagKey,
//                "cta_label",
//                userId
//        );
//
//        PDPActions pdp = new PDPActions(DriverManager.getDriver());
//        String actualCtaLabel = pdp.getCtaLabel();
//
//        logger.info(
//                "CTA Label Validation | Variation={}, User={}, ExpectedLabel={}, ActualLabel={}",
//                context.variationKey(),
//                userId,
//                expectedCtaLabel,
//                actualCtaLabel
//        );
//
//        Assert.assertEquals(
//                actualCtaLabel,
//                expectedCtaLabel,
//                "CTA label should match Optimizely variable for control"
//        );
//    }
//
//    @Test(groups = {"smoke", "experiment"})
//    @Story("PDP CTA Treatment A Variant")
//    @Description("Validate cta_label variable for treatment_a variant")
//    public void testTreatmentAVariantCtaLabel() {
//        String flagKey = "pdp_cta_position";
//        String expectedVariation = "treatment_a";
//
//        ExperimentCoverageHelper coverageHelper =
//                new ExperimentCoverageHelper(
//                        variantResolver,
//                        new ExperimentUserService(new FakerDataService())
//                );
//
//        String userId = coverageHelper.findUserForVariation(
//                flagKey,
//                expectedVariation,
//                100
//        );
//
//        DriverManager.getDriver().get(
//                config.get("base.url", "http://localhost:5173") + "/product/DEMO_SKU"
//        );
//
//        ExperimentContext context = variantResolver.resolve(
//                flagKey,
//                userId,
//                DriverManager.getDriver().getUnderlyingDriver()
//        );
//
//        Assert.assertEquals(
//                context.variationKey(),
//                expectedVariation,
//                "Resolved variation should be treatment_a"
//        );
//
//        Boolean expectedCartButton = variantResolver.getBooleanVariable(
//                flagKey,
//                "cart_button",
//                userId
//        );
//
//        Assert.assertNotNull(
//                expectedCartButton,
//                "cart_button variable should not be null"
//        );
//
//
//        PDPActions pdp = new PDPActions(DriverManager.getDriver());
//
//        boolean actualCartButton = pdp.isCartPresent();
//
//        logger.info(
//                "Cart button display Validation | Variation={}, User={}, ExpectedCartButton={}, ActualCartButton={}",
//                context.variationKey(),
//                userId,
//                expectedCartButton,
//                actualCartButton
//        );
//
//        Assert.assertEquals(
//                actualCartButton,
//                expectedCartButton,
//                "Cart button visibility should match Optimizely cart_button variable"
//        );
//    }

    @Test(groups = {"regression", "experiment"}, dataProvider = "allPriceDisplayVariants")
    @Story("Pricing Display")
    @Description("Validate price display variable behavior across pdp_price_display variants")
    public void testPricingDisplay_AllVariants(String expectedVariation) {
        String flagKey = "pdp_price_display";

        ExperimentCoverageHelper coverageHelper =
                new ExperimentCoverageHelper(
                        variantResolver,
                        new ExperimentUserService(new FakerDataService())
                );

        String userId = coverageHelper.findUserForVariation(
                flagKey,
                expectedVariation,
                200
        );

        DriverManager.getDriver().get(
                config.get("base.url", "http://localhost:5173") + "/product/DEMO_SKU"
        );

        ExperimentContext context = variantResolver.resolve(
                flagKey,
                userId,
                DriverManager.getDriver().getUnderlyingDriver()
        );

        Assert.assertEquals(
                context.variationKey(),
                expectedVariation,
                "Resolved variation should match expected variation"
        );

        String displayMode = variantResolver.getStringVariable(
                flagKey,
                "display_mode",
                userId
        );

        String pricePosition = variantResolver.getStringVariable(
                flagKey,
                "price_position",
                userId
        );

        logger.info(
                "Price Display Validation | Variation={}, User={}, display_mode={}, price_position={}",
                context.variationKey(),
                userId,
                displayMode,
                pricePosition
        );

        Assert.assertNotNull(displayMode, "display_mode should not be null");
        Assert.assertNotNull(pricePosition, "price_position should not be null");

        Assert.assertTrue(
                List.of("inline", "modal", "treatment_a").contains(displayMode),
                "display_mode should be valid"
        );

        Assert.assertTrue(
                List.of("below_title", "above_cta").contains(pricePosition),
                "price_position should be valid"
        );

        PDPActions pdp = new PDPActions(DriverManager.getDriver());
        String price = pdp.getPrice();

        Assert.assertFalse(
                price.isEmpty(),
                "Price should be available for variant: " + expectedVariation
        );

        logger.info("Price for variation={} is {}", expectedVariation, price);
    }

    @DataProvider(name = "allPriceDisplayVariants")
    public Object[][] allPriceDisplayVariants() {
        return new Object[][]{
                {"control"},
                {"treatment_a"},
                {"treatment_b"}
        };
    }
}
