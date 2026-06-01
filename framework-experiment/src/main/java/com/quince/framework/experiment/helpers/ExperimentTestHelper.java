package com.quince.framework.experiment.helpers;

import com.quince.framework.data.FakerDataService;
import com.quince.framework.experiment.ExperimentContext;
import com.quince.framework.experiment.ExperimentUserService;
import com.quince.framework.experiment.detection.VariantResolver;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

public class ExperimentTestHelper {

    private static final int DEFAULT_VARIANT_SEARCH_ATTEMPTS = 50;

    private final VariantResolver variantResolver;
    private final ExperimentCoverageHelper coverageHelper;

    public ExperimentTestHelper(VariantResolver variantResolver) {
        this.variantResolver = variantResolver;
        this.coverageHelper = new ExperimentCoverageHelper(
                variantResolver,
                new ExperimentUserService(new FakerDataService())
        );
    }

    public String findUserForVariation(String flagKey, String expectedVariation) {
        return coverageHelper.findUserForVariation(
                flagKey,
                expectedVariation,
                DEFAULT_VARIANT_SEARCH_ATTEMPTS
        );
    }

    @Step("Resolve Optimizely variation for flag: {flagKey}")
    public ExperimentContext resolve(String flagKey, String userId, Object webDriver) {
        ExperimentContext context = variantResolver.resolve(flagKey, userId, webDriver);
        Allure.parameter("flagKey", context.flagKey());
        Allure.parameter("userId", context.userId());
        Allure.parameter("variation", context.variationKey());
        Allure.parameter("enabled", context.enabled());
        Allure.parameter("detectionSource", context.detectionSource());
        Allure.parameter("confidence", context.confidence());

        return context;
    }

    @Step("Fetch string variable: {variableKey}")
    public String getStringVariable(String flagKey, String variableKey, String userId) {
        String value = variantResolver.getStringVariable(flagKey, variableKey, userId);

        Allure.parameter(variableKey, value);

        return value;
    }

    @Step("Fetch boolean variable: {variableKey}")
    public Boolean getBooleanVariable(String flagKey, String variableKey, String userId) {
        Boolean value = variantResolver.getBooleanVariable(flagKey, variableKey, userId);

        Allure.parameter(variableKey, value);

        return value;
    }
}