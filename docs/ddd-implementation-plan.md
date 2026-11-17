# VIVM DDD 与 MyBatis-Plus 具体实施计划

日期：2026-09-11。状态更新于 2026-11-17：R0～R2 本机验收完成且获批准；R3 代码及本机验证完成，用户已批准提交，未进入 R4。本文件细化并更新 [DDD 设计](ddd-refactoring-plan.md)，业务术语见 [CONTEXT.md](../CONTEXT.md)。

2026-09-24 批次更新：报告见 [R0](refactoring/r0-report.md)、[R1](refactoring/r1-report.md)、[R2](refactoring/r2-report.md) 与 [R3](refactoring/r3-report.md)；真实生产数据及真实 Libvirt/SSH 核对未执行。R4～R9 尚未开始。按用户要求一次只执行一个批次，报告修改、依据、验证证据后等待明确批准；不会自动推进下一批。

新增结构依据：[crypto-transfer 风格映射](refactoring/reference-style.md)。目标收敛为 `vivm-cycle / vivm-dao / vivm-connector / vivm-http-server` 四个 Maven 模块；采用 AppService/DomainService、model/entity/valueobj/req、repo/impl、assembler 命名。每个业务动作是对应 AppService 的一个方法，不创建 `*UseCase` 类。R0～R9 业务职责和验收不变。R1 同时建立构建骨架，旧源码先整体迁到 HTTP 模块以保持单份定义，再逐批迁移。未来命令调整为聚合根测试、指定 http-server 启动。

## 1. 输入、范围与完成标准

已读取用户指定目录 `/Users/xuyitong/Library/Mobile Documents/iCloud~md~obsidian/Documents/打工/Projects/vivm` 中全部 11 篇文档：00 总体规划、01 安全、02 外部边界、03 任务中心、04 幂等、05 冲突、06 Redisson、07 补偿、08 对账、09 WebSocket、10 简历证据。原笔记仅作为输入，不修改、不勾选完成项。

本文最初交付为实施计划，后续按批次授权实施。下述尚未勾选的内容均为后续任务，不能视为已实现；实际验证范围以各批报告为准。

本次重构完成条件：

- 当前登录、用户管理、镜像、VM、版本功能迁入明确的模块和用例，使用 MyBatis-Plus 持久化，移除 JPA。
- 用户已确认的权限、依赖链、单写者和 shutdown/destroy 规则由可重复测试验证。
- 普通测试不依赖 MySQL、Redis、SSH、Libvirt 或原生库；本地演示明确使用 Fake。
- 数据库迁移和事务/并发由本机真实 MySQL 专用测试库验证；本机依赖不可用时记录未验证，不能用内存测试代替。
- 操作身份、阶段、未知结果、外部适配接口可直接被后续任务中心、幂等、补偿和对账复用。
- 真实虚拟化服务器当前不可用，真实 Libvirt/SSH、QCOW2 与脚本验证是延期验收，不阻塞本机交付。

本轮重构不提前交付完整异步调度、Redis、WebSocket、自动对账或通用补偿引擎。安全、可测试边界、最小操作记录及资源占用因当前规则需要，在重构内完成；其余能力按第 7 节接续。

## 2. 与后续计划的兼容及修正

| 后续文档 | 本轮做什么 | 后续直接接什么 / 需修正什么 |
| --- | --- | --- |
| 01 安全 | 用例级资源授权、边界校验、配置外置、账户失效检查 | 当前 SecurityConfig 未关闭 CSRF，前端 api.js 已取 token；保留并回归，不重复实现。池授权规则以本轮用户确认优先于“全部高风险操作仅管理员”的旧概述 |
| 02 外部边界 | 生产与 Fake 适配器、显式模式、结构化失败、有状态 Fake | 真实外部验收延期；后续重启/双进程升级 Fake 状态存储，不改变业务接口 |
| 03 任务中心 | 直接使用 operation_task 命名和稳定 operationId，分开状态、步骤与资源状态 | 后续增加提交/领取/调度；旧 JPA @Version 方案改成 MyBatis-Plus/显式条件更新。操作类型从已迁移用例逐步增加 |
| 04 幂等 | 命令对象可规范化；资源按 ID 定位；创建前预留名称 | 后续增加 Idempotency-Key 与请求摘要，复用同一任务表；operationId 本身不等于请求幂等 |
| 05 冲突 | 集中规则、资源占用、明确 409 | 同一策略用于提交预检查及执行前原子重检，不在 Controller/Worker 各写一套 |
| 06 Redisson | 保持执行互斥与运行占用分离；当前以数据库准入实现正确性 | 后续接入资源级执行锁；长期占用仍留在 MySQL，不能改成随线程释放的 RLock |
| 07 补偿 | 记录阶段、产物身份、明确失败/结果未知，禁止不明结果自动重试 | 后续按操作添加补偿和恢复处理器，复用记录；失败不强行归入普通 FAILED |
| 08 对账 | Gateway 提供当前用例必需的资源查询；数据库不作为 Fake 外部状态来源 | 后续扩展枚举/分页采集、差异报告、受权修复入口 |
| 09 WebSocket | 操作查询 DTO 与 version 固定；更新操作状态在事务中完成 | 后续提交后通知，REST 为事实来源；业务聚合不依赖 STOMP |
| 10 简历证据 | 每批记录实现、测试环境、命令、结果和未验证项 | 不把 Fake 资源、本机双进程写成真实虚拟机或生产高可用 |

