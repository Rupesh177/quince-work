package com.quince.framework.experiment;

import com.quince.framework.data.DataBuilder;
import com.quince.framework.data.DataRegistry;
import com.quince.framework.data.DataService;

import java.util.UUID;

// once record-based test data creation is supported.

public class ExperimentUserService {

    private final DataService dataService;

    public ExperimentUserService(DataService dataService) {
        this.dataService = dataService;
    }

    public String createUserForVariationSearch() {

        DataBuilder.UserData user =
                new DataBuilder.UserDataBuilder()
                        .email(UUID.randomUUID() + "@test.com")
                        .build();

        DataRegistry.register(user.userId());

        return user.userId();
    }
}
