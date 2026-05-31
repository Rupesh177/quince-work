package com.quince.tests;


import com.quince.framework.core.driver.DriverManager;
import com.quince.framework.experiment.ExperimentContext;
import com.quince.framework.ui.actions.PDPActions;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

@Epic("Experiments")
@Feature("PDP Variant Testing")
public class ContractTest extends BaseTest {

    @Test(groups = {"smoke", "experiment"})
    @Story("PDP CTA Position")
    @Description("Validate PDP CTA behavior using Optimizely resolved variation and cta_position variable")
    public void testPdpCtaPositionVariableControlsUi() {
        String flagKey = "pdp_cta_position";

        DriverManager.getDriver().get(
                config.get("base.url", "http://localhost:5173") + "/product/DEMO_SKU"
        );

        ExperimentContext context = variantResolver.resolve(
                flagKey,
                testUserId,
                DriverManager.getDriver().getUnderlyingDriver()
        );

        Assert.assertTrue(
                context.enabled(),
                "Feature flag should be enabled"
        );

        Assert.assertTrue(
                List.of("control", "treatment_a", "treatment_b")
                        .contains(context.variationKey()),
                "Variation should be one of configured Optimizely variations"
        );

        String ctaPosition = variantResolver.getStringVariable(
                flagKey,
                "cta_position",
                testUserId
        );

        Assert.assertTrue(
                List.of("top", "bottom", "sticky").contains(ctaPosition),
                "cta_position should be one of expected values"
        );

        logger.info(
                "AB Test Proof | Flag={}, Enabled={}, Variation={}, cta_position={}",
                flagKey,
                context.enabled(),
                context.variationKey(),
                ctaPosition
        );

        PDPActions pdp = new PDPActions(DriverManager.getDriver());

        pdp.validateCtaPosition(ctaPosition);

        pdp.addToCart();

        logger.info(
                "PDP CTA validation successful for variation={} and cta_position={}",
                context.variationKey(),
                ctaPosition
        );
    }

}
