package com.xxl.job.admin;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import java.lang.reflect.Field;
import org.springframework.context.ApplicationContext;

final class TestAdminConfigUtils {

    private static final Field ADMIN_CONFIG_FIELD = initAdminConfigField();

    private TestAdminConfigUtils() {}

    static void bindCurrentAdminConfig(ApplicationContext applicationContext) {
        try {
            XxlJobAdminConfig springConfig = applicationContext.getBean(XxlJobAdminConfig.class);
            if (ADMIN_CONFIG_FIELD.get(null) != springConfig) {
                ADMIN_CONFIG_FIELD.set(null, springConfig);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to rebind XxlJobAdminConfig.adminConfig", e);
        }
    }

    private static Field initAdminConfigField() {
        try {
            Field adminConfigField = XxlJobAdminConfig.class.getDeclaredField("adminConfig");
            adminConfigField.setAccessible(true);
            return adminConfigField;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Failed to access XxlJobAdminConfig.adminConfig", e);
        }
    }
}
