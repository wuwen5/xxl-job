# xxl-job-lite

XXL-JOB-Lite — a deep refactor of XXL-Job 2.5.0 focused on the core distributed scheduling capabilities, with the redundant extensions removed. Lightweight, stable, and faithful to the original usage.

Upstream: [xuxueli/xxl-job](https://github.com/xuxueli/xxl-job)

中文版：[README.md](./README.md)

[![CI](https://github.com/wuwen5/xxl-job-lite/actions/workflows/ci.yml/badge.svg)](https://github.com/wuwen5/xxl-job-lite/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=wuwen5_xxl-job-lite&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=wuwen5_xxl-job-lite)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=wuwen5_xxl-job-lite&metric=coverage)](https://sonarcloud.io/summary/new_code?id=wuwen5_xxl-job-lite)
<img src="https://img.shields.io/badge/xxl--job--core-JDK%208%2B-339933?logo=openjdk&logoColor=white" alt="xxl-job-core JDK 8+">
<img src="https://img.shields.io/badge/xxl--job--admin-JDK%2017%2B-007396?logo=openjdk&logoColor=white" alt="xxl-job-admin JDK 17+">
[![Maven Central](https://img.shields.io/maven-central/v/io.github.wuwen5.xxl-job/xxl-job-core.svg)](https://central.sonatype.com/artifact/io.github.wuwen5.xxl-job/xxl-job-core)
[![GitHub release](https://img.shields.io/github/release/wuwen5/xxl-job-lite.svg)](https://github.com/wuwen5/xxl-job-lite/releases)
[![License: GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](http://www.gnu.org/licenses/gpl-3.0.html)
[![zread](https://img.shields.io/badge/Ask_Zread-_.svg?style=flat&color=00b0aa&labelColor=000000&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB3aWR0aD0iMTYiIGhlaWdodD0iMTYiIHZpZXdCb3g9IjAgMCAxNiAxNiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTQuOTYxNTYgMS42MDAxSDIuMjQxNTZDMS44ODgxIDEuNjAwMSAxLjYwMTU2IDEuODg2NjQgMS42MDE1NiAyLjI0MDFWNC45NjAxQzEuNjAxNTYgNS4zMTM1NiAxLjg4ODEgNS42MDAxIDIuMjQxNTYgNS42MDAxSDQuOTYxNTZDNS4zMTUwMiA1LjYwMDEgNS42MDE1NiA1LjMxMzU2IDUuNjAxNTYgNC45NjAxVjIuMjQwMUM1LjYwMTU2IDEuODg2NjQgNS4zMTUwMiAxLjYwMDEgNC45NjE1NiAxLjYwMDFaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik00Ljk2MTU2IDEwLjM5OTlIMi4yNDE1NkMxLjg4ODEgMTAuMzk5OSAxLjYwMTU2IDEwLjY4NjQgMS42MDE1NiAxMS4wMzk5VjEzLjc1OTlDMS42MDE1NiAxNC4xMTM0IDEuODg4MSAxNC4zOTk5IDIuMjQxNTYgMTQuMzk5OUg0Ljk2MTU2QzUuMzE1MDIgMTQuMzk5OSA1LjYwMTU2IDE0LjExMzQgNS42MDE1NiAxMy43NTk5VjExLjAzOTlDNS42MDE1NiAxMC42ODY0IDUuMzE1MDIgMTAuMzk5OSA0Ljk2MTU2IDEwLjM5OTlaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik0xMy43NTg0IDEuNjAwMUgxMS4wMzg0QzEwLjY4NSAxLjYwMDEgMTAuMzk4NCAxLjg4NjY0IDEwLjM5ODQgMi4yNDAxVjQuOTYwMUMxMC4zOTg0IDUuMzEzNTYgMTAuNjg1IDUuNjAwMSAxMS4wMzg0IDUuNjAwMUgxMy43NTg0QzE0LjExMTkgNS42MDAxIDE0LjM5ODQgNS4zMTM1NiAxNC4zOTg0IDQuOTYwMVYyLjI0MDFDMTQuMzk4NCAxLjg4NjY0IDE0LjExMTkgMS42MDAxIDEzLjc1ODQgMS42MDAxWiIgZmlsbD0iI2ZmZiIvPgo8cGF0aCBkPSJNNCAxMkwxMiA0TDQgMTJaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik00IDEyTDEyIDQiIHN0cm9rZT0iI2ZmZiIgc3Ryb2tlLXdpZHRoPSIxLjUiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIvPgo8L3N2Zz4K&logoColor=ffffff)](https://zread.ai/wuwen5/xxl-job-lite)

---

## Table of Contents

- [Introduction](#introduction)
- [Relationship with the Upstream Project](#relationship-with-the-upstream-project)
- [Design Goals](#design-goals)
- [Highlights](#highlights)
- [Requirements](#requirements)
- [Database Support](#database-support)
- [Quick Start](#quick-start)
  - [A. Docker Compose (recommended for trying it out)](#a-docker-compose-recommended-for-trying-it-out)
  - [B. Build and Run from Source](#b-build-and-run-from-source)
  - [C. Use the Executor in Your Application](#c-use-the-executor-in-your-application)
- [Configuration](#configuration)
- [Compatibility](#compatibility)
- [Development & Testing](#development--testing)
- [Acknowledgements](#acknowledgements)
- [License](#license)
- [Contributing](#contributing)

---

## Introduction

`xxl-job-lite` is a deep refactor of `xxl-job 2.5.0`. The goal is to keep and strengthen the core distributed scheduling capability, simplify the project structure, and remove non-core components, in order to improve maintainability, extensibility, and ecosystem compatibility. The handler conventions, scheduling model, database schema, and admin API are kept aligned with xxl-job 2.5.0.

## Relationship with the Upstream Project

- Forked from [xuxueli/xxl-job](https://github.com/xuxueli/xxl-job), with 2.5.0 as the refactor baseline.
- Original author: [@xuxueli](https://github.com/xuxueli), with many community contributors.
- Notable adjustments on top of 2.5.0: drop the 3.0+ extensions (e.g. AI executor / AI jobs), introduce a dual-JDK strategy (client JDK 8, admin JDK 17), remove xxl-branded in-house components, and adopt mainstream open-source building blocks (Spring, Netty, Gson, MyBatis, etc.).

## Design Goals

- **Lightweight**: keep only the scheduling essentials, drop in-house utilities and non-core features.
- **Stability first**: inherit the mature 2.5.x scheduling model (sharding, routing, failure retry, block strategies, etc.).
- **Ecosystem friendly**: built on mainstream open-source components, easy to integrate with Spring Boot / Cloud.
- **Gradual migration**: APIs, handler conventions, and the database schema are kept compatible with 2.5.x where possible.
- **Dual JDK strategy**:
  - `xxl-job-core` stays on JDK 8, so business applications can adopt it without upgrading their runtime.
  - `xxl-job-admin` runs on JDK 17, to use modern language features and align with mainstream infrastructure.

## Highlights

**🔧 Core Capabilities**
- ✂️ **Trimmed dependencies**: built on mainstream components (Netty / Gson / MyBatis / Spring); no more xxl-branded in-house modules.
- ☕ **Dual JDK strategy**: client on JDK 8, admin on JDK 17 — balancing ecosystem compatibility with a modern infrastructure baseline.
- 🗄️ **Multi-database support**: MySQL, PostgreSQL, Oracle, and DaMeng (DM) are all supported as the admin's backing store. The DAO layer does not rely on database-specific syntax.
- ⚡ **Auto-registered jobs**: declare cron / fixedRate via `@XxlJob` annotation, jobs are registered automatically on startup — code as configuration.

**🖥️ Admin**
- 🎨 **Vue 3 frontend rewrite**: Vue 3 + TypeScript + Vite + Element Plus for a modern development experience.
- 🔌 **RESTful API refactor**: unified `/admin-api/v1/` endpoint style, supporting frontend-backend separation.
- 🌍 **Internationalization**: built-in Simplified Chinese, Traditional Chinese, and English with one-click switching.

**🧪 Quality Assurance**
- ✅ **E2E automated testing**: Playwright-based end-to-end tests covering login, job management, executor management, and other core workflows.
- 📊 **Code quality**: SonarQube static analysis with 3K+ issues resolved, continuous test coverage tracking.
- 🎯 **Code formatting**: Spotless + palantir-java-format for unified code style.

**🔗 Integration**
- 🔧 **Easy to integrate**: client side fits naturally into the Spring ecosystem; samples cover both Spring Boot and frameless usage.
- 🔍 **Service discovery extension**: pluggable `ServiceAddressResolver` to integrate with registries such as Nacos or Consul.

### Tech Stack Overview

| Component | Tech Stack |
|-----------|------------|
| Frontend | Vue 3 + TypeScript + Vite + Element Plus |
| Backend | Spring Boot 3.5 + MyBatis + Netty |
| Database | MySQL / PostgreSQL / Oracle / DaMeng |
| Testing | JUnit 5 + Playwright + JaCoCo |
| CI/CD | GitHub Actions + SonarCloud + Codecov |

### UI Preview

| Dashboard | Job Management |
|:---------:|:--------------:|
| ![Dashboard](doc/images/dashboard.jpg) | ![Job List](doc/images/jobinfo.jpg) |

| Create Job | Execution Logs |
|:----------:|:--------------:|
| ![Create Job](doc/images/jobinfo_new.jpg) | ![Execution Logs](doc/images/joblog.jpg) |

## Requirements

| Module | Description | Min JDK |
| --- | --- | --- |
| `xxl-job-core` | Executor client library (added to your business application) | JDK 8 |
| `xxl-job-admin` | Scheduling center (admin) | JDK 17 |
| `xxl-job-executor-samples` | Spring Boot and frameless executor samples | JDK 17 |

Additional:

- **Build tool**: use the bundled Maven Wrapper (`./mvnw`); no local Maven install required.
- **Docker** (optional): required only if you take the Docker Compose path.
- **Database**: for local development, MySQL 8.x works out of the box. The admin defaults to `127.0.0.1:3306`; override via environment variables.

## Database Support

The admin supports the following databases. Initialization scripts are under `doc/db/`:

| Database | Init script | Driver |
| --- | --- | --- |
| MySQL | `tables_xxl_job.sql` | `com.mysql:mysql-connector-j` |
| PostgreSQL | `tables_xxl_job_pg.sql` | `org.postgresql:postgresql` |
| Oracle | `tables_xxl_job_oracle.sql` | `com.oracle.database.jdbc:ojdbc8` |
| DaMeng (DM) | `tables_xxl_job_dm.sql` | `com.dameng:DmJdbcDriver18` |
| H2 | `xxl-job-admin/src/test/resources/schema.sql` | Tests only |

To switch databases, swap the JDBC driver and the matching DDL script. The DAO and service layers do not rely on database-specific syntax.

## Quick Start

### A. Docker Compose (recommended for trying it out)

The repo ships modular Compose files that can be combined to choose your database backend.

**Start with MySQL**

```bash
# From the repo root
docker compose \
  -f compose.yml \
  -f compose.mysql.yml \
  up --build xxl-job-admin executor-sample
```

**Start with PostgreSQL**

```bash
docker compose \
  -f compose.yml \
  -f compose.postgres.yml \
  up --build xxl-job-admin executor-sample
```

Once it is up:

- Admin UI: [http://localhost:8080](http://localhost:8080)
- Actuator health: [http://localhost:9001/actuator/health/readiness](http://localhost:9001/actuator/health/readiness)
- Sample executor: port `8081`

Default login: `admin / 123456` (seeded by the database init script).

### B. Build and Run from Source

```bash
# 1. Build with the bundled Maven Wrapper (JDK 17+)
./mvnw -B clean package -DskipTests --file pom.xml

# 2. Start the admin
java -jar xxl-job-admin/target/*.jar
```

Then open [http://localhost:8080](http://localhost:8080) and log in with `admin / 123456`.

Before starting, make sure:

- A `mysql` instance is running locally with a `xxl_job` database (apply `doc/db/tables_xxl_job.sql` first), **or**
- You point the admin at another database via `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`.

### C. Use the Executor in Your Application

Add the executor client to your business app's `pom.xml`:

```xml
<dependency>
    <groupId>io.github.wuwen5.xxl-job</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.5.2</version>
</dependency>
```

Configure the executor (`application.yml` / `application.properties`):

```properties
### Admin address
xxl.job.admin.addresses=http://127.0.0.1:8080
### Access token, must match xxl.job.accessToken on the admin side
xxl.job.admin.accessToken=default_token
### Executor app name (used for registration)
xxl.job.executor.appname=xxl-job-executor-sample
### Executor port (used for admin-to-executor callbacks)
xxl.job.executor.port=9999
### Executor log directory
xxl.job.executor.logpath=applogs/xxl-job/jobhandler
```

Declare a job with `@XxlJob`:

```java
@XxlJob(value = "demoJobHandler", cron = "0/10 * * * * ?", desc = "Demo job")
public void demoJobHandler() {
    System.out.println("Hello XXL Job Lite");
}
```

Full samples: `xxl-job-executor-samples/xxl-job-executor-sample-springboot` (Spring Boot) and `xxl-job-executor-samples/xxl-job-executor-sample-frameless` (frameless).

## Configuration

Key admin defaults (see `xxl-job-admin/src/main/resources/application.properties`):

| Key | Default | Description |
| --- | --- | --- |
| `server.port` | `8080` | Web port |
| `server.servlet.context-path` | `/` | Admin context path |
| `management.server.port` | `9001` | Actuator port |
| `management.endpoints.web.exposure.include` | `health,info` | Actuator endpoints exposed |
| `management.endpoint.health.probes.enabled` | `true` | Enables liveness / readiness probes |
| `xxl.job.accessToken` | `default_token` | Shared secret between admin and executor. **Change in production.** |
| `xxl.job.i18n` | `zh_CN` | Admin UI language. Allowed: `zh_CN` / `zh_TC` / `en` |
| `xxl.job.logretentiondays` | `30` | Job log retention in days |
| `spring.datasource.url` | `jdbc:mysql://127.0.0.1:3306/xxl_job?...` | Data source URL — override via `SPRING_DATASOURCE_URL` in production |

Common executor keys (see `xxl-job-executor-samples/xxl-job-executor-sample-springboot/src/main/resources/application.properties`):

| Key | Description |
| --- | --- |
| `xxl.job.admin.addresses` | Admin address (comma-separated for multiple) |
| `xxl.job.admin.accessToken` | Must match the admin's `xxl.job.accessToken` |
| `xxl.job.executor.appname` | Executor app name |
| `xxl.job.executor.address` | Externally visible address as `ip:port`; falls back to `ip` + `port` if empty |
| `xxl.job.executor.ip` | Executor IP, useful on multi-NIC hosts |
| `xxl.job.executor.port` | Executor port (`0` for auto-assigned) |
| `xxl.job.executor.logpath` | Job run log directory |
| `xxl.job.executor.logretentiondays` | Log retention in days |

## Compatibility

Based on `xxl-job 2.5.0`, the refactored version maintains compatibility as follows:

**✅ Compatible**
- **Executor interfaces**: Core RPC interfaces like `AdminBiz` and `ExecutorBiz` remain unchanged — existing executors can connect directly.
- **Handler conventions**: `@XxlJob` annotation, `IJobHandler` lifecycle, GLUE mode, and other usage patterns are unchanged.
- **Database schema**: The 8 core tables remain compatible — existing data can be used as-is.
- **Operation habits**: Task configuration, scheduling strategies, routing rules, blocking strategies, and other core concepts and workflows are unchanged.

**🔄 Refactored**
- **Admin API**: RESTful refactor to `/admin-api/v1/` endpoints — the original API is no longer compatible.
- **Frontend UI**: Rewritten with Vue 3 + TypeScript — UI interaction details may differ.
- **Dependencies**: `xxl-job-core` no longer transitively depends on Groovy. If you need GLUE(Groovy) mode, add `org.apache.groovy:groovy` dependency yourself.
- **Internals**: Module layout, dependencies, and some default configurations may have changed.

Validate the upgrade in a staging environment and back up data before rolling into production.

## Development & Testing

- Use `./mvnw` to invoke Maven so the pinned versions in `pom.xml` are honored.
- `./mvnw -B clean verify --file pom.xml` runs Spotless format checks, unit tests, and JaCoCo coverage.
- Run only unit tests: `./mvnw -pl xxl-job-core,xxl-job-admin test`.
- Run a single test class: `./mvnw -pl xxl-job-admin -Dtest=ClassName test`.
- End-to-end tests are not part of the Maven build. Trigger the Playwright suite with the modular Compose files: `docker compose -f compose.yml -f compose.mysql.yml -f compose.e2e.yml up --build --abort-on-container-exit --exit-code-from e2e-tests` (swap `compose.mysql.yml` for `compose.postgres.yml` to use PostgreSQL).

## Acknowledgements

- Thanks to [@xuxueli](https://github.com/xuxueli) and the [xuxueli/xxl-job](https://github.com/xuxueli/xxl-job) community for their long-standing contributions.
- This project is built on top of the upstream under its open-source license, with the corresponding attribution preserved.

## License

This project is licensed under the [GNU General Public License v3](./LICENSE). Use, redistribute, and derivative works must comply with the license terms.

## Contributing

Contributions are welcome — bug fixes, documentation, test cases, and core improvements are all appreciated.

Suggested flow:

1. Fork the repo and create a feature branch off `main`.
2. Add unit tests and ensure the affected modules pass locally.
3. Open a PR that describes the change, its compatibility impact, and the regression-test steps you ran.