排期以依赖与验收为准。原 18 个有效工作日是后续功能的既有基线，未单列此次 DDD/MyBatis-Plus 迁移；本轮 01/02 与其重叠部分只计算一次。完成 R0～R2 并测得实际速度后重估整体日期，本文件不虚报固定完成日。

## 3. 最小实现契约

### 3.1 代码边界

所有 Java 路径以下均相对 `src/main/java/com/ttt/vivm/`；测试使用 `src/test/java/com/ttt/vivm/` 对应包。

| 位置 | 本轮职责与代表文件 |
| --- | --- |
| `vivm-cycle/.../domain/identity/` | `UserE` 及账户生命周期规则；认证适配位于 http-server，MyBatis 实现位于 dao |
| `resource/image/` | `domain/Image`、`StoragePool`、`ImageAccessPolicy`、`ImageOccupancyPolicy`；`ImageAppService.cloneImage/deleteImage` |
| `resource/machine/` | `domain/VirtualMachine`；`VmAppService` 集中创建、启动、关机、断电、重启、删除方法 |
| `resource/versioning/` | `domain/BackupVersion`；`VersionAppService` 集中备份、还原和版本查询方法 |
| `resource/application/` | 必要的 AppService 实现；创建模板 VM 等跨实体流程由所属的 `VmAppServiceImpl` 编排，不另建同名 UseCase |
| `vivm-cycle/.../domain/operation/` | 最小 `OperationE`、`OperationRepository`、查询与阶段更新；未来任务领取、执行器、通知在此扩展 |
| `application/` | `UserAppService.deleteUser` 等跨身份与资源的流程；实现协调相应领域与端口 |

Repository 契约只暴露实际保存/查询需要并位于 vivm-cycle；实现中的 `*Record`、`*Mapper`、Assembler、SQL 留在 vivm-dao。领域模型不引用 BaseMapper、Wrapper、Servlet、Spring Security、Libvirt 或 JSch。现有 DTO 字段初期尽量保持；DTO 的构造不再遍历 JPA 对象图。

外部接口按业务能力分为 VirtualizationGateway、ImageStorageGateway、BackupGateway，契约位于 vivm-cycle 的 port 包，实现位于 vivm-connector。每个方法接收明确的资源标识/规格，返回自身结果；应用看不到 Connect、Domain、XML 或 shell 命令。生产实现和有状态 Fake 是两种适配器，不为纯内部方法增加接口层。

AppService 方法接收 `actorId + Command + operationId` 等独立于 HTTP 的信息。当前同步调用和后续 Worker 调用同一方法；不能把 Authentication 对象、线程上下文或数据库事务跨线程传递。

### 3.2 表与迁移拆分

2026-09-14 用户确认：旧数据位于 `vivm_db`，全部可以舍弃，重构后重新构造项目数据。因此 `vivm_db` 不作为数据迁移目标；目标环境从空库执行版本化 SQL。R2 的兼容表及回填仍用于分批替换当前 JPA 代码，避免同一批次重写全部资源路径，不再承担保存旧业务数据的承诺。

