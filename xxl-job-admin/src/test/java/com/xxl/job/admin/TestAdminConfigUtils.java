package com.xxl.job.admin;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import java.lang.reflect.Field;
import org.springframework.context.ApplicationContext;

final class TestAdminConfigUtils {

    private TestAdminConfigUtils() {}

    static void bindCurrentAdminConfig(ApplicationContext applicationContext) throws Exception {
        XxlJobAdminConfig springConfig = applicationContext.getBean(XxlJobAdminConfig.class);
        Field adminConfigField = XxlJobAdminConfig.class.getDeclaredField("adminConfig");
        adminConfigField.setAccessible(true);
        if (adminConfigField.get(null) != springConfig) {
            adminConfigField.set(null, springConfig);
        }
    }
}
