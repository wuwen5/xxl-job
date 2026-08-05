package com.xxl.job.admin.core.thread;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobRegistry;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobRegistryDao;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JobRegistryHelper unit test
 */
class JobRegistryHelperTest {

    private static XxlJobAdminConfig adminConfig;
    private static XxlJobRegistryDao registryDao;
    private static XxlJobGroupDao groupDao;
    private static XxlJobAdminConfig originalAdminConfig;
    private static Field adminConfigField;

    @BeforeAll
    static void setUpClass() throws Exception {
        adminConfigField = XxlJobAdminConfig.class.getDeclaredField("adminConfig");
        adminConfigField.setAccessible(true);
        originalAdminConfig = (XxlJobAdminConfig) adminConfigField.get(null);

        adminConfig = mock(XxlJobAdminConfig.class);
        registryDao = mock(XxlJobRegistryDao.class);
        groupDao = mock(XxlJobGroupDao.class);

        when(adminConfig.getXxlJobRegistryDao()).thenReturn(registryDao);
        when(adminConfig.getXxlJobGroupDao()).thenReturn(groupDao);

        // Set static adminConfig via reflection
        adminConfigField.set(null, adminConfig);
    }

    @BeforeEach
    void setUp() throws Exception {
        stopHelper();
        adminConfigField.set(null, adminConfig);
        reset(registryDao, groupDao);
        resetSingletonInstance();
    }

    @AfterEach
    void tearDown() {
        stopHelper();
    }

    @AfterAll
    static void tearDownClass() throws Exception {
        stopHelper();
        adminConfigField.set(null, originalAdminConfig);
    }

