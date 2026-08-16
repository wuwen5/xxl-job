# AGENTS.md

## 项目结构

- Maven 多模块：`xxl-job-core`（执行器客户端库）、`xxl-job-admin`（Spring Boot 3 调度中心）、`xxl-job-executor-samples`（Spring Boot + 无框架示例）。
- `xxl-job-admin-ui` 是**纯前端项目**（Vue 3 + TypeScript + Vite），不是 Maven 子模块。它由 `xxl-job-admin` 的 `frontend-maven-plugin` 在 `generate-resources` 阶段自动构建，`dist/` 输出打包进 admin jar 的 `static/` 目录。独立前端开发指南见 `xxl-job-admin-ui/AGENTS.md`。
- GAV：`io.github.wuwen5.xxl-job` / `2.5.3-SNAPSHOT`（在根 `pom.xml` 定义，不要在子模块修改版本）。
- 兼容目标是 xxl-job 2.5.0；重构保留原版 handler 约定、数据库表结构和管理端 API。

## 双 JDK 策略（容易搞错，不要统一）

- 只有 `xxl-job-admin` 是 **JDK 17**（Spring Boot 3.5 / Jakarta EE，`spring-boot.version=3.5.14`）。
- `xxl-job-core` 编译目标 **JDK 8**（`maven.compiler.source/target=1.8`）。
- `xxl-job-executor-samples` 同样走 **JDK 8**：`xxl-job-executor-sample-frameless` 无 compiler 覆盖、继承根 POM 的 `1.8`；`xxl-job-executor-sample-springboot` 用 **Spring Boot 2.7.18**（javax.*，非 Jakarta）而非 3.x。
- 不要在 `xxl-job-core` 和 samples 中引入 Java 9+ API（不能用 `List.of`、`var`、`jakarta.*`）。
- CI 矩阵在 Ubuntu 上跑 JDK 17 和 21；本地最低要求 JDK 17 来构建 admin 模块。不需要本地 JDK 8 工具链——编译器插件生成 1.8 字节码。

## 构建与验证

- 使用 Wrapper：`./mvnw -B clean verify --file pom.xml`。不要调用系统 `mvn`——版本在 `pom.xml` 中锁定。
- 默认构建在 `process-sources` 阶段运行 **Spotless 检查**（palantir-java-format）；格式错误会构建失败。用 `./mvnw spotless:apply` 自动修复。
- 构建 `xxl-job-admin` 时，`frontend-maven-plugin` 会在 `generate-resources` 阶段自动在 `xxl-job-admin-ui/` 下执行 `npm install` + `npm run build`，然后将 `dist/` 打包进 jar 的 `static/`。首次构建或前端依赖变更时需要联网下载 Node/npm。
- 发布构建（Maven Central）用 `-P release -DperformRelease=true`；该 profile 只构建 `xxl-job-core`，用 GPG 签名，跳过测试，通过 `central-publishing-maven-plugin` 发布。没有配置 GPG 时不要在本地执行。

## 运行测试

- 仅单元测试：`./mvnw -pl xxl-job-core,xxl-job-admin test`。
- 单个类：`./mvnw -pl xxl-job-admin -Dtest=ClassName test`。
- PostgreSQL DAO 测试在 `xxl-job-admin/src/test/java/com/xxl/job/admin/dao/pg/*`，继承 `AbstractPostgreSQLTest`，会启动 Testcontainers `postgres:16-alpine`——需要 Docker 运行。
- H2 切片测试使用 `test` profile（`application-test.properties`、`schema.sql`）。
- E2E 测试**不在** Maven 流程中——它们在 `e2e/`（Playwright 1.44），通过模块化 Compose 文件运行。例如 MySQL：`docker compose -f compose.yml -f compose.mysql.yml -f compose.e2e.yml up --build --abort-on-container-exit --exit-code-from e2e-tests`；PostgreSQL 替换为 `-f compose.postgres.yml`。`.github/workflows/e2e.yml` 中的 Maven 构建步骤用 `-Dmaven.test.skip=true`；e2e 流水线和 CI 流水线是独立的，且支持 MySQL / PostgreSQL 矩阵测试。
- E2E 测试编写与 playwright-cli 使用指南见 `e2e/AGENTS.md`。

## 运行时默认值

- Admin Web 端口：`8080`，**上下文路径 `/`**。Actuator 在 `9001`（探针启用，只暴露 `health,info`；readiness 包含 `db`）。E2E 与 Compose 健康检查都轮询 `/actuator/health/readiness`。
- API 基础路径：`/admin-api/v1/...`（前端 Vite proxy 默认目标 `http://localhost:8080`，见 `xxl-job-admin-ui/vite.config.ts`）。Docker Compose 中执行器 sample 以 `XXL_JOB_ADMIN_ADDRESSES=http://xxl-job-admin:8080` 注册（监听 `8081`）。
- 默认登录：`admin / 123456`（由 SQL 初始化脚本种子数据生成，在 `e2e/tests/login.spec.ts` 中断言）。
- 默认 MySQL JDBC URL 硬编码在 `application.properties`（`jdbc:mysql://127.0.0.1:3306/xxl_job`），通过 `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` 环境变量覆盖（Compose 即用此方式注入）。
- 多数据库 schema 在 `doc/db/`：`tables_xxl_job.sql`（MySQL）、`tables_xxl_job_pg.sql`、`tables_xxl_job_oracle.sql`、`tables_xxl_job_dm.sql`（达梦）。测试 schema：`xxl-job-admin/src/test/resources/schema.sql`（H2/MySQL 模式）和 `schema-postgresql.sql`。

