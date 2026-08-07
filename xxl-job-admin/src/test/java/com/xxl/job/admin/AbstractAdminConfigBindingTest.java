package com.xxl.job.admin;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

abstract class AbstractAdminConfigBindingTest {

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void bindAdminConfig() {
        TestAdminConfigUtils.bindCurrentAdminConfig(applicationContext);
    }
}
