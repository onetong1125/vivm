# R2 MyBatis-Plus 基础、账户持久化与数据库基线报告

日期：2026-09-12。结论：R2 本机范围通过；2026-09-14 用户审阅批准。当前未进入 R3。

## 修改内容

1. 在 `vivm-dao` 引入 MyBatis-Plus Boot 3 Starter、分页所需 JSQLParser 模块、Flyway Core 与 MySQL 模块；根 POM 统一 MyBatis-Plus 版本为 3.5.17。Flyway 继续使用 Spring Boot 3.4.3 管理的 10.20.1，没有在本批脱离 Boot 依赖管理升级主版本。
2. 在 cycle 的 identity 领域增加 `UserE`、`AccountStatus`、`UserRepository` 与 `UserPage`。账户状态与资源状态分开表达；Repository 接口属于 cycle，MyBatis 技术实现属于 dao。
3. 在 dao 增加 `UserRecord`、`UserMapper`、`UserAssembler`、`UserRepositoryImpl` 和分页插件配置。登录、注册、用户列表、详情、更新与删除的账户持久化路径改用该 Repository；移除 http-server 中旧的 JPA `UserRepository`。
4. 注册接口改用独立 `RegisterUserRequest`，在 HTTP 边界校验用户名、密码和邮箱，不再直接绑定持久化实体。认证读取通过 cycle Repository，再转换为现有 Spring Security principal；未迁移资源关系仍暂时使用旧 JPA `User` 实体。
5. 增加命名为 `jdbcTransactionManager` 的 JDBC 事务管理器，并将现有 JPA 事务管理器明确命名为 `transactionManager`/`jpaTransactionManager`。账户写路径只使用 JDBC 事务；未迁移资源路径继续使用 JPA，本批没有构造一个同时混用两种事务管理器的原子写操作。
6. 增加 V1～V3 Flyway 迁移：V1 按本机旧 schema 建立 12 张兼容表；V2 增加账户状态及资源关系目标字段并回填；V3 增加操作记录、池权限、池准入和镜像占用表。V4 的约束收紧与 V5 的旧关系移除仍按计划留在后续批次。
7. demo、real 配置改为 `ddl-auto=validate` 并启用 Flyway；remember-me 密钥从代码常量改为 `security.remember-me-key`。真实值只保存在 Git 忽略、权限为 `0600` 的 `.local` properties 中，示例文件只保留占位符。SSH host key 的外置沿用 R1 已完成的配置。
8. 增加账户持久化、登录注册、CSRF、管理员分页、MySQL 空库迁移、旧库夹具迁移、唯一约束和多表 JDBC 回滚测试。集成测试库使用随机名称，测试完成后删除自身创建的数据库。
9. 更新领域术语：新增账户状态，并将私有镜像归属规则改为已确认的“其他用户不能依赖它创建镜像”。镜像策略实现仍属于 R3，本批没有提前实现。

## 依据

- cycle 定义业务模型与 Repository 契约，dao 提供 Record/Mapper/Assembler/Repository 实现，符合已确认的四模块依赖方向与参考项目命名风格。
- 身份是第一条 MyBatis 纵向切片：它同时覆盖认证读取、注册写入、后台列表与分页，可在保留资源 JPA 路径的前提下验证新持久化边界。
- Spring Security principal 仍兼容旧资源代码，但账户事实来源已切换到 cycle Repository；这样不会为了迁移 identity 而提前改动 R3～R8 的资源模型。
- Flyway 先建立与旧表兼容的基线，再以扩展、回填、后续收紧的顺序迁移，避免 R2 直接删除旧关系或一次性施加尚未核验的数据约束。
- MyBatis-Plus 官方 Boot 3 安装文档当前示例版本为 3.5.17，因此本批采用该版本；Flyway 使用 Boot 管理版本，以避免在本批额外承担 12.x 模块与许可变化的升级范围。

## TDD 证据

