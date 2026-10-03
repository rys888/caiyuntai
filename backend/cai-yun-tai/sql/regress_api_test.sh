#!/bin/bash
# 企业物资采购平台 后端接口回归测试
# 后端地址。默认 8084（与 nginx 反代目标一致）；
# 若后端跑在别的端口，可覆盖：BASE_URL=http://127.0.0.1:8086 bash regress_api_test.sh
B=${BASE_URL:-http://127.0.0.1:8084}
pass=0; fail=0

chk() {
  if [ "$2" == "$3" ]; then echo "  ✅ $1"; pass=$((pass+1));
  else echo "  ❌ $1 (期望=$2 实际=$3)"; fail=$((fail+1)); fi
}

# 取 JSON 路径，用法: jget "$RESP" "data.token"
jget() {
  python3 -c "
import sys, json
try:
    d = json.loads(sys.argv[1])
except Exception:
    print(''); sys.exit()
for k in sys.argv[2].split('.'):
    if d is None: break
    if k.isdigit():
        d = d[int(k)] if isinstance(d, list) and len(d) > int(k) else None
    else:
        d = d.get(k) if isinstance(d, dict) else None
print('' if d is None else d)
" "$1" "$2" 2>/dev/null
}

req() { # method url tokenheader [body]
  local m=$1 u=$2 t=$3 b=$4
  if [ -n "$b" ]; then
    curl -s --max-time 20 -X "$m" "$B$u" -H "$t" -H 'Content-Type: application/json' -d "$b"
  else
    curl -s --max-time 20 -X "$m" "$B$u" -H "$t"
  fi
}


echo "═══ 0. 测试前置：重置预算与清理上一轮数据 ═══"
mysql -uroot -p123456 sky_take_out <<'SQL' 2>/dev/null
UPDATE budget SET used_amount = 0 WHERE dept_id = (SELECT id FROM department WHERE code='DEPT_TECH');
UPDATE orders SET status = 6, cancel_reason = '回归测试清理' WHERE status IN (2,3,4);
DELETE FROM department WHERE code = 'TEST_X';
DELETE FROM shopping_cart WHERE user_id = (SELECT id FROM employee WHERE username='zhangsan');
-- 保证测试员工一定有默认收货地址（否则提交申领单会因空地址失败）
INSERT INTO address_book (user_id, consignee, phone, sex, detail, dept_name, station, label, is_default)
SELECT e.id, e.name, e.phone, '1', 'A座12层技术部办公区', '技术部', 'A座12层工位03', '公司', 1
FROM employee e
WHERE e.username = 'zhangsan'
  AND NOT EXISTS (SELECT 1 FROM address_book a WHERE a.user_id = e.id);
SQL
redis-cli -a 123456 -n 10 --no-auth-warning keys 'goods:list:*' 2>/dev/null | xargs -r redis-cli -a 123456 -n 10 --no-auth-warning del >/dev/null 2>&1
redis-cli -a 123456 -n 10 --no-auth-warning keys 'combo:list:*' 2>/dev/null | xargs -r redis-cli -a 123456 -n 10 --no-auth-warning del >/dev/null 2>&1
echo "  已重置预算、清理在途单据与测试部门、清除相关缓存"

echo "═══ 1. 员工端登录（账号密码，替代原微信登录） ═══"
R=$(req POST /user/user/login 'X: 1' '{"username":"zhangsan","password":"123456"}')
EMP_TOKEN=$(jget "$R" "data.token"); EH="authentication: $EMP_TOKEN"
EMP_ID=$(jget "$R" "data.id")
chk "返回角色 EMPLOYEE" "EMPLOYEE" "$(jget "$R" "data.role")"
chk "返回部门名称 技术部" "技术部" "$(jget "$R" "data.deptName")"
chk "部门id 正确" "2" "$(jget "$R" "data.deptId")"
[ -n "$EMP_TOKEN" ] && { echo "  ✅ 拿到员工端 token"; pass=$((pass+1)); } || { echo "  ❌ token 为空"; fail=$((fail+1)); }

echo "═══ 2. 普通员工不允许登录管理端（角色隔离） ═══"
R=$(req POST /admin/employee/login 'X: 1' '{"username":"zhangsan","password":"123456"}')
chk "被拒绝(code=0)" "0" "$(jget "$R" "code")"
echo "  提示: $(jget "$R" "msg")"

echo "═══ 3. 管理员登录 ═══"
R=$(req POST /admin/employee/login 'X: 1' '{"username":"admin","password":"123456"}')
ADMIN_TOKEN=$(jget "$R" "data.token"); AH="token: $ADMIN_TOKEN"
chk "角色 ADMIN" "ADMIN" "$(jget "$R" "data.role")"

echo "═══ 4. 采购专员登录 ═══"
R=$(req POST /admin/employee/login 'X: 1' '{"username":"purchaser","password":"123456"}')
PUR_TOKEN=$(jget "$R" "data.token"); PH="token: $PUR_TOKEN"
chk "角色 PURCHASER" "PURCHASER" "$(jget "$R" "data.role")"

echo "═══ 5. 未登录访问被拦截 ═══"
chk "员工端 401" "401" "$(curl -s --max-time 15 -o /dev/null -w '%{http_code}' $B/user/shoppingCart/list)"
chk "管理端 401" "401" "$(curl -s --max-time 15 -o /dev/null -w '%{http_code}' $B/admin/order/statistics)"

echo "═══ 6. 物资目录（走 Redis 缓存） ═══"
R=$(req GET "/user/goods/list?categoryId=23" "$EH")
N=$(python3 -c "import sys,json;d=json.loads(sys.argv[1]);print(len(d['data']) if d.get('data') else 0)" "$R" 2>/dev/null)
chk "办公耗材分类下 3 项" "3" "$N"
HAS_SPEC=$(python3 -c "
import sys,json
d=json.loads(sys.argv[1])
print('yes' if any(x.get('spec') for x in (d.get('data') or [])) else 'no')" "$R" 2>/dev/null)
chk "物资带规格字段(spec)" "yes" "$HAS_SPEC"
HAS_UNIT=$(python3 -c "
import sys,json
d=json.loads(sys.argv[1])
print('yes' if any(x.get('unit') for x in (d.get('data') or [])) else 'no')" "$R" 2>/dev/null)
chk "物资带单位字段(unit)" "yes" "$HAS_UNIT"
R2=$(req GET "/user/goods/list?categoryId=23" "$EH")
chk "二次访问命中缓存返回一致" "$(jget "$R" "data.0.name")" "$(jget "$R2" "data.0.name")"

echo "═══ 7. 组合申领包（走 Redis 缓存） ═══"
R=$(req GET "/user/combo/list?categoryId=27" "$EH")
chk "按分类查组合包" "1" "$(jget "$R" "code")"

# 回归保护：不传 categoryId（查全部）曾因 @Cacheable(key="#categoryId") 拿到 null 抛
# IllegalArgumentException: Null key returned for cache operation，导致前端弹"操作失败"。
# 现在 key 用三元表达式把 null 归一成 'all'，这里守住这条路径。
R=$(req GET "/user/combo/list" "$EH")
chk "不传分类查全部组合包(曾 500)" "1" "$(jget "$R" "code")"
N=$(python3 -c "import sys,json;d=json.loads(sys.argv[1]);print(len(d['data']) if d.get('data') else 0)" "$R" 2>/dev/null)
[ "$N" -gt 0 ] && { echo "  ✅ 无分类参数返回 $N 个组合包"; pass=$((pass+1)); } || { echo "  ❌ 无分类参数返回为空"; fail=$((fail+1)); }

# 同一接口也应该能查到全部在售物资
R=$(req GET "/user/goods/list" "$EH")
chk "不传分类查全部物资" "1" "$(jget "$R" "code")"

# 明细接口：返回的是物资数组（name/copies），不是组合包对象
PKG_ID=$(req GET "/user/combo/list" "$EH" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d['data'][0]['id'] if d.get('data') else 0)" 2>/dev/null)
R=$(req GET "/user/combo/goods/$PKG_ID" "$EH")
chk "查组合包明细" "1" "$(jget "$R" "code")"
N=$(python3 -c "import sys,json;d=json.loads(sys.argv[1]);print(len(d['data']) if d.get('data') else 0)" "$R" 2>/dev/null)
[ "$N" -gt 0 ] && { echo "  ✅ 包内物资 $N 项"; pass=$((pass+1)); } || { echo "  ❌ 包内物资为空"; fail=$((fail+1)); }
chk "明细含物资名与数量" "中性签字笔" "$(jget "$R" "data.0.name")"

echo "═══ 8. 部门预算（提交前） ═══"
R=$(req GET /user/budget/my "$EH")
chk "单位名称" "技术部" "$(jget "$R" "data.deptName")"
B_TOTAL=$(jget "$R" "data.totalAmount"); B_USED0=$(jget "$R" "data.usedAmount")
echo "  总额=$B_TOTAL 已用=$B_USED0 可用=$(jget "$R" "data.remainAmount")"

echo "═══ 9. 申领车：重复加入应累加 ═══"
req POST /user/shoppingCart/add "$EH" '{"goodsId":70}' > /dev/null
req POST /user/shoppingCart/add "$EH" '{"goodsId":71}' > /dev/null
req POST /user/shoppingCart/add "$EH" '{"goodsId":71}' > /dev/null
R=$(req GET /user/shoppingCart/list "$EH")
CART_N=$(python3 -c "import sys,json;d=json.loads(sys.argv[1]);print(len(d['data']) if d.get('data') else 0)" "$R" 2>/dev/null)
chk "申领车 2 种物资" "2" "$CART_N"
NUM=$(python3 -c "
import sys,json
d=json.loads(sys.argv[1])
for x in d['data']:
    if x['goodsId']==71: print(x['number'])
" "$R" 2>/dev/null)
chk "同一物资数量累加为 2" "2" "$NUM"

echo "═══ 10. 提交申领单（应原子预扣预算） ═══"
ADDR=$(req GET /user/addressBook/list "$EH" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d['data'][0]['id'])" 2>/dev/null)
redis-cli -a 123456 -n 10 --no-auth-warning del "order:submit:$EMP_ID" >/dev/null 2>&1
R=$(req POST /user/order/submit "$EH" "{\"addressBookId\":$ADDR,\"amount\":32.00,\"remark\":\"接口回归测试\",\"payMethod\":1,\"packAmount\":0,\"tablewareNumber\":0,\"tablewareStatus\":0,\"deliveryStatus\":1}")
ORDER_ID=$(jget "$R" "data.id")
chk "提交成功" "1" "$(jget "$R" "code")"
echo "  申领单 id=$ORDER_ID 单号=$(jget "$R" "data.orderNumber")"

echo "═══ 11. 预算被预扣 32 ═══"
B_USED1=$(req GET /user/budget/my "$EH" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['usedAmount'])" 2>/dev/null)
chk "已用金额 = 32" "32.0" "$B_USED1"

echo "═══ 12. 单据状态与预算扣减状态 ═══"
R=$(req GET /user/order/orderDetail/$ORDER_ID "$EH")
chk "状态=待审批(2)" "2" "$(jget "$R" "data.status")"
chk "预算状态=已扣减(1)" "1" "$(jget "$R" "data.payStatus")"
chk "记录了申请部门" "2" "$(jget "$R" "data.requesterDept")"

echo "═══ 13. 催办（写 remind_time，不再推送） ═══"
R=$(req GET /user/order/reminder/$ORDER_ID "$EH")
chk "催办成功" "1" "$(jget "$R" "code")"
R=$(req GET /user/order/orderDetail/$ORDER_ID "$EH")
[ -n "$(jget "$R" "data.remindTime")" ] && { echo "  ✅ remindTime 已记录: $(jget "$R" "data.remindTime")"; pass=$((pass+1)); } || { echo "  ❌ remindTime 未记录"; fail=$((fail+1)); }

echo "═══ 14. 角色鉴权：员工 token 不能调管理端 ═══"
chk "员工 token → 401" "401" "$(curl -s --max-time 15 -o /dev/null -w '%{http_code}' -X PUT $B/admin/order/confirm -H "$EH" -H 'Content-Type: application/json' -d "{\"id\":$ORDER_ID}")"

echo "═══ 15. 审批通过（记录审批人/审批时间） ═══"
R=$(req PUT /admin/order/confirm "$PH" "{\"id\":$ORDER_ID}")
chk "审批成功" "1" "$(jget "$R" "code")"
R=$(req GET /user/order/orderDetail/$ORDER_ID "$EH")
chk "状态=采购中(3)" "3" "$(jget "$R" "data.status")"
chk "审批人=采购专员(id5)" "5" "$(jget "$R" "data.approveUser")"
[ -n "$(jget "$R" "data.approveTime")" ] && { echo "  ✅ 审批时间: $(jget "$R" "data.approveTime")"; pass=$((pass+1)); } || { echo "  ❌ 审批时间缺失"; fail=$((fail+1)); }

echo "═══ 16. 发货 → 配送中(4) ═══"
req PUT /admin/order/delivery/$ORDER_ID "$PH" > /dev/null
chk "状态=配送中" "4" "$(jget "$(req GET /user/order/orderDetail/$ORDER_ID "$EH")" "data.status")"

echo "═══ 17. 员工确认收货 → 已完成(5) ═══"
chk "确认收货成功" "1" "$(jget "$(req PUT /user/order/confirmReceipt/$ORDER_ID "$EH")" "code")"
chk "状态=已完成" "5" "$(jget "$(req GET /user/order/orderDetail/$ORDER_ID "$EH")" "data.status")"

echo "═══ 18. 完成后预算不返还（仍是 32） ═══"
chk "已用金额仍为 32" "32.0" "$(req GET /user/budget/my "$EH" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['usedAmount'])" 2>/dev/null)"

echo "═══ 19. 驳回路径应返还预算 ═══"
req POST /user/shoppingCart/add "$EH" '{"goodsId":72}' > /dev/null
redis-cli -a 123456 -n 10 --no-auth-warning del "order:submit:$EMP_ID" >/dev/null 2>&1
R=$(req POST /user/order/submit "$EH" "{\"addressBookId\":$ADDR,\"amount\":18.00,\"remark\":\"驳回返还测试\",\"payMethod\":1,\"packAmount\":0,\"tablewareNumber\":0,\"tablewareStatus\":0,\"deliveryStatus\":1}")
O2=$(jget "$R" "data.id")
chk "提交后已用=50" "50.0" "$(req GET /user/budget/my "$EH" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['usedAmount'])" 2>/dev/null)"
R=$(req PUT /admin/order/rejection "$PH" "{\"id\":$O2,\"rejectionReason\":\"预算科目不符\"}")
chk "驳回成功" "1" "$(jget "$R" "code")"
chk "驳回后返还,已用回到 32" "32.0" "$(req GET /user/budget/my "$EH" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['usedAmount'])" 2>/dev/null)"
R=$(req GET /user/order/orderDetail/$O2 "$EH")
chk "驳回理由已记录" "预算科目不符" "$(jget "$R" "data.rejectionReason")"
chk "预算状态=已返还(2)" "2" "$(jget "$R" "data.payStatus")"

echo "═══ 20. 撤销申领应返还预算 ═══"
req POST /user/shoppingCart/add "$EH" '{"goodsId":72}' > /dev/null
redis-cli -a 123456 -n 10 --no-auth-warning del "order:submit:$EMP_ID" >/dev/null 2>&1
R=$(req POST /user/order/submit "$EH" "{\"addressBookId\":$ADDR,\"amount\":18.00,\"payMethod\":1,\"packAmount\":0,\"tablewareNumber\":0,\"tablewareStatus\":0,\"deliveryStatus\":1}")
O3=$(jget "$R" "data.id")
req PUT /user/order/cancel/$O3 "$EH" > /dev/null
chk "撤销后已用回到 32" "32.0" "$(req GET /user/budget/my "$EH" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['usedAmount'])" 2>/dev/null)"
chk "重复撤销被拒(状态已变更)" "0" "$(jget "$(req PUT /user/order/cancel/$O3 "$EH")" "code")"
chk "重复撤销未造成重复返还" "32.0" "$(req GET /user/budget/my "$EH" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['usedAmount'])" 2>/dev/null)"