| 迁移 | 具体内容 | 必须验证 |
| --- | --- | --- |
| `V1__legacy_baseline.sql` | 为尚未迁移的 JPA 资源路径建立过渡表结构 | 空库可重复建库，当前旧路径可在迁移期间运行 |
| `V2__resource_fields.sql` | img 增加 pool_id、parent_image_id、storage_kind、visibility、lifecycle；vm 增加 image_id；version 增加 pool_id；pool 增加 root_image_id；user 增加账户状态 | 空库结构正确；兼容夹具只验证过渡路径，不代表需要迁移 `vivm_db` 数据 |
| `V3__operation_and_access.sql` | operation_task、pool_permission、池准入记录、image_occupancy | 名称预留与资源准入可竞争；业务状态和执行阶段独立 |
| `V4__resource_constraints.sql` | 回填后收紧外键/非空/唯一性；版本成员增加活动成员与依赖文件标记 | root_image_id 与池归属一致；公开祖先合法；权限种子没有扩大授权 |
| `V5__retire_legacy_relations.sql` | 仅在 R9 验收后移除被取代的 img_pool、img_child、img_vm、version_pool 和旧字段 | 应用与迁移脚本无旧写入；不能删除仍需要的 user_img/user_vm/version_img |

实际字符串长度、字符集、大小写及唯一索引遵循 R0 查到的 MySQL/schema 条件，不先编造精确 DDL。已有 img_type 的 0/1 映射为 BASE/PUBLIC、DELTA/PRIVATE 是一次性迁移规则，新写入独立表达两维。

同一私有镜像不得授予不同用户共同使用；保留关系表仅为兼容与历史核对，不实现共享私有资源。VM 的公共/私有分类在现有模型中没有字段，删除用户的具体清单需要 R6 的权限矩阵明确，不能简单按所用镜像的 PUBLIC 推导 VM 为公共。

镜像 lifecycle 不使用 `@TableLogic`，否则会影响已逻辑删除祖先的查询和恢复。多表写入由 MyBatis-Plus Mapper/必要 SQL 在显式本地事务内提交。

### 3.3 操作记录与后续状态机

本轮 operation_task 最少字段：`id`、`actor_id`、`operation_type`、`resource_type/resource_id`、`pool_id`、必要命令快照、`status`、`step`、`external_ref`、`error_code/error_summary`、`version`、创建/开始/结束时间。创建资源前尚无资源 ID 的记录以预留名称及 pool_id 定位，外部产物须能与 operationId 对应。

本轮实际状态：RUNNING、SUCCEEDED、FAILED、REQUIRES_REVIEW。步骤按实际用例采用 VALIDATING、REMOTE_EXECUTING、REMOTE_SUCCEEDED、METADATA_UPDATING 等。FAILED 只表示结果已知且未留下未处理副作用；远端结果未知、远端成功但本地提交失败、无法安全处理的残留进入 REQUIRES_REVIEW。数据库失联无法更新状态时，持久化记录会停在先前步骤；下次核对视为未完成，不能声称错误状态必定已写入。

后续异步阶段再增加 PENDING 与领取字段，补偿阶段再增加 COMPENSATING 等必要状态；CANCELLED 仅在取消语义真正实现后开放。状态字段采用可迁移的字符串值，不依赖 Java 枚举 ordinal 或 JPA @Version。

