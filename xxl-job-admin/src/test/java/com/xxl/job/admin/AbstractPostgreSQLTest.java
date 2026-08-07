package com.xxl.job.admin;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("pgtest")
public abstract class AbstractPostgreSQLTest {

    @Autowired
    private ApplicationContext applicationContext;

    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("xxl_job")
            .withUsername("test")
            .withPassword("test");

    static {
        POSTGRESQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRESQL_CONTAINER::getDriverClassName);
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:schema-postgresql.sql");
    }

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
