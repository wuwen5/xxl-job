package com.xxl.job.admin.core.route.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.IdleBeatParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for ExecutorRouteBusyover
 */
public class ExecutorRouteBusyoverTest {

    private static XxlJobAdminConfig originalAdminConfig;
    private static Field adminConfigField;

    @BeforeAll
    static void setUp() throws Exception {
        adminConfigField = XxlJobAdminConfig.class.getDeclaredField("adminConfig");
        adminConfigField.setAccessible(true);
        originalAdminConfig = (XxlJobAdminConfig) adminConfigField.get(null);
        XxlJobAdminConfig xxlJobAdminConfig = new XxlJobAdminConfig();
        adminConfigField.set(null, xxlJobAdminConfig);
    }

    @AfterAll
    static void tearDown() throws Exception {
        adminConfigField.set(null, originalAdminConfig);
    }

    @Test
    public void shouldRouteToFirstIdleAddress() {
        ExecutorRouteBusyover router = new ExecutorRouteBusyover();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(1);

        try (MockedStatic<XxlJobScheduler> mockedScheduler = mockStatic(XxlJobScheduler.class)) {
            ExecutorBiz mockExecutorBiz = mock(ExecutorBiz.class);
            when(mockExecutorBiz.idleBeat(any(IdleBeatParam.class)))
                    .thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "idle"));

            mockedScheduler
                    .when(() -> XxlJobScheduler.getExecutorBiz(anyString()))
                    .thenReturn(mockExecutorBiz);

            ReturnT<String> result = router.route(triggerParam, addressList);