本地状态 CAS 更新必须包含 `id + expected version + allowed status` 并检查影响行数，任务领取还须具备执行权标识。可使用 MyBatis-Plus 乐观锁插件处理它支持的更新路径，关键任务 SQL 显式实现条件，不能假定任意自定义 SQL 都自动受保护。[MyBatis-Plus 乐观锁说明](https://baomidou.com/en/plugins/optimistic-locker/)

### 3.4 四类机制不能混用

| 机制 | 生命周期 | 责任 |
| --- | --- | --- |
| 请求幂等（后续） | 多次 HTTP 请求之间 | 相同请求只产生一条任务 |
| 任务领取（后续） | 一次 Worker 执行期间 | 同一任务只有一个合法执行者 |
| 操作准入/执行锁 | 创建、删除、启动等执行期间 | 协调会冲突的业务操作和外部调用 |
| 镜像读写占用 | VM 实际运行期间 | 当前镜像独占写、全部祖先共享读；跨请求和应用重启保留 |

R4 先使用每池短数据库事务序列化准入判定，记录池级活动操作；外部执行不持有数据库事务。暂时同池外部操作串行，不同池并行；兄弟 VM 可以在分别启动完成后同时运行。后续 05/06 要证明同池无冲突外部操作本身也能重叠时，再细化为资源集合的持久化预留和执行锁。这是明确的后续粒度调整，不把“VM 可以同时运行”冒充“外部操作可以并行”。

占用组为 `(target, WRITE) + (all ancestors, READ)`，在同一池准入下原子检查、提交。启动失败但远端结果未知时保留占用；停止一个 VM 只释放该 VM 的占用。锁不能靠时间到期自动证明 VM 停止。新增克隆时要取得父链读取准入，避免在祖先正在写入时制造不稳定的依赖。

## 4. 重构任务清单（按顺序执行）

### R0：建立可验证基线与权限清单

- [ ] 核对工作区改动，保留已有暂存/未暂存内容；记录当前 Controller 路由和 Vue 调用，不擅自提交。
- [ ] 检查 Java/Maven、本机 MySQL、Redis、容器运行时是否可用；只记录条件，不将安装/启动默认为已完成。
- [ ] 新增 `docs/refactoring/api-baseline.md`、`permission-matrix.md`、`migration-preflight.md`，列出合法/拒绝场景及后续需迁移的接口。
- [ ] 将配置检查结果记录为键名/位置，不把真实凭据写入输出；外部凭据轮换记为独立未完成项。
- [ ] 从可获得的 schema/数据核对私有归属、根、父链、重复名称；没有数据环境时先完成测试夹具 schema，真实迁移核对单独待执行。

通过标准：现有成功路径、已知缺陷、新业务规则分开；每个会改变资源的入口都归入权限与并发矩阵。未取得备份脚本不阻塞 R1～R6。

### R1：外部边界、测试隔离与 Fake

修改 `service/SshService.java`、`connector/LibvirtConnector.java`、两个 TemplateService；新增三个 Gateway、生产/Fake 实现及必要配置。

- [x] 业务代码通过 Gateway 操作，XML/命令构造迁入适配器；生产连接仅在显式生产模式初始化。
- [x] SSH 结果包含 stdout、stderr、exitCode、duration；明确成功、确定失败和结果未知，增加总超时与中断处理，移除错误字符串伪成功。
- [x] `demo` 使用有状态内存 Fake 与独立本机 MySQL 库，前端 `App.vue` 标注模拟环境；`test` 用 H2 + Fake/Mock，无原生库加载；`real` 使用 `.local/application-real.properties` 装配真实连接器，缺配置明确失败。MyBatis/Flyway 迁移与正式 MySQL 集成测试仍属于 R2。
- [x] Fake 保存独立资源状态，支持阻塞、失败、成功后丢响应；不由业务数据库镜像出一份“外部状态”。
- [x] 新增 Gateway 契约测试、`ExternalFailurePropagationTest`、离线上下文测试；替换现有依赖真实初始化的 contextLoads。

通过标准：`./mvnw test` 在无数据库/Redis/虚拟化连接的测试模式运行；本机演示成功与失败结果可观测。此时不声明双进程 Fake 或真实命令正确性已验证。

### R2：MyBatis-Plus 基础、账户持久化与数据库基线

修改 `pom.xml`，加入 Boot 3 对应 MyBatis-Plus Starter、Flyway 及选定数据库所需模块；移除手动固定 Mockito 版本时先验证测试兼容，避免顺带升级其他依赖。

- [x] 增加 `config/MybatisConfiguration`、明确 JDBC 事务管理器、V1～V3 迁移；新库与旧库迁移测试分开。
- [x] 先迁移 identity 的 Row/Mapper/Repository 与认证读取路径，让登录/注册/用户列表验证首批 MyBatis 映射；注册使用独立请求 DTO，不直接绑定持久化 User。
- [x] 生产 Secret、SSH Host Key 配置外置；保留 Session、CSRF，增加 spring-security-test 对应的鉴权测试。
- [x] 旧 JPA 路径明确使用其事务管理器，新路径只用 JDBC 事务；禁止一次原子写操作混用两者。
- [x] 新增 `IdentityPersistenceIT`、`MigrationIT`，本机专用 MySQL 验证唯一性、多表事务失败回滚；容器可用才使用 Testcontainers。

通过标准：登录/注册/CSRF/分页契约通过；空库迁移和夹具旧库迁移通过。JPA 此时仍可为未迁移资源路径保留，不能提前删除。

### R3：镜像、池与权限领域模型

2026-11-17：详见 [R3 具体计划](refactoring/r3-plan.md) 和 [R3 报告](refactoring/r3-report.md)。本机验证完成，用户已批准提交；以下勾选不代表后续写流程已接入。

修改旧 `model/Img.java`、`Pool.java` 相关映射的使用方；新增 image 模块领域模型、持久化与查询实现。

- [x] 定义 BASE/DELTA、PUBLIC/PRIVATE、ACTIVE/DELETED 三组独立含义；父 ID、池 ID、根 ID 明确，关联读取不加载对象图。
- [x] 用领域策略表达：所有用户（包括管理员）不可依赖他人私有镜像创建自己的镜像；公共镜像全部祖先必须公共；逻辑删除祖先仍可支撑已有后代。写端点接入留 R5。
- [x] 模型区分私有归属 `ownerId`、创建者 `createdBy` 与管理权限；公共 owner 为 null，创建者不用于授权。账户删除保留公共资源的实际闭环及公共管理主体留 R7。
- [x] 将仍由旧写流程维护的字段及 ImgChild、ImgPool 关系作为过渡查询的单一权威来源，通过 Mapper 投影成新模型；写流程切换前不直接读取一次性回填后已可能过时的新列，也不在读取时修复数据。
- [x] `ImgDetailsDTO`、`ImgDTO` 改由查询实现投影；普通列表只返回 ACTIVE 且对主体可见的镜像。完整祖先查询仅供内部规则使用，不对用户暴露其他人的分支。
- [x] 新增 `ImagePolicyTest`、`ImageQueryIT`、`ResourceAuthorizationTest` 和 `LegacyImageWriteQueryTest`，覆盖管理员派生资格被拒的领域规则、公共父下跨用户兄弟分支隔离及旧创建/删除/恢复后的查询一致性。

通过标准：镜像关系与已确认规则可独立于 Spring/数据库进行单测；查询能区分普通可见列表与完整依赖查询。克隆的池授权条件在 R5 接入写流程前确定，R3 不把池操作权限作为公共镜像可见或可读的附加条件。暂不新建发布/改父功能。

### R4：最小操作记录与原子准入

新增 operation 模块、池准入持久化、image_occupancy；修改所有冲突写入口，避免新旧路径绕过准入。

- [ ] 实现创建 operation_task、阶段更新、归属校验后的 `GET /api/operations/{id}`；错误与结果 DTO 统一返回 operationId。
- [ ] 同一短事务预留名称、检查池活动操作、写操作记录；全局 VM/池名按各自范围预留，不能仅依赖按池串行避免跨池 VM 重名。
- [ ] 实现整组读写占用的申请、按持有者释放及人工触发核对；事务失败整组撤销，远端调用期间无 DB 长事务。
- [ ] 已运行 VM 在切换窗口从可信 Fake 状态初始化占用；发现冲突作为迁移问题报告。真实环境初始化留到真实上线。
- [ ] 新增 `OperationRepositoryIT`、`AdmissionConcurrencyIT`、`ImageOccupancyTest`，用线程同步屏障制造竞争，不靠 sleep 猜并发窗口。

通过标准：同镜像双启动只准入一个；父写阻止后代、后代读阻止父写；兄弟 READ 共享；停止一个不释放另一个；未知状态保留待核对。当前同步执行，不声称已完成异步任务中心。

### R5：克隆/删除镜像纵向迁移

修改 `controller/ImageController.java`、`service/ImageService.java`、`ChildService.java`、`PoolService.java` 及对应 DTO；新增 `ImageAppService` 与 `ImageAppServiceImpl`，实现 `cloneImage`、`deleteImage`。

- [ ] Controller 仅转换输入/主体和 HTTP，调用一次 AppService 方法；规则、权限、外部阶段、元数据提交归入 AppService 实现及其调用的领域行为。
- [ ] 克隆使用 parentId；本轮 Vue `ImagesPage.vue` 同步切换。旧 parentName 适配在仍保持全局名称唯一时保留，结束兼容后才放宽池内同名。
- [ ] Gateway 产物绑定 operationId；文件成功、元数据失败记录待核对，禁止无条件重新创建。
- [ ] 删除先判断 VM 关联，再作逻辑删除/物理回收决策；完整后代仍需依赖时保留文件，不能只看直接子节点。
- [ ] 新增 `ImageAppServiceTest`，按 `cloneImage_*`、`deleteImage_*` 场景组织测试；增加 HTTP 集成测试，覆盖假成功、同名竞争、他人私有镜像、间接依赖。

通过标准：成功创建一份正确父/池/归属记录；失败不产生可用假镜像；Controller 不直接操作关联表。生产部署仍需所有旧写入口完成准入接入。

### R6：VM 生命周期与 shutdown/destroy

修改 `controller/VMController.java`、`service/VmService.java`、`frontend/src/pages/VmsPage.vue`；迁移 VM Record/Mapper/Repository，新增 `VmAppService` 与实现。

- [ ] 创建模板流程编排池、基础镜像、VM；逐步记录产物，避免最后只记一次“失败”。基础镜像与 VM 名称分别预留。
- [ ] 创建克隆 VM 和 start 均使用相同占用协议；即使创建后自动启动，也不能绕过读写占用。
- [ ] shutdown 调用正常关机，返回 202 和“关机请求已受理”，状态保留 STOPPING；确认远端停止才释放占用。destroy 增加独立路由与危险操作按钮；不隐式删除镜像/定义。
- [ ] restart 不释放占用；delete 对运行 VM 的处置明确，用户删除流程采用请求关机并等待后续核对，不静默强制断电。
- [ ] 在 `VmAppService` 增加 `refreshState`，列表/详情显式刷新及执行前核对复用同一内部流程；离线/状态未知时不能把 VM 显示为已停止。大规模后台自动对账留后续。
- [ ] 新增 `VmLifecycleTest`、MockMvc 权限测试、前端状态回归；覆盖关机被忽略、调用丢响应、元数据提交失败、兄弟 VM 并行运行。

通过标准：一镜像多 VM 关联正常；运行与占用状态一致；VM 列表区分停止/运行/关机中/待核对；Fake 演示可完成创建—启动—关机—断电—删除路径。

### R7：池权限与用户删除闭环

修改 `UsersController`、用户删除入口、池权限查询；在 `UserAppService` 增加 `deleteUser`，实现跨身份与资源的处置流程。

- [ ] 按池具体操作授权备份/恢复；公共使用权不扩大为池管理权。普通用户与管理员的用例入口均经过权限策略。
- [ ] 删除账户先阻止登录和所有旧会话继续访问；所有资源写入在接受与实际执行前检查账户有效性，避免在途创建漏入清理之后。
- [ ] 在途操作完成或进入明确待核对后形成删除清单；公共资源保留、撤销用户授权；私有 VM 停止并删除，再按叶到根清理私有镜像。
- [ ] 故障保存当前步骤，提供已有操作查询/核对入口；不能仅删除 user 行或依赖数据库级联处理外部资源。
- [ ] 新增 `UserAppServiceTest` 的删除场景及 `AccountRevocationTest`，覆盖已有会话、remember-me、并发创建、部分失败和公共祖先保留。

通过标准：账户失效不再访问；私有资源清理结果逐项可核对；公共资源和其他用户数据保留。历史共享归属或 VM 可见性没有明确映射时，仅阻止这一步的数据切换，不阻塞 R1～R6。

### R8：备份/恢复兼容迁移

修改 `VersionController`、`VersionService`、`VersionDTO`、`VersionsPage.vue`，迁移 versioning 模块与版本成员持久化。

- [ ] 保持同端、不停机全量文件复制的业务范围；界面说明不承诺一致恢复点，不新增异地灾备。
- [ ] 备份记录活动成员与必需依赖文件，复制完成不等于恢复验证通过。恢复前在准入下确认该池无运行 VM/未知占用。
- [ ] 还原只恢复符合当前权限和账户状态的业务成员，不能因旧版本把已删除账户的私有资源重新公开或恢复为可用。涉及已删除私有内容的历史版本处置列为第 8 节的决策，不自动清除其他用户的整池备份。
- [ ] Fake 验证成员切换、依赖保留、远程成功后数据库失败；脚本缺失时标记真实适配器未验证，保留原命名兼容范围。
- [ ] 新增 `VersionAppServiceTest`，按备份/还原场景组织测试；增加 `VersionPersistenceIT`。

通过标准：无池权限不能执行；恢复与启动互斥；不会把依赖文件误恢复成活动镜像；当前 API 的查询功能可用。脚本命名审计完成前，不放宽生产版本名唯一性。

### R9：收尾、MyBatis 全量切换与本机交付

- [ ] 全局检查旧 JPA/Repository 调用，完成查询与 DTO 替换；移除 Spring Data JPA、Hibernate、旧实体关系及已替代 Service。
- [ ] 执行 V4 的完整性验证；V5 破坏性迁移作为独立最后步骤，先提供可审阅 SQL 与数据核对结果，不能和普通代码启动绑定。
- [ ] 当前项目没有前端测试脚本；本轮先补必要的 HTTP/状态回归和可重复浏览器场景，`npm run build` 只证明构建，不代表交互正确。
- [ ] 新增 README 启动说明、`docs/refactoring/validation.md`、数据库迁移/回退步骤；结果记录注明 Fake、本机依赖和未执行项。

通过标准：普通测试、本机 MySQL 集成、前端构建和关键交互通过；无旧写入口绕过权限/占用；已有用户改动没有被覆盖。至此 DDD/MyBatis 重构结束，异步任务等进入后续增量。

## 5. 验证入口与必测矩阵

实施时建立以下入口，当前均为目标命令，尚未运行：

| 入口 | 范围 | 依赖 |
| --- | --- | --- |
| `./mvnw test` | 领域、用例、HTTP 安全、Fake 契约 | 无服务依赖，生产适配器不加载 |
| `./mvnw -Pmysql-it verify` | MyBatis、Flyway、唯一约束、并发、事务回滚 | 本机专用 MySQL；Failsafe 运行 `*IT` |
| `./mvnw -pl vivm-http-server spring-boot:run -Dspring-boot.run.profiles=demo` | 本机可登录的模拟应用 | Fake + `.local/application-demo.properties` 指向的独立 `vivm_demo` MySQL，并显式标识 |
| `npm run build`（frontend 目录） | Vue 构建 | 前端依赖已安装 |
| 后续 `-Predis-it`、双进程脚本 | Redisson 与任务领取 | 共享 MySQL/Redis + 同一 Fake 外部状态 |
| 延期 `-Preal-it` | Libvirt/SSH/脚本真实协议 | 专用真实测试资源、显式配置 |

关键验证数据：A→B→C；A 下有 B、D 两个兄弟增量；两个不同用户、一个管理员；用户私有和公共根；已删除 B 仍支撑 C；已停止 VM、运行 VM、未知 VM；相同名称/相同请求的并发提交。

每次故障断言四项：数据库资源状态、Fake 外部状态、操作记录、占用/准入状态。仅检查抛异常不算一致性验收。测试范围随切片扩展，阶段通过后除非新增改动/问题不重复跑全套。

## 6. 迁移与回退操作顺序

1. 保留测试夹具与旧 schema 清单作为兼容参考；不迁移 `vivm_db` 数据，目标环境从空库重建项目数据。
2. 过渡阶段扩展列/表；持久化迁移以模块完整写入集合为边界，禁止 JPA 与 MyBatis 在同一事务内隐式混写。
3. 开发可分批，生产切换必须在维护窗口停止旧写入并核对在途操作，建立已有运行资源占用；切换期间不允许旧版本继续写新状态。
4. 校验新路径后收紧约束；旧关联表只保留到对应 JPA 路径迁移完成，不为可舍弃数据增加冲突修复逻辑。
5. 回退应用前检查 schema 兼容、在途操作和外部状态；旧版本不认识新占用/生命周期时不能直接恢复其写权限。
6. R9 通过后再单独移除旧关系；外部文件、虚拟机变更不能用数据库回滚冒充恢复。

## 7. 重构后的功能接续顺序

这些是后续功能的接入计划，不属于本轮重构完成声明。

| 批次 | 具体工作与接点 | 验收 |
| --- | --- | --- |
| F1 可靠异步任务（03） | 扩展 operation_task 为 PENDING；增加 SubmitOperation、原子领取、OperationDispatcher 和有界执行器；创建/删除 VM、恢复版本先迁移 | HTTP 202 返回 operationId；提交持久化后崩溃仍可发现任务；队列满的已提交任务保留 PENDING，后续数据库扫描领取，不依赖内存队列可靠性 |
| F2 请求幂等（04） | 客户端 Idempotency-Key；以 actor/type/key 唯一约束和规范化 payload_hash 去重；同键不同内容 409；先不加 Redis 快速拦截 | 20 个并发同请求只生成一个任务和一个 Fake 产物；重复返回同 ID；不同用户不能取他人任务 |
| F3 冲突与 Redisson（05/06） | 复用 ImageOccupancyPolicy；将同池粗粒度活动操作细化为冲突资源集合，短池事务仍负责原子准入；Redisson 在 Worker 同一线程加/解锁，不替代持久化预留 | 同资源不重叠，不冲突资源（含同池兄弟资源）可重叠；Redis 不可用不得绕过锁写；锁等待超时明确；固定资源类型/ID 排序 |
| F4 补偿与重启恢复（07） | 在用例步骤上添加已确认的重试/补偿处理；启动恢复核对 RUNNING、REQUIRES_REVIEW；未知外部结果不重新执行 | 外部成功、本地失败、丢响应、进程中断均能说明外部/本地/任务/占用结果；补偿只处理属于本次操作的产物 |
| F5 自动对账（08） | 新增定时/分页采集、差异记录，复用 Gateway 查询与任务用例；先报告，修复经受权任务执行 | 外部独有、数据库独有、状态偏差与正常对照均覆盖；不静默删资源；避免将执行中的暂态误判并修复 |
| F6 WebSocket（09） | operation_task 提交后发送 operationId/status/step/version，用户目的地鉴权；REST 补查 | 消息失败不改任务结果；乱序不覆盖新状态；断线补查恢复；连接 A/执行 B 另测跨实例路由 |
| F7 证据与简历（10） | 聚合前述执行命令、数据、环境、局限与演示 | 只写实际验证过的本地能力，不写真实虚拟化压测或多机高可用 |

F1 的执行权限在提交与执行时都检查；用户删号或权限撤销后，已排队任务不可继续创建资源。领取条件更新防止同一记录被同时领取，但不自动证明远程 exactly-once；Worker 崩溃后未知任务必须核对外部状态。任务查询、409 中的冲突信息和 WebSocket 都需要防止泄露其他用户的资源/任务详情。

Redisson 的锁所有权与线程有关，Watchdog/租约释放也不代表已经停止的或仍在运行的远程命令；不在 HTTP 线程获取锁后交给 Worker 解锁。持久化预留不能随 Redis 锁过期而清空；没有外部 fencing 支持时不能宣称网络分区下绝对互斥。[Redisson 官方锁语义](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/)

F1 重启验证前升级 Fake：使用独立的本机 Fake 服务进程保存资源状态和调用日志，应用重启不重启该进程；需要重启 Fake 自身时先持久化快照并恢复。F3 两个 VIVM 进程连接同一 Fake 服务，共享测试 MySQL/Redis。此服务只实现已用 Gateway 操作，不变成通用虚拟化模拟器。记录全局调用区间及产物数，禁止用两个互不相干的内存 Fake 证明唯一性。

## 8. 剩余局部决策及处理边界

以下不阻塞 R0～R5 的领域与基础实现；开始相关功能前把实际选择写入权限矩阵/迁移报告即可，不重新询问已经确认的业务规则。

| 项目 | 可先完成的工作 | 必须确定的时点 |
| --- | --- | --- |
| 公共使用是否包含在绑定池中创建个人增量 | 实现 USE_PUBLIC_IMAGE 与 MANAGE_POOL 的分离；建议自由使用包含派生个人增量，备份/恢复另授权，但尚不把建议当已确认规则 | R5 对普通用户开放完整克隆接口前 |
| VM 的公共/私有、公共资源留存后的管理主体 | 保留旧归属数据，完成分类可输入的用户删除流程与测试 | R7 执行实际资源删除前；不可从“使用公共镜像”推导公共 VM |
| 备份包含已删除用户的私有内容 | 恢复时不复活已删除账户权限，识别受影响版本 | R8 明确仅删除活动资源还是连备份副本一并清除；未授权前不删除整个池备份 |
| 真实脚本路径、名称与复制语义 | Fake 与 BackupGateway 实现、旧版本命名兼容 | 放宽真实版本名范围和真实适配器验收前 |
| 本机 MySQL/Redis/容器版本 | 离线单测、文档与接口实现 | R2 集成测试/F3 Redis 验收前 |

验收记录按“计划 / 已实现 / 本机测试通过 / 真实外部待验证”分别填写。此次只完成计划编写和输入对照，不产生任何功能完成或运行性能结论。
