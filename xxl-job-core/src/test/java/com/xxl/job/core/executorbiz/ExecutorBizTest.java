package com.xxl.job.core.executorbiz;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.client.ExecutorBizClient;
import com.xxl.job.core.biz.model.IdleBeatParam;
import com.xxl.job.core.biz.model.KillParam;
import com.xxl.job.core.biz.model.LogParam;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.handler.IJobHandler;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * executor api test
 * <p>
 * Created by xuxueli on 17/5/12.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@SpringBootApplication
@Import(TestConfiguration.class)
public class ExecutorBizTest {
    private static final long FIXED_TIME_MILLIS = 1_700_000_000_000L;

    // admin-client
    private static String addressUrl = "http://127.0.0.1:9999/";
    private static String accessToken = null;
    private static int timeout = 3;

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void setup() {
        wireMockServer = new WireMockServer(options().port(18760));
        wireMockServer.start();
        configureFor(wireMockServer.port());

        stubFor(post(urlPathMatching("/api/initJobInfo"))
                .willReturn(ok().withBody("{\"code\":200,\"msg\":\"success\",\"content\":\"result\"}")));
    }

    @AfterAll
    public static void teardown() {
        wireMockServer.stop();
    }

    @BeforeEach
    void checkPort() {
        waitForPort("127.0.0.1", 9999, Duration.ofSeconds(10), Duration.ofMillis(100));
    }

    private void waitForPort(String host, int port, Duration timeout, Duration interval) {

        long deadline = System.nanoTime() + timeout.toNanos();
        Exception lastException = null;

        while (System.nanoTime() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), (int) interval.toMillis());
                // 连接成功，说明端口已就绪
                return;
            } catch (Exception e) {
                lastException = e;
                try {
                    Thread.sleep(interval.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("端口检测被中断", ie);
                }
            }
        }

        Assertions.fail("等待端口 " + port + " 启动超时（" + timeout.getSeconds() + "s）", lastException);
    }

    @Test
    public void beat() {
        ExecutorBiz executorBiz = new ExecutorBizClient(addressUrl, accessToken, timeout);
        // Act
        final ReturnT<String> retval = executorBiz.beat();

        // Assert result
        Assertions.assertNotNull(retval);
        Assertions.assertNull(retval.getContent());
        Assertions.assertNull(retval.getMsg());
        Assertions.assertEquals(200, retval.getCode());
    }

    @Test
    public void idleBeat() {
        ExecutorBiz executorBiz = new ExecutorBizClient(addressUrl, accessToken, timeout);

        final int jobId = 0;

        XxlJobExecutor.registJobHandler("ut-demoJobHandler", new IJobHandler() {
            @Override
            public void execute() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        final TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(jobId);
        triggerParam.setExecutorHandler("ut-demoJobHandler");
        triggerParam.setExecutorParams(null);
        triggerParam.setExecutorBlockStrategy(ExecutorBlockStrategyEnum.COVER_EARLY.name());
        triggerParam.setGlueType(GlueTypeEnum.BEAN.name());
        triggerParam.setGlueSource(null);
        triggerParam.setGlueUpdatetime(FIXED_TIME_MILLIS);
        triggerParam.setLogId(1);
        triggerParam.setLogDateTime(FIXED_TIME_MILLIS);
        executorBiz.run(triggerParam);

        // Act
        final ReturnT<String> retval = executorBiz.idleBeat(new IdleBeatParam(jobId));

        // Assert result
        Assertions.assertNotNull(retval);
        Assertions.assertNull(retval.getContent());
        Assertions.assertEquals(500, retval.getCode());
        Assertions.assertEquals("job thread is running or has trigger queue.", retval.getMsg());
    }

    @Test
    public void run() {
        ExecutorBiz executorBiz = new ExecutorBizClient(addressUrl, accessToken, timeout);

        // trigger data
        final TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(1);
        triggerParam.setExecutorHandler("demoJobHandler");
        triggerParam.setExecutorParams(null);
        triggerParam.setExecutorBlockStrategy(ExecutorBlockStrategyEnum.COVER_EARLY.name());
        triggerParam.setGlueType(GlueTypeEnum.BEAN.name());
        triggerParam.setGlueSource(null);
        triggerParam.setGlueUpdatetime(FIXED_TIME_MILLIS);
        triggerParam.setLogId(1);
        triggerParam.setLogDateTime(FIXED_TIME_MILLIS);

        // Act
        final ReturnT<String> retval = executorBiz.run(triggerParam);

        // Assert result
        Assertions.assertNotNull(retval);
    }

    @Test
    public void kill() {
        ExecutorBiz executorBiz = new ExecutorBizClient(addressUrl, accessToken, timeout);

        final int jobId = 0;

        // Act
        final ReturnT<String> retval = executorBiz.kill(new KillParam(jobId));

        // Assert result
        Assertions.assertNotNull(retval);
        Assertions.assertNull(retval.getContent());
        Assertions.assertEquals(200, retval.getCode());
        //        Assertions.assertNull(retval.getMsg());
    }

    @Test
    public void log() {
        ExecutorBiz executorBiz = new ExecutorBizClient(addressUrl, accessToken, timeout);

        final long logDateTim = 0L;
        final long logId = 0;
        final int fromLineNum = 0;

        // Act
        final ReturnT<LogResult> retval = executorBiz.log(new LogParam(logDateTim, logId, fromLineNum));

        // Assert result
        Assertions.assertNotNull(retval);
    }

    @Test
    public void registry() {
        await().atMost(3, SECONDS).untilAsserted(() -> verify(postRequestedFor(urlPathEqualTo("/api/initJobInfo"))));
    }
}