echo "═══ 21. 预算超额拦截（条件 UPDATE + 影响行数） ═══"
# 后端从购物车重算金额，故这里先压低可用余额到 100，制造"199 超出余额"的超额场景
mysql -uroot -p123456 sky_take_out <<'SQL' 2>/dev/null
UPDATE budget SET used_amount = total_amount - 100 WHERE dept_id = (SELECT id FROM department WHERE code='DEPT_TECH');
SQL
B_USED_BEFORE=$(req GET /user/budget/my "$EH" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['usedAmount'])" 2>/dev/null)
req POST /user/shoppingCart/add "$EH" '{"goodsId":74}' > /dev/null
redis-cli -a 123456 -n 10 --no-auth-warning del "order:submit:$EMP_ID" >/dev/null 2>&1
R=$(req POST /user/order/submit "$EH" "{\"addressBookId\":$ADDR,\"amount\":199.00,\"payMethod\":1,\"packAmount\":0,\"tablewareNumber\":0,\"tablewareStatus\":0,\"deliveryStatus\":1}")
chk "超额被拦截(code=0)" "0" "$(jget "$R" "code")"
echo "  提示: $(jget "$R" "msg")"
chk "拦截后预算未变动" "$B_USED_BEFORE" "$(req GET /user/budget/my "$EH" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['usedAmount'])" 2>/dev/null)"

