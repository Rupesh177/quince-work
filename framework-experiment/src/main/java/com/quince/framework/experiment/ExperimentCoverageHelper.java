package com.quince.framework.experiment;

import com.quince.framework.experiment.detection.VariantResolver;

public class ExperimentCoverageHelper {

    private final VariantResolver variantResolver;
    private final ExperimentUserService experimentUserService;

    public ExperimentCoverageHelper(VariantResolver variantResolver,
                                    ExperimentUserService experimentUserService) {
        this.variantResolver = variantResolver;
        this.experimentUserService = experimentUserService;
    }

    public String findUserForVariation(String flagKey,
                                       String expectedVariation,
                                       int maxAttempts) {

        for (int i = 1; i <= maxAttempts; i++) {
            String userId = experimentUserService.createUserForVariationSearch();

            ExperimentContext context = variantResolver.resolve(
                    flagKey,
                    userId,
                    null
            );

            if (expectedVariation.equals(context.variationKey())) {
                return userId;
            }
        }

        throw new AssertionError(
                "Could not find user for variation: " + expectedVariation
        );
    }
}