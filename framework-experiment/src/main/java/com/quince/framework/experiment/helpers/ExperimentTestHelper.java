package com.quince.framework.experiment.helpers;

import com.quince.framework.data.FakerDataService;
import com.quince.framework.experiment.ExperimentContext;
import com.quince.framework.experiment.ExperimentUserService;
import com.quince.framework.experiment.detection.VariantResolver;

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

    public ExperimentContext resolve(String flagKey, String userId, Object webDriver) {
        return variantResolver.resolve(flagKey, userId, webDriver);
    }

    public String getStringVariable(String flagKey, String variableKey, String userId) {
        return variantResolver.getStringVariable(flagKey, variableKey, userId);
    }

    public Boolean getBooleanVariable(String flagKey, String variableKey, String userId) {
        return variantResolver.getBooleanVariable(flagKey, variableKey, userId);
    }
}