## 代码风格与约定

- 代码格式为 palantir-java-format 2.38.0（4 空格缩进、120 列、无 tab）——编辑后重新运行 `spotless:apply`；不要手动格式化。
- Lombok 广泛使用。`xxl-job-core` 和 `xxl-job-admin` 使用 `@Slf4j` 注解；`xxl-job-executor-samples` 中使用手动 `Logger` 字段。添加日志时参照所在文件的现有模式。
- `xxl-job-core` 包根：`com.xxl.job.core.*`。`xxl-job-admin` 包根：`com.xxl.job.admin.*`。不要跨包根移动类。
- `.gitattributes` 将 `*.js`、`*.css`、`*.html`、`*.ftl` 标记为 Java（这是有意为之——它们是 admin 内部的 Freemarker/JS 模板）。
- `mvn versions:set` 后本地会出现 `pom.xml.versionsBackup` 文件；它们不会被提交（release workflow 运行 `versions:commit` 来清除）。
- `applogs/` 和 `logs/` 是运行时输出目录，不是源码。

## 发布与部署

- `release.yml` 是 `workflow_dispatch`。必需输入：`release_version`、`next_snapshot`。必需 secrets：`GPG_PRIVATE_KEY`、`GPG_PASSPHRASE`、`OSSRH_USERNAME`、`OSSRH_PASSWORD`、`MAVEN_CENTRAL_TOKEN`、`MAVEN_USERNAME`、`SONAR_TOKEN`、`CODECOV_TOKEN`。
- `xxl-job-admin` 和 `xxl-job-executor-samples` 设置 `skip_maven_deploy=true`；只有 `xxl-job-core` 发布到 Maven Central。
- Dependabot 配置为 Maven 和 GitHub Actions（每日，上限 20 个 PR）。

## 容易犯的错误

- 不要把 `xxl-job-core` 升级到 JDK 17——这会破坏 `README.md` 中"JDK 8 客户端"的承诺。
- 修改 admin 代码后，不要在没有 Docker 的情况下不加 `-DskipTests` 就 `mvn install` 父 POM——PostgreSQL 容器会在第一个 DAO 测试时启动。
- 不要把 `e2e/` 当成 Maven 子项目；它有自己的 `package.json`，通过 Docker Compose 运行。
- 根目录的 `docker-compose-e2e.yml` 是**遗留文件**（旧的单文件 MySQL 栈）；当前 e2e 用模块化 `compose.yml` + `compose.mysql.yml`/`compose.postgres.yml` + `compose.e2e.yml`，不要用它来跑测试。
- admin 的默认 Spring profile 未设置——测试时切换到 `test`（H2）或 `pgtest`（Testcontainers PG）；`pgtest` profile 的 `xxl.job.accessToken=` 由 `application-pgtest.properties` 置空，数据源与 `spring.sql.init.schema-locations=classpath:schema-postgresql.sql` 由 `AbstractPostgreSQLTest` 的 `@DynamicPropertySource` 注入。
- Sonar 排除已经过滤了 `**/src/test/**` 和 `xxl-job-executor-samples/**`；覆盖率报告因此不包含 samples。

## 编码强制规则

- **Import**：禁止 `.*` 通配符导入
- **Javadoc**：所有类 + public 方法 + 字段 javadoc；
- **最小改动 / 复用优先**：优先复用已有 Service/API，保持原有风格
- 优先 Lambda / Stream（简单场景），复杂逻辑提取为普通方法

## Agent 工作原则

### 需求理解

- 不要假设不确定的信息；如果关键信息缺失，先询问。
- 遇到无法确定的需求或上下文时，停止并请求澄清。
- 如果发现更简单或更安全的实现方案，先指出，而不是直接执行复杂方案。

### 变更范围

- 只实现用户明确要求的功能，不额外扩展需求。
- 不要为一次性需求增加不必要的抽象、配置项或扩展能力。
- 不要为理论上不会发生的场景增加复杂错误处理。
- 发现与当前任务无关的问题时，指出即可，不要擅自修复。
- 只修改完成当前任务所必需的代码。
- 如果发现必须扩大修改范围才能正确完成任务，先说明原因。

### 验证

- 修复 Bug：先尽可能复现问题，再修改并验证修复。
- 添加功能：补充必要测试，并验证测试通过。
- 重构代码：确保行为没有发生非预期变化，并运行相关测试。
- 修改完成后主动运行适当的测试、检查或构建，不要仅凭代码看起来正确就认为任务完成。