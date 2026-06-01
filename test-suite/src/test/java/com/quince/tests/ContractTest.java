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
    @Description("Validate treatment_b CTA behavior using Optimizely cta_position variable")
    public void testTreatmentB_CtaPositionControlsUi() {
        String flagKey = "pdp_cta_position";
        String expectedVariation = "treatment_b";

        // Finds a generated user bucketed into treatment_b.
        String userId = experiment.findUserForVariation(
                flagKey,
                expectedVariation
        );

        // Opens PDP for UI validation.
        openPDP();

        // Resolves Optimizely decision for the discovered user.
        ExperimentContext context = experiment.resolve(
                flagKey,
                userId,
                DriverManager.getDriver().getUnderlyingDriver()
        );

        Assert.assertEquals(
                context.variationKey(),
                expectedVariation,
                "Expected treatment_b variation"
        );

        Assert.assertTrue(
                context.enabled(),
                "Feature flag should be enabled"
        );

        // Reads cta_position variable from Optimizely.
        String ctaPosition = experiment.getStringVariable(
                flagKey,
                "cta_position",
                userId
        );

        Assert.assertEquals(
                ctaPosition,
                "sticky",
                "treatment_b should have sticky CTA"
        );

        logger.info(
                "AB Test Proof | User={}, Variation={}, cta_position={}",
                userId,
                context.variationKey(),
                ctaPosition
        );

        // Validates CTA placement based on Optimizely variable.
        pdp.validateCtaPosition(ctaPosition);

        // Clicks CTA using fallback chain.
        pdp.addToCart();

        logger.info(
                "Treatment_b validation successful | Variation={}, cta_position={}",
                context.variationKey(),
                ctaPosition
        );
    }
}
