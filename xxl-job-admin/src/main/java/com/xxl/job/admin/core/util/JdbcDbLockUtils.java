package com.xxl.job.admin.core.util;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author wuwen
 */
@Slf4j
public final class JdbcDbLockUtils {

    private JdbcDbLockUtils() {}

    private static final String INSERT_LOCK_SQL = "insert into xxl_job_lock (lock_name) values(?) ";

    private static final String LOCK_SQL = "select lock_name from xxl_job_lock where lock_name = ? for update";

    public static void executeWithDbLock(String lockName, Runnable run) {
        executeWithDbLock(lockName, false, true, run);
    }

    public static void executeWithDbLock(String lockName, boolean insertLock, boolean waitForLock, Runnable run) {

        TransactionTemplate txTemplate =
                new TransactionTemplate(XxlJobAdminConfig.getAdminConfig().getPlatformTransactionManager());
        JdbcTemplate jdbcTemplate = XxlJobAdminConfig.getAdminConfig().getJdbcTemplate();

        try {
            txTemplate.execute(status -> {
                try {
                    // 目前仅在执行器启动存在自动注册任务时，才会插入锁记录
                    if (insertLock) {
                        jdbcTemplate.update(INSERT_LOCK_SQL, lockName);
                    }
                } catch (DuplicateKeyException e) {
                    log.debug("executeWithDbLock duplicate key, lockName={}", lockName);
                }
                jdbcTemplate.queryForObject(LOCK_SQL + (waitForLock ? "" : " nowait"), String.class, lockName);
                run.run();
                return null;
            });
        } catch (PessimisticLockingFailureException e) {
            if (!waitForLock) {
                // NOWAIT 获取不到锁，属于正常情况
                log.debug("Could not acquire db lock, lockName={}", lockName);
                return;
            }
            throw e;
        } catch (DataAccessException e) {
            SQLException sqlException = findSqlException(e);
            if (sqlException != null && isLockNotAvailable(sqlException)) {
                log.debug("executeWithDbLock could not acquire lock, lockName={}", lockName);
            } else {
                log.warn("executeWithDbLock sql error, lockName={}", lockName, e);
                throw e;
            }
        }
    }

    static boolean isLockNotAvailable(SQLException e) {
        int code = e.getErrorCode();
        String state = e.getSQLState();

        // Oracle
        return code == 54
                // MySQL
                || code == 1205
                || code == 3572
                // PostgreSQL
                || "55P03".equals(state);
    }

    private static SQLException findSqlException(Throwable t) {
        while (t != null) {
            if (t instanceof SQLException sqlException) {
                return sqlException;
            }
            t = t.getCause();
        }
        return null;
    }
}
