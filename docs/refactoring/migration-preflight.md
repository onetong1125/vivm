# R0：环境与迁移预检

日期：2026-09-11。本次只运行本地只读命令，不访问数据库数据、不启动/停止服务、不连接 SSH/Libvirt。

## 环境实测

| 项目 | 观察 | 结论 |
| --- | --- | --- |
| java、mvn | Java 22.0.2；系统 Maven 3.6.3 | pom 声明 Java 17，需 R1 构建时核对工具链兼容 |
| `/usr/libexec/java_home -V` | 注册 JDK 22 与三个 Java 8，未列 JDK 17 | 不等于磁盘绝无 JDK17；本次未做全盘搜索或安装 |
| Maven Wrapper 配置 | distribution 为 Maven 3.9.9，wrapper 3.3.2 | 配置事实，未下载/执行 wrapper，不是运行版本证明 |
| mysql --version | MySQL 客户端 8.4.3 | 仅证明客户端存在，不证明服务端版本 |
| `lsof -nP -iTCP:3306 -iTCP:6379 -sTCP:LISTEN` | 6379 有本机 redis-server；3306 无匹配监听 | 不排除 MySQL socket/非标准端口；未认证探测 |
| redis-server --version | 二进制 8.0.0 | 不用二进制版本推断正在运行进程的版本 |
| command -v docker/podman | 当前 PATH 均未找到 | 不预设 Testcontainers 可用；不声称机器没装 GUI 容器工具 |
| node/npm | v20.19.4 / 10.8.2 | 前端工具可找到，未运行构建 |

## 配置位置（不记录值）

- `src/main/resources/application.properties:6-9`：datasource URL、用户名、密码及驱动键。
- 同文件 `:13`：libvirt.uri；`:16-19`：SSH host/port/username/password；`:23-24`：超时与 keepalive。
- 同文件 `:10`：`spring.jpa.hibernate.ddl-auto=update`。需要先比对真实 schema，再安排 Flyway 基线；不能直接对非空库启用自动 baseline。
- SecurityConfig 有 remember-me key；SshService/LibvirtConnector 初始化连接需 R1 隔离。配置外置与凭据轮换不是 R0 已完成事项。

只提取键名/位置，未在报告保存连接地址、用户口令。参考项目配置不作为 VIVM 运行配置来源。

## 迁移核对项目

当前仓库未检出项目 SQL 文件；未取得业务数据库副本或 schema 导出，因此以下均为待验证，不能标记通过：

- 表结构、唯一索引、字符集/排序规则、外键与 JPA 声明是否一致。
- img_pool/parent 关系是否唯一、无环且同池；是否存在多根池。
- user_img/user_vm 是否有多用户归属；是否有公共后代依赖私有祖先。
- VM 关联多个/不存在镜像、镜像跨池引用、孤儿关联。
- 版本成员、已删除祖先保留文件、版本名与备份目录碰撞。
- 既有运行 VM 的读写占用组是否冲突。

R2 在专用本机库先构建合成夹具：公共根 A、私人链 A→B→C、兄弟 B/D、两个用户和管理员；另设非法数据夹具测试迁移必须报告而非修复。真实数据回填和占用初始化留到取得数据后的单独切换步骤。

## 批次状态

R0 的源码/风格/环境基线已完成；真实 schema、数据及运行资源核对未执行。R1 不依赖真实数据，可以在用户批准后开始。不能据此批准真实数据库迁移或宣称旧库符合新模型。
