package com.xxl.job.admin;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import java.lang.reflect.Field;
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
        XxlJobAdminConfig springConfig = applicationContext.getBean(XxlJobAdminConfig.class);
        Field adminConfigField = XxlJobAdminConfig.class.getDeclaredField("adminConfig");
        adminConfigField.setAccessible(true);
        if (adminConfigField.get(null) != springConfig) {
            adminConfigField.set(null, springConfig);
        }
    }
}
