#!/bin/bash
# 停止「苍穹外卖」前端运行环境
set -e

PREFIX="$(cd "$(dirname "$0")" && pwd)"
CONF="conf/nginx-mac.conf"

if [ -f "$PREFIX/logs/nginx.pid" ]; then
  nginx -p "$PREFIX" -c "$CONF" -s stop 2>/dev/null || kill "$(cat "$PREFIX/logs/nginx.pid")" 2>/dev/null || true
  echo "已停止 nginx"
else
  echo "nginx 没有在运行（找不到 logs/nginx.pid）"
fi
