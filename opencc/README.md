# OpenCC AndroLua 模块调用文档

OpenCC（开放中文转换）的 AndroLua / Lua 5.3 绑定。用于简体 ↔ 繁体及台湾 / 香港用字转换。
基于 OpenCC 1.4.2 的 C API，模块名 `opencc`，导出入口 `luaopen_opencc`。

## 1. 部署

### 1.1 so 文件

> ** 动态链 STL**：本模块 NEEDED 含 `libc++_shared.so`。链接器只搜 APK 的`lib/<abi>/`（nativeLibraryDir），**不搜 app_lib**。因此 `libc++_shared.so` 必须与 `libopencc.so` 一起进 APK（重打包 / 方案 B）；app_lib 免重打包（方案 A）对opencc 不适用。工作区已把两 ABI 的 `libopencc.so` + `libc++_shared.so` 归置到`lib/<abi>/`，直接整目录塞进 APK 即可。

### 1.2 字典数据

配置与字典放在**同一目录**（模块用配置文件的父目录作为字典搜索路径，无需额外指定）。

| 内容 | 数量 | 体积 |
|---|---|---|
| `*.json` 配置 | 6 | 约 3 KB |
| `*.txt` 字典 | 17 | 约 1.4 MB |

字典用 **text 格式**（未走 `.ocd2` 生成管线）。代价：首次加载某配置要在运行时构建
darts trie，比较慢；好处：不用为字典生成器单独准备宿主环境。

## 2. API 参考

### 模块函数

| 函数 | 签名 | 说明 |
|---|---|---|
| `opencc.new(cfg_path)` | `-> userdata | nil, err` | 打开一个转换器。成功返回句柄；失败返回 `nil` + 错误信息 |
| `opencc.error()` | `-> string` | 返回最近一次 OpenCC 内部错误描述 |
| `opencc.version` | `string`（常量） | OpenCC 版本号，`"1.4.2"`（注意是字段不是函数，别写 `opencc.version()`） |

### 转换器方法（userdata 上的方法）

| 方法 | 签名 | 说明 |
|---|---|---|
| `conv:convert(text)` | `-> string | nil, err` | 执行转换。成功返回结果串；失败返回 `nil` + 错误信息 |
| `conv:close()` | `->`（无返回） | 显式释放转换器。之后该句柄不可再 `convert` |

转换器含 `__gc` 元方法，Lua 垃圾回收时会自动释放；但**不要依赖 GC 时机**，用完应
显式 `close()`，尤其要新建多个转换器时。

## 3. 配置文件

6 个配置，方向如下：

| 配置 | 方向 | 说明 |
|---|---|---|
| `s2t.json` | 简体 → 繁体 | 标准转换（OpenCC 语义） |
| `t2s.json` | 繁体 → 简体 | 标准转换 |
| `s2tw.json` | 简体 → 台湾正体 | 含台湾用语转换（如 软件→軟體） |
| `tw2s.json` | 台湾正体 → 简体 | 反向 |
| `s2hk.json` | 简体 → 香港繁体 | 含香港用语转换 |
| `hk2s.json` | 香港繁体 → 简体 | 反向 |

> 注：官方 `.ocd2` 配置里的 `STPhrases_GeneratedFromRegionalPhrases`、
> `TSCharactersExt` 由脚本生成、无 `.txt` 形式，本 text 版未包含；
> `TWVariantsRev.txt` / `HKVariantsRev.txt` 不存在（只有 `...RevPhrases.txt`）。

## 4. 示例

```lua
require "import"
import "android.content.Context"

-- 1) 部署 so + 数据（参考 install_opencc.lua，此处略）
--    so      -> app_lib 或 APK lib/<abi>/
--    数据    -> activity.getFilesDir() .. "/opencc/"
local dataDir = activity.getFilesDir().getAbsolutePath() .. "/opencc"

-- 2) 加载
local opencc = require "opencc"
print("opencc 版本:", opencc.version)   -- "1.4.2"

-- 3) 打开转换器（复用，别每次转换都 new）
local conv, err = opencc.new(dataDir .. "/s2t.json")
if not conv then
    return print("打开失败:", err, "/", opencc.error())
end

-- 4) 转换
print(conv.convert("汉字"))            -- 漢字
print(conv.convert("软件"))            -- 軟件
print(conv.convert("中国"))            -- 中國

-- 5) 释放
conv.close()
```

多方向并存（每个方向一个句柄，各自复用）：

```lua
local s2t = opencc.new(dataDir .. "/s2t.json")
local t2s = opencc.new(dataDir .. "/t2s.json")
local s2tw = opencc.new(dataDir .. "/s2tw.json")

print(s2t.convert("汉字"))             -- 漢字
print(t2s.convert("漢字"))             -- 汉字
print(s2tw.convert("软件"))            -- 軟體

s2t.close(); t2s.close(); s2tw.close()
```

## 5. 注意事项

1. **转换器必须复用**：text 字典首次加载要构建 darts trie，代价大。不要在循环里
   反复 `opencc.new`，应一次创建、多次 `convert`。
2. **version 是字段不是函数**：`opencc.version`（字符串常量），写 `opencc.version()`
   会报错（`attempt to call a string value`）。
3. **错误返回约定**：`new` 与 `convert` 失败都返回 `nil, err`（两返回值），err 已含
   OpenCC 内部描述；`opencc.error()` 是补充渠道，用于拿到最近一次底层错误。
4. **close 后不可再用**：`convert` 对已 close 的句柄会抛 Lua error
   （`converter already closed`）。
5. **UTF-8 字节透明**：输入输出均为 UTF-8 字符串，无转码；AndroLua 字符串天然 UTF-8，
   直接传中文即可。
6. **部署路径**：配置文件路径传给 `new` 时，字典必须与配置同目录，否则找不到字典。
