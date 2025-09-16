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