#!/bin/bash
# ============================================================
# 采云台 —— nginx 一键重启脚本
#
# 用途：把占用 8082/8083 的旧 nginx 进程杀掉，再用新路径干净启动。
#
# 安全性：只 kill "监听 8082/8083" 的进程，绝不触碰 8080 上
#         黑马点评的 nginx，也不会误杀本项目后端(8084)。
#
# 用法：bash /Users/eason/Desktop/caiyuntai/front/restart-nginx.sh
# ============================================================

NGX="/Users/eason/Desktop/caiyuntai/front/nginx-1.20.2"
CONF="conf/nginx-mac.conf"

echo "=========================================="
echo " 采云台 nginx 重启"
echo "=========================================="
echo ""

# ---------- 1. 杀掉占用 8082 / 8083 的 nginx ----------
echo "[1/4] 检查并清理占用 8082/8083 的进程..."

PIDS=$(lsof -nP -iTCP:8082 -sTCP:LISTEN -t 2>/dev/null | sort -u)
PIDS="$PIDS $(lsof -nP -iTCP:8083 -sTCP:LISTEN -t 2>/dev/null | sort -u)"
PIDS=$(echo "$PIDS" | tr ' ' '\n' | grep -E '^[0-9]+$' | sort -u)

if [ -z "$PIDS" ]; then
  echo "      8082/8083 当前没有被占用，无需清理"
else
  for pid in $PIDS; do
    CMD=$(ps -p "$pid" -o command= 2>/dev/null)
    case "$CMD" in
      *nginx*)
        echo "      发现 nginx 进程 $pid，正在停止..."
        kill "$pid" 2>/dev/null
        ;;
      *)
        echo "      ⚠️  跳过非 nginx 进程 $pid：$CMD"
        ;;
    esac
  done

  sleep 2

  STILL=$(lsof -nP -iTCP:8082 -sTCP:LISTEN -t 2>/dev/null | sort -u)
  STILL="$STILL $(lsof -nP -iTCP:8083 -sTCP:LISTEN -t 2>/dev/null | sort -u)"
  STILL=$(echo "$STILL" | tr ' ' '\n' | grep -E '^[0-9]+$' | sort -u)
  for pid in $STILL; do
    echo "      进程 $pid 未退出，强制结束..."
    kill -9 "$pid" 2>/dev/null
  done
  sleep 1
fi

LEFT=$(lsof -nP -iTCP:8082 -sTCP:LISTEN -t 2>/dev/null | wc -l | tr -d ' ')
LEFT2=$(lsof -nP -iTCP:8083 -sTCP:LISTEN -t 2>/dev/null | wc -l | tr -d ' ')
if [ "$LEFT" != "0" ] || [ "$LEFT2" != "0" ]; then
  echo "      ❌ 端口仍被占用，请手工检查："
  lsof -nP -iTCP:8082 -sTCP:LISTEN
  lsof -nP -iTCP:8083 -sTCP:LISTEN
  exit 1
fi
echo "      ✅ 8082 / 8083 已释放"
echo ""

# ---------- 2. 检查配置语法 ----------
echo "[2/4] 检查 nginx 配置语法..."
if nginx -p "$NGX" -c "$CONF" -t 2>&1 | grep -q "test is successful"; then
  echo "      ✅ 配置语法正确"
else
  echo "      ❌ 配置有语法错误："
  nginx -p "$NGX" -c "$CONF" -t 2>&1 | grep -v "could not open error log"
  exit 1
fi
echo ""

# ---------- 3. 启动 ----------
echo "[3/4] 启动 nginx..."
nginx -p "$NGX" -c "$CONF" 2>&1 | grep -v "could not open error log"
sleep 2

if lsof -nP -iTCP:8082 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "      ✅ nginx 已启动"
else
  echo "      ❌ 启动失败，错误日志如下："
  tail -15 "$NGX/logs/error.log" 2>/dev/null
  exit 1
fi
echo ""

# ---------- 4. 验证 ----------
echo "[4/4] 验证前端可访问性..."
ADMIN=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://localhost:8082/ 2>/dev/null)
STAFF=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://localhost:8083/ 2>/dev/null)
echo "      管理端 :8082 → HTTP $ADMIN"
echo "      员工端 :8083 → HTTP $STAFF"
echo ""

BACK=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://127.0.0.1:8084/admin/employee/login 2>/dev/null)
echo "=========================================="
if [ "$BACK" = "000" ]; then
  echo " ⚠️  后端(:8084)未启动"
  echo "    页面能打开，但接口会报 502。"
  echo "    请在 IDEA 里运行 CaiyuntaiApplication，"
  echo "    Program arguments 填：--server.port=8084"
else
  echo " ✅ 后端(:8084)已在运行 → HTTP $BACK"
fi
echo ""
echo " 管理端：http://localhost:8082   (admin / 123456)"
echo " 员工端：http://localhost:8083   (zhangsan / 123456)"
echo "=========================================="
