# 文档自动同步机制指南

> 📋 确保每次开发任务完成后，相关文档都能及时更新同步

## 🎯 文档同步原则

### 1. 开发即文档 (Documentation as Code)
- 每个服务的代码变更，必须同时更新对应的文档
- 文档与代码在同一个仓库中，保持版本一致性
- 使用Markdown格式，便于版本控制和协作

### 2. 多层级文档体系
```
HavenButler/
├── README.md                    # 🔄 项目总体介绍 (每次发版更新)
├── CLAUDE.md                   # 🔄 开发指导文档 (架构变更时更新)
├── DEVELOPMENT_PROGRESS.md     # 🔄 开发进度总览 (每次开发完成更新)
├── dev-dashboard/
│   └── project-overview.md     # 🔄 项目总览面板 (实时更新)
├── services/*/
│   ├── README.md              # 🔄 服务说明文档 (服务变更时更新)
│   ├── dev-panel.md           # 🔄 开发状态面板 (开发过程中实时更新)
│   └── CLAUDE.md              # 🔄 服务开发指导 (架构调整时更新)
└── infrastructure/*/
    ├── README.md              # 🔄 组件说明文档 (组件变更时更新)
    └── CLAUDE.md              # 🔄 组件开发指导 (设计变更时更新)
```

## 📋 文档更新检查清单

### 🚀 每次开发任务完成后必须更新

#### 1. 服务级文档更新
- [ ] **services/{service-name}/dev-panel.md**
  - 更新任务进度状态
  - 记录完成的功能模块
  - 更新技术债务清单
  - 更新测试状态
  - 更新依赖服务状态

- [ ] **services/{service-name}/README.md**
  - 新增API接口文档
  - 更新配置说明
  - 更新部署信息
  - 同步Infrastructure集成变更

#### 2. 项目级文档更新
- [ ] **dev-dashboard/project-overview.md**
  - 更新任务进度跟踪表
  - 更新里程碑完成状态
  - 添加最近更新记录
  - 更新开发统计数据

- [ ] **DEVELOPMENT_PROGRESS.md**
  - 更新重大里程碑状态
  - 更新技术实现统计
  - 更新代码质量指标
  - 更新后续开发规划

#### 3. 根目录文档更新
- [ ] **README.md**
  - 更新项目结构状态标识
  - 更新开发进度统计
  - 更新技术栈信息
  - 更新快速开始指南

- [ ] **CLAUDE.md**
  - 更新当前开发状态
  - 更新常用命令
  - 更新项目结构说明
  - 添加新的开发规范

## ⚡ 自动化同步脚本

### 1. 文档状态检查脚本
```bash
#!/bin/bash
# scripts/check-docs-sync.sh

echo "🔍 检查文档同步状态..."

# 检查各服务dev-panel.md是否存在
for service in gateway services/*/; do
    if [ -d "$service" ]; then
        service_name=$(basename "$service")
        if [ ! -f "$service/dev-panel.md" ]; then
            echo "❌ 缺少开发面板: $service_name/dev-panel.md"
        else
            echo "✅ 开发面板完整: $service_name"
        fi
    fi
done

# 检查Infrastructure集成说明是否同步
for service in services/*/README.md; do
    if ! grep -q "Infrastructure集成" "$service"; then
        echo "❌ 缺少Infrastructure集成说明: $service"
    else
        echo "✅ Infrastructure文档完整: $(dirname $service)"
    fi
done

echo "📊 文档同步检查完成"
```

### 2. 文档更新提醒脚本
```bash
#!/bin/bash
# scripts/update-docs-reminder.sh

echo "📋 文档更新检查清单:"
echo ""
echo "🔄 必须更新的文档:"
echo "  1. 服务开发面板: services/*/dev-panel.md"
echo "  2. 项目总览面板: dev-dashboard/project-overview.md"
echo "  3. 开发进度总览: DEVELOPMENT_PROGRESS.md"
echo ""
echo "🔄 可能需要更新的文档:"
echo "  4. 服务README: services/*/README.md"
echo "  5. 项目README: README.md"
echo "  6. 开发指导: CLAUDE.md"
echo ""
echo "✨ 提示: 请根据本次开发内容，更新相应的文档"
```

