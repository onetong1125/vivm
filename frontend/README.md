# VIVM 前端

该目录是独立的 Vue 3 应用。开发服务器将 `/api` 代理到 `http://localhost:8081`，因此浏览器仍以同源方式使用 Session Cookie 和 CSRF token。

```bash
npm install
npm run dev
```

生产环境由 Web 服务器托管 `npm run build` 生成的 `dist/`，并将 `/api` 反向代理到 Spring Boot。不要让浏览器直接跨域访问后端；这样无需为 Cookie、CORS 和 CSRF 设置额外的跨域例外。
