# R1 外部边界、测试隔离与 Fake 报告

日期：2026-09-12。结论：R1 本机范围通过，已停在 R1，未进入 R2。

## 修改内容

1. 将根项目改为 Maven 聚合工程，建立 `vivm-cycle`、`vivm-dao`、`vivm-connector`、`vivm-http-server` 四个模块。旧业务源码先整体迁入 http-server，保持每个类只有一份定义；dao 在 R2 前保持空模块。
2. 在 cycle 定义 `ImageStorageGateway`、`VirtualizationGateway`、`BackupGateway` 和不依赖 Libvirt/JSch 的 `ExternalOperationException`，区分 `FAILED` 与 `UNKNOWN`。
3. 在 connector 实现 real 适配器，将 Libvirt 连接、XML 渲染、SSH 命令与备份脚本调用从 HTTP 业务模块移出。SSH 返回 stdout、stderr、exitCode、duration，使用严格 host key、命令超时和中断传播。
   R1 评审中将三个实现类修正命名为 `LibvirtVirtualizationAdapter`、`LibvirtImageStorageAdapter`、`SshBackupAdapter`；`*Gateway` 名称只用于 cycle 中由业务定义的端口接口。
4. 在 connector 实现有状态 `FakeExternalEnvironment`。池、镜像、VM、备份均有独立外部状态，并可控制下一次操作为阻塞、确定失败或副作用完成后丢失响应。
5. `PoolService`、`ImageService`、`VmService`、`VersionService` 改为构造器注入 Gateway；外部失败先抛出，失败路径不继续写备份元数据。Controller 不再认识 `LibvirtException`，并在外部异常响应中保留 outcome。
6. 增加显式 `real/demo/test` 配置。R1 评审最终采用外部本地 properties：demo、real 分别导入 `.local/application-demo.properties` 和 `.local/application-real.properties`；test 使用 H2。没有激活模式时因没有 Gateway Bean 而快速失败。环境变量仅作为未来部署时可选的 Spring 配置覆盖来源，不是本机启动的必需流程。
7. R1 评审修正后，demo 从 Git 忽略且权限为 `0600` 的 `.local/application-demo.properties` 读取旧 MySQL 账号，并固定使用独立的 `vivm_demo` 库；test 继续使用 H2。MySQL 8.4.3 连接与当前 JPA 建表已实测，MyBatis-Plus 与 Flyway 仍从 R2 开始。
8. 前端增加 demo 构建/启动脚本，并在登录页和页头显示模拟环境标识。

## 依据

- `vivm-cycle` 持有业务所需端口，且不依赖 HTTP、DAO、Libvirt 或 JSch；connector 依赖 cycle 并实现端口；http-server 负责装配。这符合已确认的四模块依赖方向。
- 外部资源可能出现“已执行但响应丢失”，因此不能把所有异常都当成未执行；`UNKNOWN` 与有状态 Fake 为后续 operation、补偿和对账留下稳定接点。
- 本批只建立外部边界和测试隔离，不引入 AppService/领域实体迁移、MyBatis-Plus、Flyway、并发占用或新业务功能，避免越过 R2～R9 的审批边界。

## TDD 证据

- Red 1：先增加 Fake 契约测试，编译因三个 Gateway 与 Fake 尚不存在而失败；随后补齐最小契约与实现，4 个场景转绿。
- Red 2：先增加 `ExternalFailurePropagationTest`，编译因 `VersionService` 没有可注入构造器而失败；改为 Gateway 构造器注入后转绿，并验证外部复制失败时 `VersionRepository` 没有交互。
- Spring 上下文测试激活 `test`，断言三个 Gateway 指向同一个有状态 Fake，证明测试启动没有选择 real 连接器。

## 验证结果

| 命令/检查 | 结果 |
| --- | --- |
| `./mvnw -o clean test` | 通过；connector 4 个测试、http-server 2 个测试，共 6 个，0 failure/error/skipped |
| `./mvnw -o package -DskipTests` | 四模块打包通过；仅 http-server 生成可执行 Boot jar |
| demo jar，`spring.profiles.active=demo`，随机端口 | 无附加配置参数启动通过；自动导入 `.local/application-demo.properties`，连接 MySQL 8.4.3 的独立 `vivm_demo`，Tomcat 启动后人工正常终止 |
| 查询 `INFORMATION_SCHEMA.TABLES` | `vivm_demo` 中有当前应用初始化的 12 张表 |
| 无 profile、非 Web 启动 | 按预期退出码 1；明确报告缺少 `ImageStorageGateway` Bean |
| `npm run build` | 通过；15 modules transformed |
| `npm run build:demo` | 通过；15 modules transformed，生成独立 demo bundle |
| `git diff --check` | 通过 |
| `.local/application-{demo,real}.properties` | 均被 Git 忽略且权限为 `0600`；可执行 jar 不包含 `.local` 文件 |

## 未验证与已知边界

- 当前没有真实虚拟化服务器，real Libvirt/SSH/QCOW2/备份脚本只完成编译与边界实现，未声称真实命令正确。
- `.local/application-real.properties` 已完成配置迁移和键名校验，但没有为验证配置而主动连接真实 Libvirt/SSH；真实适配器验收仍延期。
- 当前只证明旧账号可连接、`vivm_demo` 可建库及旧 JPA schema 可初始化；尚未执行 R2 的 MyBatis/Flyway 迁移、事务、唯一性和旧数据兼容验证。
- Fake 的失败/丢响应/阻塞控制已通过自动化测试验证，但没有新增面向用户的故障注入 HTTP 接口；demo UI 只负责明确标识模拟环境。
- 现有 JPA 模型、Service 和 Controller 仍位于 http-server，这是 R1 约定的过渡布局；R2 起按纵向批次迁入 dao/cycle。
- 测试日志仍有现存 Spring Security provider 提示和 JDK 22 下 Mockito 动态 agent 提示，不影响本批测试结果；不在 R1 顺带改认证配置或测试依赖。
- 根目录原本被 Git 忽略的本地 `src/main/resources/application.properties` 保持不变；其中 MySQL/JPA 键被迁入 `.local/application-demo.properties`，real 数据库、Libvirt、SSH 配置迁入 `.local/application-real.properties`。旧属性名 `ssh.connectionTimeout`、`ssh.session.keepAliveInterval` 同步转换为 connector 使用的新键；旧文件暂时保留，未在缺少真实虚拟化环境时做破坏性删除。
