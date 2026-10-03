#!/bin/bash
# 启动「苍穹外卖」前端运行环境（macOS 原生 nginx）
# 用法：./start-nginx-mac.sh
set -e

PREFIX="$(cd "$(dirname "$0")" && pwd)"
CONF="conf/nginx-mac.conf"

if ! command -v nginx >/dev/null 2>&1; then
  echo "未找到 nginx，请先安装： brew install nginx"
  exit 1
fi

# 已经跑着就先停掉，避免端口占用。
# 这里按端口判断而不是看 logs/nginx.pid：pid 文件可能是上次崩溃的残留，
# 对着死 pid 发信号有误伤其他进程的风险。
if lsof -nP -iTCP:8082 -sTCP:LISTEN >/dev/null 2>&1 || lsof -nP -iTCP:8083 -sTCP:LISTEN >/dev/null 2>&1; then
  nginx -p "$PREFIX" -c "$CONF" -s quit 2>/dev/null || true
  sleep 1
fi

# -p 指定运行目录（资料包根目录），-c 指定 macOS 版配置
nginx -p "$PREFIX" -c "$CONF" 2>/dev/null

sleep 1
ADMIN=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://127.0.0.1:8082/ 2>/dev/null || true)
STAFF=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://127.0.0.1:8083/ 2>/dev/null || true)
if [ "$ADMIN" = "200" ] && [ "$STAFF" = "200" ]; then
  echo "✅ nginx 已启动"
  echo "   管理端： http://localhost:8082"
  echo "   员工端： http://localhost:8083"
  echo "   停止：   ./stop-nginx-mac.sh"
else
  echo "❌ 启动失败（8082=$ADMIN  8083=$STAFF），看日志： $PREFIX/logs/error.log"
  exit 1
fi