            assertNotNull(result);
            assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
            assertEquals("192.168.1.1:9999", result.getContent());
            assertTrue(result.getMsg().contains("address：192.168.1.1:9999"));
        }
    }

    @Test
    public void shouldSkipBusyAddressAndRouteToNext() {
        ExecutorRouteBusyover router = new ExecutorRouteBusyover();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(2);

        try (MockedStatic<XxlJobScheduler> mockedScheduler = mockStatic(XxlJobScheduler.class)) {
            // First address is busy, second is idle
            ExecutorBiz mockExecutorBiz1 = mock(ExecutorBiz.class);
            when(mockExecutorBiz1.idleBeat(any(IdleBeatParam.class)))
                    .thenReturn(new ReturnT<>(ReturnT.FAIL_CODE, "busy"));

            ExecutorBiz mockExecutorBiz2 = mock(ExecutorBiz.class);
            when(mockExecutorBiz2.idleBeat(any(IdleBeatParam.class)))
                    .thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "idle"));

            mockedScheduler
                    .when(() -> XxlJobScheduler.getExecutorBiz("192.168.1.1:9999"))
                    .thenReturn(mockExecutorBiz1);
            mockedScheduler
                    .when(() -> XxlJobScheduler.getExecutorBiz("192.168.1.2:9999"))
                    .thenReturn(mockExecutorBiz2);
            mockedScheduler
                    .when(() -> XxlJobScheduler.getExecutorBiz("192.168.1.3:9999"))
                    .thenReturn(mockExecutorBiz2);

            ReturnT<String> result = router.route(triggerParam, addressList);

            assertNotNull(result);
            assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
            assertEquals("192.168.1.2:9999", result.getContent());
            // Should contain idle beat results from both addresses
            assertTrue(result.getMsg().contains("192.168.1.1:9999"));
            assertTrue(result.getMsg().contains("192.168.1.2:9999"));
        }
    }

    @Test
    public void shouldReturnFailureWhenAllAddressesAreBusy() {
        ExecutorRouteBusyover router = new ExecutorRouteBusyover();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(3);

        try (MockedStatic<XxlJobScheduler> mockedScheduler = mockStatic(XxlJobScheduler.class)) {
            ExecutorBiz mockExecutorBiz = mock(ExecutorBiz.class);
            when(mockExecutorBiz.idleBeat(any(IdleBeatParam.class)))
                    .thenReturn(new ReturnT<>(ReturnT.FAIL_CODE, "busy"));

            mockedScheduler
                    .when(() -> XxlJobScheduler.getExecutorBiz(anyString()))
                    .thenReturn(mockExecutorBiz);

            ReturnT<String> result = router.route(triggerParam, addressList);

            assertNotNull(result);
            assertEquals(ReturnT.FAIL_CODE, result.getCode());
            // Should contain failure messages from all addresses
            assertTrue(result.getMsg().contains("192.168.1.1:9999"));
            assertTrue(result.getMsg().contains("192.168.1.2:9999"));
        }
    }

    @Test
    public void shouldHandleExceptionDuringIdleBeat() {
        ExecutorRouteBusyover router = new ExecutorRouteBusyover();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(4);

        try (MockedStatic<XxlJobScheduler> mockedScheduler = mockStatic(XxlJobScheduler.class)) {
            // First address throws exception, second is idle
            ExecutorBiz mockExecutorBiz1 = mock(ExecutorBiz.class);
            when(mockExecutorBiz1.idleBeat(any(IdleBeatParam.class)))
                    .thenThrow(new RuntimeException("Connection timeout"));

            ExecutorBiz mockExecutorBiz2 = mock(ExecutorBiz.class);
            when(mockExecutorBiz2.idleBeat(any(IdleBeatParam.class)))
                    .thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "idle"));

            mockedScheduler
                    .when(() -> XxlJobScheduler.getExecutorBiz("192.168.1.1:9999"))
                    .thenReturn(mockExecutorBiz1);
            mockedScheduler
                    .when(() -> XxlJobScheduler.getExecutorBiz("192.168.1.2:9999"))
                    .thenReturn(mockExecutorBiz2);

            ReturnT<String> result = router.route(triggerParam, addressList);

            assertNotNull(result);
            assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
            assertEquals("192.168.1.2:9999", result.getContent());
            // Should contain exception message from first address
            assertTrue(result.getMsg().contains("192.168.1.1:9999"));
            assertTrue(result.getMsg().contains("Connection timeout"));
        }
    }

    @Test
    public void shouldPassCorrectJobIdToIdleBeat() {
        ExecutorRouteBusyover router = new ExecutorRouteBusyover();
        List<String> addressList = List.of("192.168.1.1:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(12345);

        try (MockedStatic<XxlJobScheduler> mockedScheduler = mockStatic(XxlJobScheduler.class)) {
            ExecutorBiz mockExecutorBiz = mock(ExecutorBiz.class);
            when(mockExecutorBiz.idleBeat(any(IdleBeatParam.class)))
                    .thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "idle"));

            mockedScheduler
                    .when(() -> XxlJobScheduler.getExecutorBiz(anyString()))
                    .thenReturn(mockExecutorBiz);
            router.route(triggerParam, addressList);

            // Verify that idleBeat was called with correct jobId
            verify(mockExecutorBiz).idleBeat(argThat(param -> param != null && param.getJobId() == 12345));
        }
    }

    @Test
    public void shouldHandleSingleAddressSuccess() {
        ExecutorRouteBusyover router = new ExecutorRouteBusyover();
        List<String> addressList = List.of("192.168.1.1:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(5);

        try (MockedStatic<XxlJobScheduler> mockedScheduler = mockStatic(XxlJobScheduler.class)) {
            ExecutorBiz mockExecutorBiz = mock(ExecutorBiz.class);
            when(mockExecutorBiz.idleBeat(any(IdleBeatParam.class)))
                    .thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "idle"));

            mockedScheduler
                    .when(() -> XxlJobScheduler.getExecutorBiz(anyString()))
                    .thenReturn(mockExecutorBiz);

            ReturnT<String> result = router.route(triggerParam, addressList);

            assertNotNull(result);
            assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
            assertEquals("192.168.1.1:9999", result.getContent());
        }
    }

    @Test
    public void shouldHandleSingleAddressFailure() {
        ExecutorRouteBusyover router = new ExecutorRouteBusyover();
        List<String> addressList = List.of("192.168.1.1:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(6);

        try (MockedStatic<XxlJobScheduler> mockedScheduler = mockStatic(XxlJobScheduler.class)) {
            ExecutorBiz mockExecutorBiz = mock(ExecutorBiz.class);
            when(mockExecutorBiz.idleBeat(any(IdleBeatParam.class)))
                    .thenReturn(new ReturnT<>(ReturnT.FAIL_CODE, "busy"));

            mockedScheduler
                    .when(() -> XxlJobScheduler.getExecutorBiz(anyString()))
                    .thenReturn(mockExecutorBiz);

            ReturnT<String> result = router.route(triggerParam, addressList);

            assertNotNull(result);
            assertEquals(ReturnT.FAIL_CODE, result.getCode());
            assertTrue(result.getMsg().contains("192.168.1.1:9999"));
        }
    }
}
