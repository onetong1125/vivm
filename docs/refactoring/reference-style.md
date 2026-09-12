# R0：crypto-transfer 结构与代码风格映射

日期：2026-09-11。状态：参考代码已只读检查，VIVM 尚未实施结构迁移。

参考根目录：`/Users/xuyitong/victorysec/asset-management-platform/crypto-transfer`。以下源路径相对该目录；没有复制金融业务代码、内部依赖或凭据。

## 已核对的依据

| 来源 | 实际观察 | VIVM 采用方式 |
| --- | --- | --- |
| `pom.xml:14` 起 | token-starter、token-io、token-dao、token-bos、token-http-server、token-api、token-gift 七个模块 | 参考其职责拆分方式，按 VIVM 的实际需要收敛为生命周期、DAO、外部连接、HTTP 四个模块 |
| `token-http-server/src/main/java/com/rndwk/projecty/token/http/server/controller/tokenio/v1/TokenTfaController.java` | Controller 构造器注入 AppService、转换请求上下文 | HTTP 入口只负责边界及委派；保留 VIVM Session/CSRF，身份不信任客户端提交 |
| `token-io/src/main/java/com/rndwk/projecty/crypto/transfer/application/TokenTfaAppService.java` | 应用服务接口，方法使用 Command/Query/Dto | 用 `*AppService` 命名应用入口；一个方法对应一个用例，不创建 `*UseCase` 类 |
| `token-io/src/main/java/com/rndwk/projecty/crypto/transfer/domain/chainalysis/model/entity/ChainalysisRegisterE.java` | E 后缀领域实体，自带 mark/update 状态行为 | ImageE、VmE、BackupVersionE，状态迁移写成有意义的方法 |
| 同目录的 `model/req/ChainalysisSyncRegisterCommand.java` | Command 请求对象、Lombok | Command/Query/Dto 分工；允许 DTO 简化访问器，聚合不开放任意状态 setter |
| 同业务包 `service/impl/ChainalysisDomainServiceImpl.java` | DomainService、构造器 final 依赖、Slf4j；也存在宽泛 catch 后仅日志 | 采用命名、注入、日志方式；不沿用吞错，与用户当前规则冲突 |
| 同业务包 `repo/impl/ChainalysisRegisterRepositoryImpl.java` 与 `assembler/ChainalysisAssembler.java` | Repository 实现使用 DAO，Assembler 转实体与持久化对象 | 保留 repo/impl、assembler 结构；内部直接调用 MyBatis-Plus Mapper，不再增加纯转发 Dao |
| 上级 `asset-management-platform/AGENTS.md` | 4 空格、构造器注入、JUnit、测试与包对应 | 新 VIVM 代码用 4 空格。抽样源代码有 2 空格，与指导文件不同；不全库格式化 |

## 目标物理结构

采用单应用的 Maven 多模块结构。限界上下文是 identity 与 resource；Maven 模块是构建与技术职责边界，两者不能等同。

```text
vivm/
  pom.xml                         # aggregator / 版本管理
  vivm-cycle/                     # 主要实体的生命周期、应用编排与端口
    .../application/              # ImageAppService、VmAppService、VersionAppService
    .../application/impl/
    .../domain/resource/image/
      model/entity/               # ImageE、StoragePoolE
      model/valueobj/             # 结构、可见性、生命周期与占用模式
      service/                    # 真正跨实体的领域规则
      repo/                       # Repository 契约
    .../domain/resource/machine/
    .../domain/resource/versioning/
    .../domain/identity/
    .../domain/operation/
    .../port/                     # Gateway 契约及其中立的输入输出模型
  vivm-dao/                       # repo/impl、assembler、Record、Mapper、XML 与迁移 SQL
  vivm-connector/                 # libvirt/SSH 与 demo Fake 的 Gateway 实现
  vivm-http-server/               # Spring Boot 入口、Controller、异常映射、HTTP 安全配置
  frontend/                       # 保留独立 Vue 应用
```

依赖约束：`vivm-cycle` 不依赖另外三个子模块；`vivm-dao` 依赖 cycle 并实现 Repository；`vivm-connector` 依赖 cycle 并实现 Gateway；`vivm-http-server` 依赖并装配 cycle、dao 和 connector。纯领域实体和策略不导入 DAO、HTTP、Libvirt 或 JSch 类型。HTTP 请求/响应模型留在 http-server；应用 Command/Query/Dto 和外部端口的中立模型由 cycle 持有。

不建立 `vivm-api`：当前没有其他项目调用 VIVM 的公共 Java 接口，HTTP 契约也由 http-server 负责。以后若出现真实外部消费者，再从已经稳定的接口中提取，不能为假想调用方预建模块。

`vivm-cycle` 表示 Image、StoragePool、VirtualMachine、BackupVersion、User 和 Operation 等主要实体的生命周期，不表示 I/O。它包含业务状态迁移、应用编排和抽象端口。`vivm-connector` 只连接外部资源，生产 Libvirt/SSH 与本机 Fake 都是 Gateway Adapter；它不拥有生命周期规则。

这是对先前五模块方案的修订。R1 先建立四模块构建骨架并将旧源码整体放入 http-server 保持可编译，后续按批次把生命周期代码迁入 cycle、持久化迁入 dao、外部连接迁入 connector；每个类同一时刻仅有一份定义。未用目录不生成空壳类。只有 http-server 打包可执行 Boot jar。

## 命名、错误与测试约定

- `ImageAppService.cloneImage(CloneImageCommand)` 等 AppService 方法就是用例入口。接口服务于 HTTP 与后续 Worker 共用，Impl 命名与参考项目一致。
- 禁止 `Controller → AppService → 同名 UseCase` 的纯转发链，也不创建 `CloneImageUseCase` 一类。复杂流程先保留为 AppServiceImpl 的私有实现；只有出现可独立复用且具有业务含义的规则时，才提取为实体行为或 DomainService。
- `*DomainService` 仅容纳不能放入实体的业务规则；实体方法不接收 Libvirt/JSch/HTTP 对象。应用服务协调事务与远端操作。
- 持久化记录命名 `ImageRecord`，MyBatis-Plus Mapper、RepositoryImpl 和 Assembler 位于 vivm-dao；本项目不沿用参考项目的生成式 MyBatis Dao 体系，不引入公司 commons 工具包。
- 只在有实际转换时写 Assembler，不新增 MapStruct 依赖或泛型转换基类；日志用 Slf4j 参数化字段，包含 operationId/资源 ID，避免整份凭据/请求日志。
- 本轮不统一替换所有 HTTP 包装为参考项目的 `Body`，以免破坏 Vue 契约；后续发生状态变更时显式同步前端。
- `*Test` 对应业务包，测试写 Given/When/Then 的行为；`*IT` 验证数据库。参考项目的技术版本不作为 VIVM 依赖版本。

批次协议：每次只执行 Rn；汇报文件、依据、验证结果/限制后等待用户批准。未获批准不进入 R(n+1)，不以已有总计划视为后续批次授权。