    private void resetSingletonInstance() throws Exception {
        Field instanceField = JobRegistryHelper.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, new JobRegistryHelper());
    }

    private static void stopHelper() {
        try {
            JobRegistryHelper.getInstance().toStop();
        } catch (Exception ignored) {
        }
    }

    @Test
    void shouldReturnFailWhenRegistryWithInvalidParam() {
        RegistryParam param = new RegistryParam();
        param.setRegistryGroup("");
        param.setRegistryKey("test-app");
        param.setRegistryValue("http://127.0.0.1:9999");

        ReturnT<String> result = JobRegistryHelper.getInstance().registry(param);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        assertEquals("Illegal Argument.", result.getMsg());
    }

    @Test
    void shouldReturnFailWhenRegistryRemoveWithInvalidParam() {
        RegistryParam param = new RegistryParam();
        param.setRegistryGroup(RegistryConfig.RegistType.EXECUTOR.name());
        param.setRegistryKey(null);
        param.setRegistryValue("http://127.0.0.1:9999");

        ReturnT<String> result = JobRegistryHelper.getInstance().registryRemove(param);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        assertEquals("Illegal Argument.", result.getMsg());
    }

    @Test
    void shouldRemoveDeadAndUpdateGroupAddressWhenMonitorRunning() throws Exception {
        XxlJobGroup group = new XxlJobGroup();
        group.setId(1);
        group.setAppname("test-app");
        group.setAddressType(0);

        XxlJobRegistry aliveRegistry = new XxlJobRegistry();
        aliveRegistry.setId(2);
        aliveRegistry.setRegistryGroup(RegistryConfig.RegistType.EXECUTOR.name());
        aliveRegistry.setRegistryKey("test-app");
        aliveRegistry.setRegistryValue("http://127.0.0.1:9999");
        aliveRegistry.setUpdateTime(new Date());

        when(groupDao.findByAddressType(0)).thenReturn(Collections.singletonList(group));
        when(registryDao.findDead(anyInt(), any(Date.class))).thenReturn(Collections.singletonList(3));
        when(registryDao.findAll(anyInt(), any(Date.class))).thenReturn(Collections.singletonList(aliveRegistry));
        when(registryDao.removeDead(anyList())).thenReturn(1);
        when(groupDao.update(any(XxlJobGroup.class))).thenReturn(1);

        JobRegistryHelper helper = JobRegistryHelper.getInstance();
        helper.start();

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(registryDao, atLeastOnce()).findDead(anyInt(), any(Date.class));
            verify(registryDao, atLeastOnce()).removeDead(argThat(ids -> ids != null && ids.contains(3)));
            verify(registryDao, atLeastOnce()).findAll(anyInt(), any(Date.class));
            verify(groupDao, atLeastOnce()).update(argThat(g -> "http://127.0.0.1:9999".equals(g.getAddressList())));
        });
    }

    @Test
    void shouldUpdateGroupWhenNoRegistryAddresses() {
        XxlJobGroup group = new XxlJobGroup();
        group.setId(1);
        group.setAppname("test-app");
        group.setAddressType(0);
        group.setAddressList("old-address");

        when(groupDao.findByAddressType(0)).thenReturn(Collections.singletonList(group));
        when(registryDao.findDead(anyInt(), any(Date.class))).thenReturn(Collections.emptyList());
        when(registryDao.findAll(anyInt(), any(Date.class))).thenReturn(Collections.emptyList());
        when(groupDao.update(any(XxlJobGroup.class))).thenReturn(1);

        JobRegistryHelper helper = JobRegistryHelper.getInstance();
        helper.start();

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(groupDao, atLeastOnce()).update(argThat(g -> g.getAddressList() == null));
        });
    }

    @Test
    void shouldSkipNonExecutorRegistryGroupWhenBuildingAddressMap() throws Exception {
        XxlJobGroup group = new XxlJobGroup();
        group.setId(1);
        group.setAppname("test-app");
        group.setAddressType(0);

        XxlJobRegistry adminRegistry = new XxlJobRegistry();
        adminRegistry.setRegistryGroup(RegistryConfig.RegistType.ADMIN.name());
        adminRegistry.setRegistryKey("test-app");
        adminRegistry.setRegistryValue("http://127.0.0.1:9999");

        when(groupDao.findByAddressType(0)).thenReturn(Collections.singletonList(group));
        when(registryDao.findDead(anyInt(), any(Date.class))).thenReturn(Collections.emptyList());
        when(registryDao.findAll(anyInt(), any(Date.class))).thenReturn(Collections.singletonList(adminRegistry));
        when(groupDao.update(any(XxlJobGroup.class))).thenReturn(1);

        JobRegistryHelper helper = JobRegistryHelper.getInstance();
        helper.start();

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(groupDao, atLeastOnce()).update(any(XxlJobGroup.class));
            verify(groupDao, atLeastOnce()).update(argThat(g -> g.getAddressList() == null));
        });
    }

    @Test
    void shouldHandleExceptionWhenMonitorThreadError() throws Exception {
        when(groupDao.findByAddressType(0)).thenThrow(new RuntimeException("database error"));

        JobRegistryHelper helper = JobRegistryHelper.getInstance();
        helper.start();

        // Wait for monitor thread to run at least one cycle
        Thread.sleep(1500);

        helper.toStop();

        verify(groupDao, atLeastOnce()).findByAddressType(0);
    }

    @Test
    void shouldNotRemoveDeadWhenEmptyDeadList() throws Exception {
        XxlJobGroup group = new XxlJobGroup();
        group.setId(1);
        group.setAppname("test-app");
        group.setAddressType(0);

        when(groupDao.findByAddressType(0)).thenReturn(Collections.singletonList(group));
        when(registryDao.findDead(anyInt(), any(Date.class))).thenReturn(Collections.emptyList());
        when(registryDao.findAll(anyInt(), any(Date.class))).thenReturn(Collections.emptyList());
        when(groupDao.update(any(XxlJobGroup.class))).thenReturn(1);

        JobRegistryHelper helper = JobRegistryHelper.getInstance();
        helper.start();

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(registryDao, never()).removeDead(anyList());
            verify(groupDao, atLeastOnce()).update(any(XxlJobGroup.class));
        });
    }

    @Test
    void shouldHandleEmptyGroupList() throws Exception {
        when(groupDao.findByAddressType(0)).thenReturn(new ArrayList<>());

        JobRegistryHelper helper = JobRegistryHelper.getInstance();
        helper.start();

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(groupDao, atLeastOnce()).findByAddressType(0);
            verify(registryDao, never()).findDead(anyInt(), any(Date.class));
            verify(registryDao, never()).findAll(anyInt(), any(Date.class));
        });
    }
}
