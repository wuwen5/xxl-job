package com.xxl.job.admin;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractTest {

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void bindAdminConfig() throws Exception {
        TestAdminConfigUtils.bindCurrentAdminConfig(applicationContext);
    }
}
