# R3 镜像、池领域与授权查询报告

日期：2026-09-24。结论：R3 代码、本机测试与页面验证完成；2026-11-17 用户批准提交，未进入 R4。

## 修改及依据

1. `vivm-cycle` 新增 `ImageE`、`StoragePoolE`、三组独立状态、`ImageActor` 与 `ImagePolicy`，按 `model/entity`、`model/valueobj`、`domainservice` 组织。Pool 与镜像是不同实体；以 ID 表达单根、单父、同池、多分支关系，不引入 JPA 对象图。
2. 将管理员查看资格与派生资格分开。领域校验要求公共镜像的全部祖先公共、私有祖先归属一致；已删除祖先可以支撑已有后代，但不能选作新派生的直接父镜像。公共 `ownerId=null`，`createdBy` 仅表达来源，不授予公共管理权。
3. cycle 定义外部可见投影 `ImageCatalog` 和内部 `ImageRepository`。dao 通过 Mapper/Record/Assembler 实现：列表/详情在 SQL 中按 ACTIVE、公共/本人私有/管理员过滤，内部祖先读取包含 DELETED。返回 DTO 不包含子树或分支统计，不为显示公共根加载其他人的分支。
4. 过渡读取只使用当前 JPA 写流程维护的 `img_type/is_delete`、`img_child/img_pool/user_img`；结构依据父关系，不将 `img_type` 当结构。V2 新列仅一次回填、后续会过时，所以本批不改读这些列、不做 COALESCE 或读取修复。可见私有记录缺失/多重归属显式失败；无权用户不会因隐藏坏记录获得信息或查询报错。
5. 镜像列表/详情 Controller 使用认证主体调用 `ImageCatalog`；不可见或已删除详情返回 404。DTO 和 Vue 同步切换为 `storageKind/visibility/lifecycle`，分别显示“基础/增量”“公共/私有”“可用/已删除”；不将旧混合 `status` 冒充运行占用。
6. 新增领域、HTTP、MySQL、旧写后查询测试；将 R2 MySQL 测试支持类移到公共 `test/.../support` 复用。测试源码纳入本批提交范围。同步术语、权限矩阵和分批计划。

依据是用户确认的业务语义与 [R3 计划](r3-plan.md)，不是扩大权限：公共可读不附加池备份/恢复权限；没有新增 UseCase、API 模块、发布/改父功能或数据库迁移。V1～V3 未修改。

## 测试与修正记录

- HTTP 负向测试先在旧查询上复现：A 列表泄露 B 私有分支、A 读取 B 详情得到 200、管理员列表混入已删除镜像；改用授权查询后同一组断言通过。
- 领域测试覆盖管理员可查看但不可派生他人私有镜像、完整祖先/跨池/环/单根多子、不合法中间 BASE 和公共间接私有祖先。另补合法 `BASE/PRIVATE` 与 `DELTA/PUBLIC`，验证结构与可见性独立。
- 全量整合发现旧写测试与其他测试共用 H2 库而互相污染，已给旧写测试使用独立内存库；未修改生产逻辑。
- MySQL 首轮唯一失败是异常类型断言：Spring 的 Repository 异常翻译将归属错误包装为 `InvalidDataAccessApiUsageException`。断言修正为同时验证包装类型、原始 cause 与归属错误消息；重跑通过，未添加吞错或回退。
- 独立只读审查未发现可确认的新代码缺陷；提出的状态组合验证遗漏已补齐并执行。

## 验证结果

2026-11-17 提交前重新运行 `./mvnw -o clean test`，29 项通过，0 failure/error/skipped；本次未重跑 MySQL 和浏览器验收，下表对应此前 R3 验收记录。

| 检查 | 实际结果 |
| --- | --- |
| `./mvnw -o clean package` | 29 项测试通过：cycle 10、connector 4、http-server 15；0 failure/error/skipped；四模块编译与可执行 jar 打包通过 |
| `./mvnw -o -pl vivm-http-server -am -Dtest=MigrationIT,IdentityPersistenceIT,ImageQueryIT -Dsurefire.failIfNoSpecifiedTests=false -Dvivm.it.config=/Users/xuyitong/IdeaProjects/vivm/.local/application-demo.properties test` | 真实 MySQL 8 项通过：镜像查询 5、账户持久化 1、迁移 2；独立随机测试库，测试后删除各自测试库 |
| `LegacyImageWriteQueryTest` | H2 + Fake 真实旧 Service 创建基础/增量、备份、删除、恢复后，新查询立即反映元数据；未靠手写镜像 INSERT 替代写流程 |
| `ResourceAuthorizationTest` | 4 项 MockMvc 测试使用真实 User principal 和 DAO，验证 A/B 对称隔离、搜索、404、公共详情不泄露分支、管理员 ACTIVE 过滤、未登录 401 |
| `npm run build` | Vue/Vite 构建通过 |
| 实际浏览器双会话 | 临时 H2 + Fake 后端及 demo 前端；A 创建公共根与 A 私有分支，B 在同根创建 B 私有分支；双方列表只含公共根和自己的分支，搜索对方分支均为空；三组状态字段有实际显示并核对完整截图 |
| `git diff --check` | 通过 |

本机运行日志为 `/tmp/vivm-r3-full.log`、`/tmp/vivm-r3-mysql.log`；页面截图为 `/tmp/vivm-r3-a-images.png`、`/tmp/vivm-r3-b-images.png`。这些是临时证据，不进入 Git；可重复断言保存在测试源码中。页面验证使用的端口 18083/15173、两个独立浏览器会话已关闭，合成数据随临时内存应用销毁。未改变 `vivm_db`、`vivm_demo` 或本机 secrets 配置。

## 边界及后续事项

- `ImagePolicy` 目前是规则层，完整克隆/删除写流程还没有调用它。不能据此宣称旧写端点的全部越权已修复；R5 接入前仍需明确公共克隆授权。现有 `parentName` 保留，不提前放宽镜像名称约束。
- 私有单写者、祖先读占用与多 VM 协调留 R4/R6；账户删除保留公共资源的实际执行留 R7；公共管理主体仍未确定。
- 内部 `ImageRepository` 不等于 HTTP 可见查询，不能直接序列化其完整祖先结果；查询投影不承担全库历史关系修复。旧数据已确认可舍弃，本批未重建业务库。
- 旧写回归仅证明元数据读取一致；Fake 备份恢复不证明真实文件内容已还原。真实 Libvirt、SSH、QCOW2 和备份脚本没有验证。
- 浏览器验收发现两个既有前端问题，未扩大本批修改：`AuthController.register` 返回 201 空 body，但 `frontend/src/api.js` 仅跳过 204，页面错误解析 JSON；登录后未清除旧 CSRF token，首次写操作返回 403。验收中手动切回登录，并在登录后重新加载页面才继续资源写入。建议单独批准修复，不能把此次页面结果描述为注册到资源写入全流程无障碍。
- 既有 Flyway 对 MySQL 8.4/H2 2.3 的版本警告、JDK 22 Mockito 动态 agent 提示仍存在；本批没有升级依赖。

## 提交范围建议

采用一个 R3 commit：领域契约、DAO、HTTP/Vue 必须同步改变，因此不拆成不能独立工作的接口或调用方提交。测试、共用测试支持类及对应文档随本批提交；不包含 `.local`、构建产物、临时截图或数据库数据。提交信息明确 R3 阶段及镜像模型、可见查询、兼容读取等重构点；范围与提交信息已展示，2026-11-17 用户批准提交。