### 3. 文档版本同步脚本
```bash
#!/bin/bash
# scripts/sync-doc-versions.sh

# 获取当前git提交信息
COMMIT_ID=$(git rev-parse --short HEAD)
COMMIT_DATE=$(date +"%Y-%m-%d")

echo "🔄 同步文档版本信息..."

# 更新DEVELOPMENT_PROGRESS.md的最后更新时间
sed -i "s/> 📅 最后更新时间：.*/> 📅 最后更新时间：$COMMIT_DATE/" DEVELOPMENT_PROGRESS.md

# 更新各服务dev-panel.md的链接
for service in services/*/dev-panel.md; do
    if [ -f "$service" ]; then
        # 确保开发日志链接是最新的
        sed -i "s|../../dev-logs/.*|../../dev-logs/$COMMIT_DATE/|g" "$service"
    fi
done

echo "✅ 文档版本信息已更新 (Commit: $COMMIT_ID)"
```

## 🔧 VS Code/IDE 集成

### 1. 任务模板 (.vscode/tasks.json)
```json
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "更新文档",
            "type": "shell",
            "command": "./scripts/update-docs-reminder.sh",
            "group": "build",
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            }
        },
        {
            "label": "检查文档同步",
            "type": "shell",
            "command": "./scripts/check-docs-sync.sh",
            "group": "test"
        }
    ]
}
```

### 2. 代码片段 (.vscode/snippets.json)
```json
{
    "更新dev-panel任务": {
        "prefix": "update-dev-panel",
        "body": [
            "| ${1:taskId} | ${2:任务描述} | ${3:开发中} | ${4:70}% | ${5:01-20} | ${6:无} |"
        ],
        "description": "添加dev-panel任务行"
    },
    "更新项目进度": {
        "prefix": "update-progress",
        "body": [
            "- ${CURRENT_YEAR}-${CURRENT_MONTH}-${CURRENT_DATE}: ${1:完成功能描述}"
        ],
        "description": "添加项目进度记录"
    }
}
```

## 📊 文档质量监控

### 1. 文档完整性指标
- **覆盖率**: 所有服务都有完整的README.md和dev-panel.md
- **同步率**: Infrastructure集成说明在所有服务中保持一致
- **更新频率**: 开发面板至少每周更新一次状态

### 2. 文档质量标准
- **准确性**: 文档内容与实际代码实现一致
- **完整性**: 包含所有必要的技术信息和使用说明
- **可读性**: 使用清晰的Markdown格式和结构化布局
- **时效性**: 及时反映最新的开发进展

## 🎯 最佳实践

### 1. 开发流程集成
```bash
# 开发完成后的标准流程
1. 完成代码开发
2. 更新服务dev-panel.md
3. 运行文档同步检查
4. 更新项目级文档
5. 提交代码和文档
```

### 2. 团队协作规范
- **责任明确**: 每个开发者负责自己开发服务的文档更新
- **同步约定**: 每次git提交必须包含相应的文档更新
- **Review机制**: PR Review时必须检查文档同步情况
- **定期检查**: 每周进行一次文档完整性检查

### 3. 工具推荐
- **Markdown编辑器**: Typora、Mark Text
- **图表工具**: Mermaid、PlantUML
- **版本控制**: Git hooks自动检查文档更新
- **CI/CD集成**: 自动化文档同步验证

---

## 📞 使用说明

每次完成开发任务后，请按照以下步骤更新文档：

1. **运行检查脚本**: `./scripts/check-docs-sync.sh`
2. **使用提醒清单**: `./scripts/update-docs-reminder.sh`
3. **更新相关文档**: 根据检查清单逐项完成
4. **同步版本信息**: `./scripts/sync-doc-versions.sh`
5. **提交变更**: git commit包含代码和文档

这样可以确保文档始终与代码保持同步，为项目的长期维护提供可靠的文档基础。