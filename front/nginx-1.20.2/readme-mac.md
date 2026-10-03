# macOS 下怎么跑这个前端运行环境（原 nginx.exe 用不了）

## 问题原因

资料包里的 `nginx-1.20.2` 是 **Windows 版**：核心文件是 `nginx.exe`（PE 可执行文件）。
你的机器是 macOS（Apple Silicon / arm64），macOS 不能执行 `.exe`，所以：

- `nginx.exe` 双击没反应 / 报错
- 网上让你装的 `WinSW`、`.bat` 启动脚本同样用不了

解决思路：**换成 macOS 原生的 nginx**，资料包里的前端静态文件（`html/sky`）和配置文件都继续用。

## 已经做好的事

1. 已安装 macOS 原生 nginx（Homebrew，`nginx -v` → 1.31.6，arm64）
2. 新增 macOS 版配置：`conf/nginx-mac.conf`（由原来的 `conf/nginx.conf` 改写）
3. 新增启动/停止脚本：`start-nginx-mac.sh`、`stop-nginx-mac.sh`
4. 已启动并通过验证：首页 200、静态资源 200、`/api/` 与 `/user/` 反向代理已生效

## 日常使用

```bash
cd /Users/eason/Desktop/caiyuntai/front/nginx-1.20.2

./start-nginx-mac.sh     # 启动
./stop-nginx-mac.sh      # 停止
```

启动后浏览器访问：**http://localhost:8082**（管理端）、**http://localhost:8083**（员工端）

不用脚本的话，等价的原生命令：

```bash
cd /Users/eason/Desktop/caiyuntai/front/nginx-1.20.2
nginx -p "$PWD" -c conf/nginx-mac.conf            # 启动
nginx -p "$PWD" -c conf/nginx-mac.conf -s stop    # 停止
nginx -p "$PWD" -c conf/nginx-mac.conf -s reload  # 改完配置后重载
```

## 和原配置相比改了 4 个地方

| 项目 | Windows 原版 | macOS 版 | 为什么 |
|---|---|---|---|
| 监听端口 | `listen 80` | `listen 8081` | macOS 上 80 是特权端口，必须 `sudo`；8081 普通用户即可。想用 80 就改回 `listen 80` 并用 `sudo` 启动 |
| 日志/pid | 默认相对路径 | 显式写在 `logs/` | 避免写 `/opt/homebrew`，不需要授权 |
| 临时目录 | 默认 | 显式指向包内 `temp/` | 直接复用 Windows 包自带的 `temp/` 目录 |
| 前端路由 | 无 | 加了 `try_files ... /index.html` | 前端是单页应用，刷新子路由不再 404 |

## 注意事项

- **`nginx.exe` 已经用不上了**，可以留着不管，也可以删掉；`conf/nginx.conf` 保留做参考，实际生效的是 `conf/nginx-mac.conf`。
- **后端必须先起**：`/api/`、`/user/`、`/ws/` 都反向代理到 `127.0.0.1:8080`。后端没启动时访问接口会返回 **502**，这是正常的（说明代理通了，只是后端不在）。
- 前端接口 `baseURL` 是 `/api`（相对路径），所以端口换成 8081 不影响前端调用，直接访问 `http://localhost:8081` 即可。
- 启动时可能看到一行提示：`could not open error log file: /opt/homebrew/var/log/nginx/error.log`。这是 nginx 读自己配置前的默认路径告警，实际日志写在包内 `logs/error.log`，**不影响使用**。
- 目录路径不要带中文（原 readme 的提醒对 macOS 同样适用）。
- 排查问题看 `logs/error.log`。