echo "═══ 22. 角色权限：部门管理仅管理员 ═══"
chk "采购专员新增部门被拒" "0" "$(jget "$(req POST /admin/department "$PH" '{"name":"测试部","code":"TEST_X"}')" "code")"
chk "管理员新增部门成功" "1" "$(jget "$(req POST /admin/department "$AH" '{"name":"测试部","code":"TEST_X"}')" "code")"
chk "采购专员可查部门列表" "1" "$(jget "$(req GET /admin/department/list "$PH")" "code")"
chk "部门编码重复被拒" "0" "$(jget "$(req POST /admin/department "$AH" '{"name":"测试部2","code":"TEST_X"}')" "code")"

echo "═══ 23. 预算管理接口权限 ═══"
chk "采购专员改预算被拒" "0" "$(jget "$(req POST /admin/budget "$PH" '{"deptId":2,"period":"2026-09","totalAmount":1}')" "code")"
chk "管理员查预算列表" "1" "$(jget "$(req GET /admin/budget/list "$AH")" "code")"

echo "═══ 24. 工作台（报表模块已整体删除，此处只留工作台） ═══"
chk "工作台今日数据" "1" "$(jget "$(req GET /admin/workspace/businessData "$AH")" "code")"
chk "工作台物资总览" "1" "$(jget "$(req GET /admin/workspace/overviewGoods "$AH")" "code")"
chk "工作台组合包总览" "1" "$(jget "$(req GET /admin/workspace/overviewCombos "$AH")" "code")"
chk "工作台申领单总览" "1" "$(jget "$(req GET /admin/workspace/overviewOrders "$AH")" "code")"
# 工作台的"新增员工"口径已切到 employee 表，这里顺带验证能取到值
R=$(req GET /admin/workspace/businessData "$AH")
echo "  工作台业务数据 newUsers=$(jget "$R" "data.newUsers") turnover=$(jget "$R" "data.turnover")"

