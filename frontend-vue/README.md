# 图书馆预约管理系统 - Vue3 前端

> Vue3 + TypeScript + Arco Design + Vite + Pinia + GSAP

## 技术栈

- **框架**: Vue 3.4+ (Composition API)
- **语言**: TypeScript 5.3+
- **构建工具**: Vite 5.0+
- **UI 框架**: Arco Design Vue 2.55+
- **状态管理**: Pinia 2.1+
- **路由**: Vue Router 4.2+
- **HTTP 请求**: Axios 1.6+
- **动画库**: GSAP 3.12+

## 项目结构

```
frontend-vue/
├── public/                # 静态资源
├── src/
│   ├── api/              # API 接口
│   │   ├── index.ts     # 接口定义
│   │   └── request.ts   # Axios 封装
│   ├── assets/           # 资源文件
│   ├── components/       # 组件
│   │   ├── AccountForm.vue       # 账号表单
│   │   ├── AccountList.vue       # 账号列表
│   │   └── AccountDetail.vue     # 账号详情
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── stores/           # Pinia 状态管理
│   │   └── account.ts   # 账号状态
│   ├── styles/           # 样式
│   │   └── main.css     # 全局样式
│   ├── utils/            # 工具函数
│   ├── views/            # 页面
│   │   └── HomeView.vue # 主页
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── index.html            # HTML 模板
├── package.json          # 依赖配置
├── tsconfig.json         # TypeScript 配置
├── vite.config.ts        # Vite 配置
└── .env                  # 环境变量
```

## 快速开始

### 安装依赖

```bash
npm install
```

或使用国内镜像：

```bash
npm install --registry=https://registry.npmmirror.com
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://127.0.0.1:8080

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 一键启动（Windows）

双击 `启动Vue3前端.bat`，脚本会自动：
1. 安装依赖（如果需要）
2. 启动开发服务器
3. 自动打开浏览器

## 核心功能

### 账号管理
- ✅ 添加账号（表单验证）
- ✅ 编辑账号
- ✅ 删除账号（确认对话框）
- ✅ Token 自动解析
- ✅ Token 状态实时监控
- ✅ 多账号支持

### 预约功能
- ✅ 测试预约（不提交）
- ✅ 执行预约（确认对话框）
- ✅ 签到
- ✅ 自动刷新状态（60秒）

### UI/UX 特性
- ✅ Arco Design 组件库
- ✅ GSAP 流畅动画
- ✅ 响应式布局
- ✅ 空状态提示
- ✅ 加载状态
- ✅ Toast 消息提示
- ✅ 确认对话框

## API 配置

后端 API 地址在 `.env` 文件中配置：

```env
VITE_API_BASE_URL=http://127.0.0.1:5000
```

Vite 已配置代理，前端请求 `/api/*` 会自动转发到后端。

## 状态管理

使用 Pinia 管理账号状态：

```typescript
import { useAccountStore } from '@/stores/account'

const accountStore = useAccountStore()

// 获取账号列表
await accountStore.fetchAccounts()

// 选中账号
accountStore.selectAccount(id)

// 添加账号
await accountStore.addAccount(data)
```

## 组件说明

### AccountForm.vue
账号添加/编辑表单组件
- 表单验证
- 支持编辑模式
- 提交后自动刷新列表

### AccountList.vue
账号列表组件
- 卡片式设计
- 点击选中
- 显示 Token 状态
- 空状态提示

### AccountDetail.vue
账号详情组件
- 认证信息展示
- 预约配置展示
- 操作按钮（测试、预约、签到、删除）
- 确认对话框

## 动画效果

使用 GSAP 实现流畅动画：

```typescript
import gsap from 'gsap'

// 入场动画
gsap.from('.element', {
  opacity: 0,
  y: 30,
  duration: 0.8,
  ease: 'power3.out'
})
```

## 样式定制

Arco Design 主题定制在 `src/styles/main.css`：

```css
/* 主题色 */
.arco-btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

/* 卡片圆角 */
.arco-card {
  border-radius: 16px;
}
```

## 构建优化

### 代码分割
Vite 自动进行代码分割，组件按需加载。

### Tree Shaking
生产构建自动移除未使用的代码。

### 压缩
生产构建自动压缩 JavaScript 和 CSS。

## 浏览器支持

- Chrome >= 90
- Edge >= 90
- Firefox >= 88
- Safari >= 14

## 开发建议

### TypeScript
使用 TypeScript 获得类型提示和代码检查。

### 组件命名
- 组件文件使用 PascalCase：`AccountForm.vue`
- 组件使用时使用 kebab-case：`<account-form />`

### 代码风格
- 使用 Composition API
- 使用 `<script setup>` 语法
- 使用 TypeScript 类型注解

## 常见问题

### Q: 依赖安装失败？
A: 使用国内镜像：
```bash
npm install --registry=https://registry.npmmirror.com
```

### Q: 无法连接后端？
A: 确认后端服务已启动（http://127.0.0.1:5000）

### Q: 端口被占用？
A: 修改 `vite.config.ts` 中的端口配置

### Q: 样式不生效？
A: 检查 Arco Design CSS 是否正确导入

## 与原前端对比

| 特性 | 原版本 | Vue3 版本 |
|------|--------|-----------|
| 框架 | 原生 JS | Vue3 + TypeScript |
| UI | 手写 CSS | Arco Design |
| 状态 | 全局变量 | Pinia 状态管理 |
| 路由 | 单页面 | Vue Router |
| 构建 | 无 | Vite |
| 类型 | 无 | TypeScript |
| 组件化 | 无 | 完全组件化 |

## 后续优化

- [ ] 添加单元测试（Vitest）
- [ ] 添加 E2E 测试（Playwright）
- [ ] 性能监控
- [ ] PWA 支持
- [ ] 国际化（i18n）

---

**版本**: 2.0  
**更新时间**: 2026-07-05  
**技术栈**: Vue3 + TypeScript + Arco Design
