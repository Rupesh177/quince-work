package com.quince.tests;

import com.quince.framework.core.driver.DriverManager;
import com.quince.framework.experiment.ExperimentContext;
import com.quince.framework.experiment.helpers.ExperimentSetupHelper;
import com.quince.framework.ui.actions.PDPActions;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PDPVariantTest extends BaseTest {

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
}
