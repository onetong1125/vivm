# R0 批次报告

日期：2026-09-11。状态：本机基线完成，等待用户批准 R1。

## 修改

- 新增并按用户反馈修订 reference-style.md：从 crypto-transfer 源码与上级 AGENTS.md 提取结构/风格，收敛为 cycle、dao、connector、http-server 四个 VIVM Maven 模块。
- 新增 api-baseline.md：23 个 Controller 路由与两个 Security 路由、前端输入输出兼容点。
- 新增 permission-matrix.md：现有/目标权限、已确认不变量、所有写入口并发范围、局部待定项。
- 新增 migration-preflight.md：环境实测、配置位置、未执行的数据核对清单。
- 更新实施计划，记录一次一个批次、批准后再推进；结构命名以 reference-style.md 为准。

本批不修改 Java、pom、前端、数据库、参考项目或 Obsidian 文件；不执行提交、不进入 R1。

R0 评审修订：删除规划中的 vivm-api；将 vivm-io 更名并重新定义为 vivm-cycle；将 vivm-starter 更名为 vivm-connector。Repository/Gateway 契约归 cycle，dao/connector 分别依赖并实现它们，避免核心生命周期模块反向依赖技术实现。

R0 评审修订：用户确认采用按领域聚合的 AppService。一个 AppService 方法对应一个业务用例，不创建 `*UseCase` 类，也不形成 `AppService → UseCase` 的同名转发层；复杂规则进入实体或必要的 DomainService。

## 依据

用户明确指定参考项目，并要求逐批批准。源码抽样覆盖 Controller→AppService、领域实体/请求/服务、RepositoryImpl/Assembler；构建依据参考聚合 pom。术语与业务规则来自本次对话，不复制参考项目的业务权限或金融依赖。

## 验证与限制

- 路由数量：脚本扫描五个 Controller，分别为 Auth 3、Image 4、Users 4、VM 8、Version 4，共 23 个，匹配基线；另计 Security login/logout。
- 文档：Node 只读检查本批 7 个文档的行末空白与相对链接，0 个错误；`git diff --check` 通过。检查包含尚未跟踪的文件，不能仅靠 git diff 判定新增文档通过。
- 环境命令：java_home、mvn -version、mysql --version、redis-server --version、node/npm 版本及监听查询，记录在预检文档。
- 业务测试：未运行。现有 VMControllerTest 为空，contextLoads 会装配真实连接；隔离是 R1 任务，不在 R0 越批修改。
- 数据库、Redis 请求、SSH、Libvirt、真实备份脚本：未调用；数据库数据/外部一致性没有运行证据。

下一批 R1：构建骨架及外部可测试边界、明确 Fake/real 模式、错误契约和离线测试。必须取得用户批准后开始。