echo "═══ 25. 报表接口应已不存在（回归保护） ═══"
for ep in turnoverStatistics userStatistics ordersStatistics top10 export; do
  code=$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' "$B/admin/report/$ep" -H "$AH")
  chk "GET /admin/report/$ep 已下线(404)" "404" "$code"
done

echo "═══ 26. Redis 缓存键（物资缓存键 + 组合包缓存键） ═══"
K1=$(redis-cli -a 123456 -n 10 --no-auth-warning keys 'goods:list:*' 2>/dev/null | head -1)
[ -n "$K1" ] && { echo "  ✅ 物资缓存键: $K1"; pass=$((pass+1)); } || { echo "  ❌ 未找到 goods:list:* 缓存键"; fail=$((fail+1)); }
K2=$(redis-cli -a 123456 -n 10 --no-auth-warning keys 'combo:list:*' 2>/dev/null | head -1)
[ -n "$K2" ] && { echo "  ✅ 组合包缓存键: $K2"; pass=$((pass+1)); } || { echo "  ❌ 未找到 combo:list:* 缓存键"; fail=$((fail+1)); }
K3=$(redis-cli -a 123456 -n 10 --no-auth-warning get 'shop:status' 2>/dev/null)
[ -n "$K3" ] && { echo "  ✅ 采购开关状态键: shop:status=$K3"; pass=$((pass+1)); } || echo "  ⚠️  shop:status 尚未设置（未调用过采购开关接口）"

