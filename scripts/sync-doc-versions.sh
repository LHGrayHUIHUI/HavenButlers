#!/bin/bash
# scripts/sync-doc-versions.sh

# 获取当前git提交信息
COMMIT_ID=$(git rev-parse --short HEAD)
COMMIT_DATE=$(date +"%Y-%m-%d")

echo "🔄 同步文档版本信息..."

# 更新DEVELOPMENT_PROGRESS.md的最后更新时间
sed -i "" "s/> 📅 最后更新时间：.*/> 📅 最后更新时间：$COMMIT_DATE/" DEVELOPMENT_PROGRESS.md

# 更新各服务dev-panel.md的链接
for service in services/*/dev-panel.md; do
    if [ -f "$service" ]; then
        # 确保开发日志链接是最新的
        sed -i "" "s|../../dev-logs/.*|../../dev-logs/$COMMIT_DATE/|g" "$service"
    fi
done

echo "✅ 文档版本信息已更新 (Commit: $COMMIT_ID)"