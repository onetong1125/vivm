# R0：现有 HTTP 与前端基线

日期：2026-09-11。依据：当前五个 Controller、SecurityConfig、frontend/src/api.js 与四个页面；静态检查，未发送请求。HEAD：`8d7a102f8f71e6ad83801ae029b89f6925c043ea`。

## 路由清单

共 23 个 Controller 路由，另有 Security 接管的 login/logout。表中参数省略来自认证上下文的主体。

| 方法 / 路径 | 输入 | 当前正常返回 / 调用方 |
| --- | --- | --- |
| GET /api/auth/csrf | 无 | headerName/parameterName/token；api.js |
| GET /api/auth/me | Session | UserViewDTO；App.vue |
| POST /api/auth/register | JSON User，含 confirmPassword | 201；App.vue |
| POST /api/auth/login | 表单认证 | Security 自定义成功处理；App.vue |
| POST /api/auth/logout | Session、CSRF | 204；App.vue |
| GET /api/users | page=0、size=10、keyword 可选 | Page<UserViewDTO>；UsersPage |
| GET /api/users/{userId} | ID | UserViewDTO |
| PUT /api/users/{id} | UserOptDTO JSON | status/message；UsersPage |
| DELETE /api/users/{userId} | ID | status/message；UsersPage |
| POST /api/imgs/createClone | imgName、parentName 表单 | status/message；ImagesPage |
| POST /api/imgs/deleteImage | imgId 表单 | status/message；ImagesPage |
| GET /api/imgs/list | search 可选 | ImgDTO 数组；ImagesPage |
| GET /api/imgs/{id} | ID | ImgDetailsDTO |
| POST /api/vms/createModel | vmName、size、isoFileName 表单 | status/message；VmsPage |
| POST /api/vms/createClone | vmName、imgId 表单 | status/message；VmsPage |
| GET /api/vms/{id} | ID | VmDetailsDTO |
| POST /api/vms/{vmName}/start | 名称 | status/message；VmsPage |
| POST /api/vms/{vmName}/shutdown | 名称 | status/message；VmsPage |
| POST /api/vms/{vmName}/restart | 名称 | status/message；VmsPage |
| DELETE /api/vms/{vmName}/delete | 名称 | status/message；VmsPage |
| GET /api/vms/list | search 可选 | VmDTO 数组；VmsPage |
| POST /api/version/createBackup | poolName、versionName、versionDescription 表单 | status/message；VersionsPage |
| POST /api/version/restoreBackup | poolName、versionId 表单 | status/message；VersionsPage |
| GET /api/version/list | search 可选 | VersionDTO 数组；VersionsPage |
| GET /api/version/pools | 无 | PoolDTO 数组；VersionsPage |

## 必须保持或显式变更的契约

- Session cookie 配合 `credentials: include`；非 GET/HEAD/OPTIONS 请求由 api.js 自动取 CSRF token 并设置请求头。现有代码未禁用 CSRF。
- 正常查询数据保持字段；分页不擅自从 Page 改成普通数组。用户更新、注册与其他表单创建的 content type 不能混用。
- 现在错误使用 400/403/404/500 和文本/Map 混合，属于当前观察，不是未来必须保留的错误设计。稳定错误码、409 冲突在切片内与 UI 同步。
- `shutdown` 目前服务端执行强制销毁；目标改为真正关机请求、202、STOPPING 并新增 destroy，属于已授权行为变更，不能只搬代码。
- `parentName` 改 parentId 时同步 ImagesPage；旧接口尚在时维持名称解析无歧义，再考虑池内同名。
- 同步创建的成功不等于后续任务提交成功。异步 202/operationId 在 F1 实施，不在本批修改 UI。

## 场景基线与后续测试归属

| 场景 | 当前依据 / 已知缺口 | 目标验证批次 |
| --- | --- | --- |
| 登录→查询列表→携带 CSRF 写入 | SecurityConfig、api.js 已实现主要机制，未运行验证 | R1/R2 |
| 管理员管理账户 | /api/users/** 限 ADMIN；尚无完整删除资源流程 | R2/R7 |
| 创建池+基础镜像+模板 VM | VMController/Service 编排混合远程与写库 | R6 |
| 创建增量并关联用户/池/父 | ImageController/Service；祖先权限和占用未统一 | R3/R5 |
| 一镜像多 VM，祖先读写互斥 | 表关系允许多关联；状态锁不能代表完整占用组 | R4/R6 |
| 已删除 B 仍支撑 A→B→C 中的 C | ChildService 仅查直接未删除孩子不足以证明物理删除安全 | R5 |
| 池备份与恢复 | 版本所属池检查存在，池授权未闭环；脚本未取得 | R8 |
| 外部失败后不返回假成功 | SSH 的错误返回和上层状态提交需重构 | R1/R4 |

测试资产：VMControllerTest 为空；VivmApplicationTests 仅 contextLoads，启动会进入真实连接 Bean。不能把现有测试资产作为上述场景已通过的证据。
