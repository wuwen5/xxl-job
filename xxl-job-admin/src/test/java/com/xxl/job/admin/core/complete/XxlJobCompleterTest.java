package com.xxl.job.admin.core.complete;

import static org.junit.jupiter.api.Assertions.*;

import com.xxl.job.admin.AbstractTest;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogDao;
import com.xxl.job.core.context.XxlJobContext;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Unit tests for XxlJobCompleter.
 */
public class XxlJobCompleterTest extends AbstractTest {

    private static final Logger logger = LoggerFactory.getLogger(XxlJobCompleterTest.class);

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    XxlJobLogDao xxlJobLogDao;

    @Autowired
    XxlJobInfoDao xxlJobInfoDao;

    private XxlJobInfo originalJobInfo;
    private XxlJobLog originalJobLog;

    @BeforeEach
    public void setUp() {
        try {
            XxlJobAdminConfig config = XxlJobAdminConfig.getAdminConfig();
            assertNotNull(config, "XxlJobAdminConfig should be initialized for SpringBootTest");
        } catch (NullPointerException e) {
            logger.warn("XxlJobAdminConfig not initialized, test may fail", e);
        }

        // Clean up test data
        jdbcTemplate.execute("DELETE FROM xxl_job_log WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_info WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_group WHERE id > 0");

        // Insert test job group
        jdbcTemplate.execute("INSERT INTO xxl_job_group(id, app_name, title, address_type, address_list, update_time) "
                + "VALUES (1, 'test-executor', 'Test Executor', 0, NULL, NOW())");

        // Insert test job info
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_info(id, job_group, job_desc, add_time, update_time, author, schedule_type, "
                        + "misfire_strategy, glue_type, trigger_status, trigger_last_time, trigger_next_time) "
                        + "VALUES (1, 1, 'Test Job', NOW(), NOW(), 'tester', 'NONE', 'DO_NOTHING', 'BEAN', 0, 0, 0)");

        // Insert child job info
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_info(id, job_group, job_desc, add_time, update_time, author, schedule_type, "
                        + "misfire_strategy, glue_type, trigger_status, trigger_last_time, trigger_next_time, child_jobid) "
                        + "VALUES (2, 1, 'Child Job', NOW(), NOW(), 'tester', 'NONE', 'DO_NOTHING', 'BEAN', 0, 0, 0, '3')");

        // Insert another child job
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_info(id, job_group, job_desc, add_time, update_time, author, schedule_type, "
                        + "misfire_strategy, glue_type, trigger_status, trigger_last_time, trigger_next_time) "
                        + "VALUES (3, 1, 'Another Child Job', NOW(), NOW(), 'tester', 'NONE', 'DO_NOTHING', 'BEAN', 0, 0, 0)");
    }

    @AfterEach
    public void tearDown() {
        // Clean up test data
        jdbcTemplate.execute("DELETE FROM xxl_job_log WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_info WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_group WHERE id > 0");
    }

    /**
     * Test updateHandleInfoAndFinish with success handle code and short message
     */
    @Test
    public void testUpdateHandleInfoAndFinishSuccessShortMsg() {
        // Create job log with success code
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(1L);
        jobLog.setJobId(1);
        jobLog.setJobGroup(1);
        jobLog.setTriggerTime(new Date());
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(XxlJobContext.HANDLE_CODE_SUCCESS);
        jobLog.setHandleMsg("Success message");
        jobLog.setAlarmStatus(0);

        // Insert into DB
        xxlJobLogDao.save(jobLog);

        // Call the method
        int result = XxlJobCompleter.updateHandleInfoAndFinish(jobLog);

        // Verify
        assertEquals(1, result);
        logger.info("testUpdateHandleInfoAndFinishSuccessShortMsg: result={}", result);
    }

    /**
     * Test updateHandleInfoAndFinish with long message (>15000 chars) → should be truncated
     */
    @Test
    public void testUpdateHandleInfoAndFinishLongMsgTruncated() {
        // Create job log with very long message
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(2L);
        jobLog.setJobId(1);
        jobLog.setJobGroup(1);
        jobLog.setTriggerTime(new Date());
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(XxlJobContext.HANDLE_CODE_SUCCESS);

        // Create message longer than 15000 chars
        StringBuilder longMsg = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            longMsg.append("Line ").append(i).append(": This is a test message\n");
        }
        jobLog.setHandleMsg(longMsg.toString());
        jobLog.setAlarmStatus(0);

