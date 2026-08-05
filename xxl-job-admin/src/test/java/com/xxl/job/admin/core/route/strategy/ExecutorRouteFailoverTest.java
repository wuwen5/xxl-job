package com.xxl.job.admin.core.route.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for ExecutorRouteFailover
 */
public class ExecutorRouteFailoverTest {

    private static XxlJobAdminConfig originalAdminConfig;

    @BeforeAll
    static void setUp() throws Exception {
        originalAdminConfig = XxlJobAdminConfig.getAdminConfig();
        XxlJobAdminConfig xxlJobAdminConfig = new XxlJobAdminConfig();
        xxlJobAdminConfig.afterPropertiesSet();
    }

    @AfterAll
    static void tearDown() throws Exception {
        XxlJobAdminConfig xxlJobAdminConfig = originalAdminConfig;
        if (xxlJobAdminConfig != null) {
            xxlJobAdminConfig.afterPropertiesSet();
        }
    }

    @Test
    public void shouldRouteToFirstHealthyAddress() {
        ExecutorRouteFailover router = new ExecutorRouteFailover();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(1);

        // Mock ExecutorBiz to return success for first address
        try (MockedStatic<XxlJobScheduler> mockedScheduler = mockStatic(XxlJobScheduler.class)) {
            ExecutorBiz mockExecutorBiz = mock(ExecutorBiz.class);
            when(mockExecutorBiz.beat()).thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success"));

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
    public void shouldSkipUnhealthyAddressAndRouteToNext() {
        ExecutorRouteFailover router = new ExecutorRouteFailover();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(2);

        try (MockedStatic<XxlJobScheduler> mockedScheduler = mockStatic(XxlJobScheduler.class)) {
            // First address fails, second succeeds
            ExecutorBiz mockExecutorBiz1 = mock(ExecutorBiz.class);
            when(mockExecutorBiz1.beat()).thenReturn(new ReturnT<>(ReturnT.FAIL_CODE, "busy"));

            ExecutorBiz mockExecutorBiz2 = mock(ExecutorBiz.class);
            when(mockExecutorBiz2.beat()).thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success"));

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
            // Should contain beat results from both addresses
            assertTrue(result.getMsg().contains("192.168.1.1:9999"));
            assertTrue(result.getMsg().contains("192.168.1.2:9999"));
        }
    }

    @Test
    public void shouldReturnFailureWhenAllAddressesAreUnhealthy() {
        ExecutorRouteFailover router = new ExecutorRouteFailover();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(3);

        try (MockedStatic<XxlJobScheduler> mockedScheduler = mockStatic(XxlJobScheduler.class)) {
            ExecutorBiz mockExecutorBiz = mock(ExecutorBiz.class);
            when(mockExecutorBiz.beat()).thenReturn(new ReturnT<>(ReturnT.FAIL_CODE, "unavailable"));

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
    public void shouldHandleExceptionDuringBeat() {
        ExecutorRouteFailover router = new ExecutorRouteFailover();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(4);

        try (MockedStatic<XxlJobScheduler> mockedScheduler = mockStatic(XxlJobScheduler.class)) {
            // First address throws exception, second succeeds
            ExecutorBiz mockExecutorBiz1 = mock(ExecutorBiz.class);
            when(mockExecutorBiz1.beat()).thenThrow(new RuntimeException("Connection timeout"));

            ExecutorBiz mockExecutorBiz2 = mock(ExecutorBiz.class);
            when(mockExecutorBiz2.beat()).thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success"));

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
    public void shouldHandleSingleAddressSuccess() {
        ExecutorRouteFailover router = new ExecutorRouteFailover();
        List<String> addressList = List.of("192.168.1.1:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(5);

        try (MockedStatic<XxlJobScheduler> mockedScheduler = mockStatic(XxlJobScheduler.class)) {
            ExecutorBiz mockExecutorBiz = mock(ExecutorBiz.class);
            when(mockExecutorBiz.beat()).thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success"));

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
        ExecutorRouteFailover router = new ExecutorRouteFailover();
        List<String> addressList = List.of("192.168.1.1:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(6);

        try (MockedStatic<XxlJobScheduler> mockedScheduler = mockStatic(XxlJobScheduler.class)) {
            ExecutorBiz mockExecutorBiz = mock(ExecutorBiz.class);
            when(mockExecutorBiz.beat()).thenReturn(new ReturnT<>(ReturnT.FAIL_CODE, "unavailable"));

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