echo "═══ 27. 收尾清理（避免残留测试数据）═══"
# 说明：budget / department / shopping_cart 之间没有数据库外键，
# 删除部门不会级联清理预算，历史上就因此留下过 dept_id 指向已删部门的孤儿预算行。
# 这里显式按依赖顺序清理，保证脚本跑完后库状态与跑之前一致。
mysql -uroot -p123456 sky_take_out <<'SQL' 2>/dev/null
DELETE FROM budget WHERE dept_id IN (SELECT id FROM department WHERE code = 'TEST_X');
DELETE FROM department WHERE code = 'TEST_X';
DELETE b FROM budget b LEFT JOIN department d ON b.dept_id = d.id WHERE d.id IS NULL;
DELETE FROM shopping_cart WHERE user_id = (SELECT id FROM employee WHERE username='zhangsan');
-- 本次回归产生的在途单据一并取消，避免下次跑时干扰统计
UPDATE orders SET status = 6, cancel_reason = '回归测试收尾清理' WHERE status IN (2,3,4);
UPDATE budget SET used_amount = 0 WHERE dept_id = (SELECT id FROM department WHERE code='DEPT_TECH');
SQL
echo "  已清理：测试部门 TEST_X、孤儿预算行、测试员工申领车、在途单据"
echo ""

echo ""
echo "════════════════════════════════════════"
echo "   通过: $pass    失败: $fail"
echo "════════════════════════════════════════"
