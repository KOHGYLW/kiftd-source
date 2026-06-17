# 企业授权管理系统前端

基于 Vue.js 3 + TypeScript + Element Plus 构建的现代化企业授权管理系统前端。

## 技术栈

- **框架**: Vue.js 3 (Composition API)
- **语言**: TypeScript
- **构建工具**: Vite
- **UI组件库**: Element Plus
- **路由**: Vue Router 4
- **状态管理**: Pinia
- **HTTP客户端**: Axios
- **代码规范**: ESLint + Prettier
- **样式预处理**: CSS

## 项目特性

- ✅ 基于 Vue 3 Composition API
- ✅ TypeScript 完整类型支持
- ✅ Element Plus 企业级UI组件
- ✅ 响应式设计，支持移动端
- ✅ 路由权限控制
- ✅ 统一的API请求封装
- ✅ 完善的错误处理机制
- ✅ 代码规范化工具配置
- ✅ 开发环境热更新

## 项目结构

```
src/
├── api/              # API接口封装
│   ├── auth.ts       # 认证相关接口
│   ├── customer.ts   # 客户管理接口
│   ├── license.ts    # 授权管理接口
│   ├── dashboard.ts  # 仪表板接口
│   └── request.ts    # HTTP请求配置
├── components/       # 公共组件
│   ├── common/       # 通用组件
│   └── layout/       # 布局组件
├── router/           # 路由配置
│   └── index.ts      # 路由定义
├── store/            # 状态管理
│   ├── user.ts       # 用户状态
│   ├── app.ts        # 应用状态
│   └── index.ts      # Store入口
├── styles/           # 样式文件
│   └── index.css     # 全局样式
├── types/            # TypeScript类型定义
│   └── index.ts      # 类型声明
├── utils/            # 工具函数
│   ├── auth.ts       # 认证工具
│   └── index.ts      # 通用工具
├── views/            # 页面组件
│   ├── Login.vue     # 登录页面
│   ├── Dashboard.vue # 仪表板页面
│   ├── CustomerManagement.vue # 客户管理
│   ├── LicenseManagement.vue  # 授权管理
│   ├── Settings.vue  # 系统设置
│   └── NotFound.vue  # 404页面
├── App.vue           # 根组件
└── main.ts           # 应用入口
```

## 开发环境

### 环境要求

- Node.js >= 16
- npm >= 8 或 yarn >= 1.22

### 安装依赖

```bash
npm install
```

### 开发服务器

```bash
npm run dev
```

启动后访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 代码检查

```bash
npm run lint
```

### 代码格式化

```bash
npm run format
```

## 功能模块

### 1. 用户认证
- 用户登录/登出
- Token管理
- 路由权限控制

### 2. 仪表板
- 关键数据统计
- 图表展示
- 即将过期授权提醒

### 3. 客户管理
- 客户列表查看
- 新增/编辑/删除客户
- 客户信息搜索和筛选
- 批量操作

### 4. 授权管理
- 授权列表管理
- 生成授权密钥
- 授权状态控制（激活/暂停）
- 授权续期
- 授权导出

### 5. 系统设置
- 基础设置配置
- 邮件服务配置
- 通知设置

## 环境变量

开发环境 (`.env.development`):
```
VITE_NODE_ENV=development
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_TITLE=企业授权管理系统
VITE_APP_VERSION=1.0.0
```

生产环境 (`.env.production`):
```
VITE_NODE_ENV=production
VITE_API_BASE_URL=/api
VITE_APP_TITLE=企业授权管理系统
VITE_APP_VERSION=1.0.0
```

## API接口

项目使用统一的API接口封装，支持：

- 请求/响应拦截器
- 统一错误处理
- Token自动添加
- 请求超时处理
- 接口代理配置

## 部署

### 构建

```bash
npm run build
```

构建完成后，`dist` 目录包含所有静态文件。

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /path/to/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend-server:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## 开发规范

### 代码风格
- 使用 ESLint + Prettier 统一代码风格
- 使用 TypeScript 严格模式
- 组件采用 Composition API
- 使用单文件组件 (SFC)

### 命名规范
- 组件名：PascalCase
- 文件名：PascalCase (组件) 或 kebab-case (其他)
- 变量名：camelCase
- 常量名：UPPER_SNAKE_CASE

### Git 提交规范
- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- style: 代码格式调整
- refactor: 代码重构
- test: 测试相关
- chore: 构建过程或辅助工具的变动

## 许可证

MIT License