- Red 1：先编写 `IdentityPersistenceTest`，首次因 `UserE` 与 cycle Repository 契约不存在而编译失败；补齐领域接口后又因没有实现 Bean 启动失败；增加 dao 映射和实现后转绿。
- Red 2：先编写 `AuthContractTest`，身份路径切换后因旧 JPA 路径找不到默认 `transactionManager` 而启动失败；显式区分 JPA/JDBC 事务管理器后转绿。
- MySQL 集成测试分别创建空库和旧库夹具：空库执行 V1～V3；旧库先执行 V1、写入兼容夹具并建立 baseline，再执行 V2/V3，验证迁移回填。两个路径没有用 H2 结果替代 MySQL 证据。

## 验证结果

| 命令/检查 | 结果 |
| --- | --- |
| `./mvnw -o clean test` | 通过；connector 4 个测试、http-server 9 个测试，共 13 个，0 failure/error/skipped |
| `./mvnw -o -pl vivm-http-server -am -Dtest=MigrationIT,IdentityPersistenceIT -Dsurefire.failIfNoSpecifiedTests=false -Dvivm.it.config=/Users/xuyitong/IdeaProjects/vivm/.local/application-demo.properties test` | 通过；MySQL 集成测试 3 个，0 failure/error/skipped。配置路径必须是绝对路径，因为 Surefire 在子模块目录运行 |
| `./mvnw -o package -DskipTests` | 四模块打包通过；http-server 可执行 jar 生成成功 |
| demo jar，`spring.profiles.active=demo`，随机端口 | 不带一次性 baseline 参数复启成功；Flyway 报告 schema 当前版本 3 且无需迁移，Hibernate schema validate 通过，随后正常关闭 |
| 只读查询本机 MySQL | `vivm_demo` 最新成功迁移版本为 3；`operation_task` 表和 `user.account_status` 字段各存在 1 个；R2 随机测试库残留数为 0 |
| 安全契约 | 无 CSRF 注册返回 403；带 CSRF 注册成功并可登录；普通用户访问用户列表返回 403；管理员分页和关键字查询通过 |
| JDBC 一致性 | 重复用户名/邮箱由 MySQL 唯一约束拒绝；用户与 operation_task 的测试事务失败后两表写入均回滚 |

## 本机状态变化

- 首次接管既有 `vivm_demo` 前先读取其 schema，确认 12 张表与 V1 定义一致；随后以 `baseline-version=1` 进行一次性 baseline，并执行 V2/V3。复启验证未再携带 baseline 参数。
- 该动作只改变专用 demo 库；没有连接或迁移生产数据库。集成测试只创建并清理随机命名的专用测试库。
- `.local/application-demo.properties` 与 `.local/application-real.properties` 增加 Flyway、JPA validate 和独立 remember-me 密钥；文件仍被 Git 忽略且权限为 `0600`，密钥值未进入日志、报告或构建产物。

## 未验证与已知边界

- 2026-09-14 用户补充确认：旧数据位于 `vivm_db` 且可以全部舍弃，重构后重新构造项目数据。V1～V3 继续作为当前分批替换 JPA 路径的过渡 schema，不再承担历史数据迁移承诺。
- `vivm_demo` 是本机演示库；旧库测试使用合成夹具。V2 回填 SQL 对不满足单一父、单一池或单一根前提的数据快速失败，该测试只证明过渡结构兼容，不代表需要迁移 `vivm_db` 数据。
- Flyway 10.20.1 对 MySQL 8.4 输出“高于已测试的 8.1”警告，对测试使用的 H2 2.3 也输出版本警告；本机迁移和复启均已通过，但这不是对所有数据库版本组合的官方兼容性证明。
- 现有资源 Repository、JPA 实体关系及其事务路径仍保留，这是 R2 的明确过渡状态；完全移除 JPA 属于 R9。
- 用户列表仍返回现有 `Page` JSON 形状，测试日志提示 Spring Data `PageImpl` 的直接序列化结构不稳定；本批保持既有接口契约，没有擅自改变响应结构。
- 测试日志仍有现存 Spring Security provider 和 JDK 22 Mockito 动态 agent 提示；当前认证与测试通过，本批不顺带调整认证装配或测试运行时。
- R3 的镜像 BASE/DELTA、PUBLIC/PRIVATE、父链和授权策略尚未实现；真实 Libvirt、SSH、QCOW2 与备份脚本仍未验证。