        // Insert into DB
        xxlJobLogDao.save(jobLog);

        int originalLength = jobLog.getHandleMsg().length();

        // Call the method
        int result = XxlJobCompleter.updateHandleInfoAndFinish(jobLog);

        // Verify message was truncated
        assertEquals(1, result);
        assertTrue(jobLog.getHandleMsg().length() <= 15000);
        assertTrue(jobLog.getHandleMsg().length() < originalLength);
        logger.info(
                "testUpdateHandleInfoAndFinishLongMsgTruncated: original={}, truncated={}",
                originalLength,
                jobLog.getHandleMsg().length());
    }

    /**
     * Test updateHandleInfoAndFinish with fail handle code
     */
    @Test
    public void testUpdateHandleInfoAndFinishFail() {
        // Create job log with fail code
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(3L);
        jobLog.setJobId(1);
        jobLog.setJobGroup(1);
        jobLog.setTriggerTime(new Date());
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(XxlJobContext.HANDLE_CODE_FAIL);
        jobLog.setHandleMsg("Failed message");
        jobLog.setAlarmStatus(0);

        // Insert into DB
        xxlJobLogDao.save(jobLog);

        // Call the method
        int result = XxlJobCompleter.updateHandleInfoAndFinish(jobLog);

        // Verify
        assertEquals(1, result);
        logger.info("testUpdateHandleInfoAndFinishFail: result={}", result);
    }

    /**
     * Test finishJob with child jobs - success scenario
     */
    @Test
    public void testFinishJobWithChildJobs() {
        // Create parent job with child jobs
        XxlJobInfo parentJob = xxlJobInfoDao.loadById(2);
        assertNotNull(parentJob);
        assertEquals("3", parentJob.getChildJobId());

        // Create job log for parent job with success
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(4L);
        jobLog.setJobId(2);
        jobLog.setJobGroup(1);
        jobLog.setTriggerTime(new Date());
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(XxlJobContext.HANDLE_CODE_SUCCESS);
        jobLog.setHandleMsg("Parent job success");
        jobLog.setAlarmStatus(0);

        // Insert into DB
        xxlJobLogDao.save(jobLog);

        // Call the method
        int result = XxlJobCompleter.updateHandleInfoAndFinish(jobLog);

        // Verify
        assertEquals(1, result);
        // The handleMsg should contain child job trigger message
        assertTrue(jobLog.getHandleMsg().contains(">>>>>>>>>>>")
                || jobLog.getHandleMsg().contains("<<<<<<<<<<"));
        logger.info("testFinishJobWithChildJobs: handleMsg={}", jobLog.getHandleMsg());
    }

    /**
     * Test finishJob when child job ID equals parent job ID (self-reference) → should skip
     */
    @Test
    public void testFinishJobSelfReferenceSkip() {
        // Update job to have self-reference child job
        jdbcTemplate.execute("UPDATE xxl_job_info SET child_jobid = '2' WHERE id = 2");

        XxlJobInfo parentJob = xxlJobInfoDao.loadById(2);
        assertEquals("2", parentJob.getChildJobId());

        // Create job log
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(5L);
        jobLog.setJobId(2);
        jobLog.setJobGroup(1);
        jobLog.setTriggerTime(new Date());
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(XxlJobContext.HANDLE_CODE_SUCCESS);
        jobLog.setHandleMsg("Parent job success");
        jobLog.setAlarmStatus(0);

        // Insert into DB
        xxlJobLogDao.save(jobLog);

        // Call the method
        int result = XxlJobCompleter.updateHandleInfoAndFinish(jobLog);

        // Verify - should succeed but not trigger child (self-reference skipped)
        assertEquals(1, result);
        logger.info("testFinishJobSelfReferenceSkip: handleMsg={}", jobLog.getHandleMsg());
    }

    /**
     * Test finishJob with invalid child job ID (non-numeric)
     */
    @Test
    public void testFinishJobInvalidChildJobId() {
        // Update job to have invalid child job ID
        jdbcTemplate.execute("UPDATE xxl_job_info SET child_jobid = 'invalid' WHERE id = 2");

        XxlJobInfo parentJob = xxlJobInfoDao.loadById(2);
        assertEquals("invalid", parentJob.getChildJobId());

        // Create job log
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(6L);
        jobLog.setJobId(2);
        jobLog.setJobGroup(1);
        jobLog.setTriggerTime(new Date());
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(XxlJobContext.HANDLE_CODE_SUCCESS);
        jobLog.setHandleMsg("Parent job success");
        jobLog.setAlarmStatus(0);

        // Insert into DB
        xxlJobLogDao.save(jobLog);

        // Call the method
        int result = XxlJobCompleter.updateHandleInfoAndFinish(jobLog);

        // Verify
        assertEquals(1, result);
        logger.info("testFinishJobInvalidChildJobId: handleMsg={}", jobLog.getHandleMsg());
    }

    /**
     * Test finishJob with empty child job ID
     */
    @Test
    public void testFinishJobEmptyChildJobId() {
        // Update job to have empty child job ID
        jdbcTemplate.execute("UPDATE xxl_job_info SET child_jobid = '' WHERE id = 2");

        XxlJobInfo parentJob = xxlJobInfoDao.loadById(2);
        assertEquals("", parentJob.getChildJobId());

        // Create job log
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(7L);
        jobLog.setJobId(2);
        jobLog.setJobGroup(1);
        jobLog.setTriggerTime(new Date());
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(XxlJobContext.HANDLE_CODE_SUCCESS);
        jobLog.setHandleMsg("Parent job success");
        jobLog.setAlarmStatus(0);

        // Insert into DB
        xxlJobLogDao.save(jobLog);

        // Call the method
        int result = XxlJobCompleter.updateHandleInfoAndFinish(jobLog);

        // Verify - no child job triggered
        assertEquals(1, result);
        logger.info("testFinishJobEmptyChildJobId: handleMsg={}", jobLog.getHandleMsg());
    }

    /**
     * Test finishJob with null child job ID
     */
    @Test
    public void testFinishJobNullChildJobId() {
        // Update job to have null child job ID
        jdbcTemplate.execute("UPDATE xxl_job_info SET child_jobid = NULL WHERE id = 2");

        XxlJobInfo parentJob = xxlJobInfoDao.loadById(2);
        assertNull(parentJob.getChildJobId());

        // Create job log
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(8L);
        jobLog.setJobId(2);
        jobLog.setJobGroup(1);
        jobLog.setTriggerTime(new Date());
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(XxlJobContext.HANDLE_CODE_SUCCESS);
        jobLog.setHandleMsg("Parent job success");
        jobLog.setAlarmStatus(0);

        // Insert into DB
        xxlJobLogDao.save(jobLog);

        // Call the method
        int result = XxlJobCompleter.updateHandleInfoAndFinish(jobLog);

        // Verify - no child job triggered
        assertEquals(1, result);
        logger.info("testFinishJobNullChildJobId: handleMsg={}", jobLog.getHandleMsg());
    }

    /**
     * Test finishJob when job info is null (job deleted)
     */
    @Test
    public void testFinishJobJobInfoNotFound() {
        // Create job log with non-existent job ID
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(9L);
        jobLog.setJobId(9999); // Non-existent
        jobLog.setJobGroup(1);
        jobLog.setTriggerTime(new Date());
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(XxlJobContext.HANDLE_CODE_SUCCESS);
        jobLog.setHandleMsg("Job not found");
        jobLog.setAlarmStatus(0);

        // Insert into DB
        xxlJobLogDao.save(jobLog);

        // Call the method - should not throw exception
        int result = XxlJobCompleter.updateHandleInfoAndFinish(jobLog);

        // Verify
        assertEquals(1, result);
        logger.info("testFinishJobJobInfoNotFound: result={}", result);
    }

    /**
     * Test finishJob with multiple child jobs (comma-separated)
     */
    @Test
    public void testFinishJobMultipleChildJobs() {
        // Update job to have multiple child jobs
        jdbcTemplate.execute("UPDATE xxl_job_info SET child_jobid = '1,3' WHERE id = 2");

        XxlJobInfo parentJob = xxlJobInfoDao.loadById(2);
        assertEquals("1,3", parentJob.getChildJobId());

        // Create job log
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(10L);
        jobLog.setJobId(2);
        jobLog.setJobGroup(1);
        jobLog.setTriggerTime(new Date());
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(XxlJobContext.HANDLE_CODE_SUCCESS);
        jobLog.setHandleMsg("Parent job success");
        jobLog.setAlarmStatus(0);

        // Insert into DB
        xxlJobLogDao.save(jobLog);

        // Call the method
        int result = XxlJobCompleter.updateHandleInfoAndFinish(jobLog);

        // Verify
        assertEquals(1, result);
        logger.info("testFinishJobMultipleChildJobs: handleMsg={}", jobLog.getHandleMsg());
    }

    /**
     * Test isNumeric helper method indirectly through finishJob
     */
    @Test
    public void testIsNumericWithMixedValidAndInvalid() {
        // Update job to have mixed valid/invalid child job IDs
        jdbcTemplate.execute("UPDATE xxl_job_info SET child_jobid = '1,abc,3' WHERE id = 2");

        XxlJobInfo parentJob = xxlJobInfoDao.loadById(2);
        assertEquals("1,abc,3", parentJob.getChildJobId());

        // Create job log
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(11L);
        jobLog.setJobId(2);
        jobLog.setJobGroup(1);
        jobLog.setTriggerTime(new Date());
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(XxlJobContext.HANDLE_CODE_SUCCESS);
        jobLog.setHandleMsg("Parent job success");
        jobLog.setAlarmStatus(0);

        // Insert into DB
        xxlJobLogDao.save(jobLog);

        // Call the method
        int result = XxlJobCompleter.updateHandleInfoAndFinish(jobLog);

        // Verify - should handle both valid and invalid IDs gracefully
        assertEquals(1, result);
        logger.info("testIsNumericWithMixedValidAndInvalid: handleMsg={}", jobLog.getHandleMsg());
    }

    /**
     * Test updateHandleInfoAndFinish with timeout handle code
     */
    @Test
    public void testUpdateHandleInfoAndFinishTimeout() {
        // Create job log with timeout code
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(12L);
        jobLog.setJobId(1);
        jobLog.setJobGroup(1);
        jobLog.setTriggerTime(new Date());
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(XxlJobContext.HANDLE_CODE_TIMEOUT);
        jobLog.setHandleMsg("Timeout message");
        jobLog.setAlarmStatus(0);

        // Insert into DB
        xxlJobLogDao.save(jobLog);

        // Call the method
        int result = XxlJobCompleter.updateHandleInfoAndFinish(jobLog);

        // Verify
        assertEquals(1, result);
        logger.info("testUpdateHandleInfoAndFinishTimeout: result={}", result);
    }

    /**
     * Test updateHandleInfoAndFinish with exactly 15000 chars message → should not be truncated
     */
    @Test
    public void testUpdateHandleInfoAndFinishExactLimit() {
        // Create job log with exactly 15000 char message
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(13L);
        jobLog.setJobId(1);
        jobLog.setJobGroup(1);
        jobLog.setTriggerTime(new Date());
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(XxlJobContext.HANDLE_CODE_SUCCESS);

        // Create message with exactly 15000 chars
        StringBuilder msg = new StringBuilder();
        while (msg.length() < 15000) {
            msg.append("x");
        }
        jobLog.setHandleMsg(msg.toString());
        jobLog.setAlarmStatus(0);

        // Insert into DB
        xxlJobLogDao.save(jobLog);

        assertEquals(15000, jobLog.getHandleMsg().length());

        // Call the method
        int result = XxlJobCompleter.updateHandleInfoAndFinish(jobLog);

        // Verify - should not be truncated (exactly at limit)
        assertEquals(1, result);
        assertEquals(15000, jobLog.getHandleMsg().length());
        logger.info(
                "testUpdateHandleInfoAndFinishExactLimit: length={}",
                jobLog.getHandleMsg().length());
    }

    /**
     * Test updateHandleInfoAndFinish with 15001 chars message → should be truncated to 15000
     */
    @Test
    public void testUpdateHandleInfoAndFinishJustOverLimit() {
        // Create job log with 15001 char message
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(14L);
        jobLog.setJobId(1);
        jobLog.setJobGroup(1);
        jobLog.setTriggerTime(new Date());
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(XxlJobContext.HANDLE_CODE_SUCCESS);

        // Create message with 15001 chars
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < 15001; i++) {
            msg.append("x");
        }
        jobLog.setHandleMsg(msg.toString());
        jobLog.setAlarmStatus(0);

        // Insert into DB
        xxlJobLogDao.save(jobLog);

        assertEquals(15001, jobLog.getHandleMsg().length());

        // Call the method
        int result = XxlJobCompleter.updateHandleInfoAndFinish(jobLog);

        // Verify - should be truncated to 15000
        assertEquals(1, result);
        assertEquals(15000, jobLog.getHandleMsg().length());
        logger.info(
                "testUpdateHandleInfoAndFinishJustOverLimit: truncated from 15001 to {}",
                jobLog.getHandleMsg().length());
    }
}
