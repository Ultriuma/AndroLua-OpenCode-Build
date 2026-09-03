把腾讯 [MMKV](https://github.com/Tencent/MMKV) (v2.4.2) 的 C++ Core 与一层 Lua 5.3 绑定
(`lua_mmkv.cpp`) 编译成**单个** libmmkv.so，作为 AndroLua 的原生库直接 `require ("mmkv")` 使用。

> MMKV 官方 nativelib 只导出 C++ mangled 符号，没有 `luaopen_*` 导出
> 必须把 Core + wrapper 用同一 NDK、同一 STL （或者使用C++共享库）编进一个 so 让依赖链回到一层（`libluajava.so` + 系统库），才能走 AndroLua 的 package.cpath + dlopen 加载

| 依赖 | 版本 / 路径 |
|---|---|
| NDK | 26.1.10909125 |
| libluajava.so | arm64 / armv7 |
| MMKV 源码 | mmkv-src（MMKV 2.4.2 Core，约 30 个 **.cpp** + 自带 AES/crc32） |
| Lua 版本 | LUA_CPATH_5_3 |
| C++共享库 | libc++_shared |
| STL | **c++_static** 用 **-static-libstdc++** 静态链（不依赖外部 **libc++_shared.so**） |
| C++ 标准 | **-std=c++20**（MMKV 2.4.2 把 **MMKVKey_t** 定义为 **std::string_view** ， **unordered_map::find** 接受 string_view 属 C++20 异构查找，c++17 编不过） |

### 使用方案简介
#### 方案 A：塞进 APK assets 免打包
> 不碰 APK、不重签名。把 so 推到 **project** 对应文件夹，在 app 内用 **io** 操作复制文件到应用程序的 **app_lib**
```lua
local LibName = "mmkv"
local rootPath = activity.getLuaDir()
local dataDir = activity.getDataDir()
luajava.bindClass("com.androlua.LuaUtil").copyFile(rootPath..'/library/lib'..LibName..'.so', string.format(dataDir.toString().."/app_lib/lib%s.so", LibName))
```

### 方案 B：塞进 APK 重打包
> 把 **libmmkv.so** 放进 APK 的 **lib/arm64-v8a/**（或者 **armeabi** 和 **armeabi-v7a/**）
> 再 zipalign → apksigner（v1+v2+v3）重签。优点是无须运行时拷贝，缺点是每次更新都要重打（或者直接修改IDE实现避免重打

---

### 函数调用以及其他信息
#### 1. 模块函数（**mmkv.**）

| 函数 | 签名 | 说明 |
|---|---|---|
| **init** | **mmkv.init(rootDir [, logLevel])** | 初始化 MMKV 根目录。**必须在 **default**/**withID** 之前调用**。返回 **true** |
| **default** | **mmkv.default([mode])** | 取默认（无名）实例，等价 **defaultMMKV**。**mode** 默认 **SINGLE_PROCESS** |
| **defaultMMKV** | **mmkv.defaultMMKV([mode])** | **default** 的高亮安全别名（AndroLua 编辑器把 **default** 当关键字上色，用这个避免） |
| **withID** | **mmkv.withID(id [, mode])** | 按 ID 取具名实例（文件在 **<rootDir>/mmkv.<id>**） |
| **version** | **mmkv.version()** | 返回 MMKV 上游版本字符串（如 **"2.4.2"**）；与字段 **MMKV_VERSION** 同值 |

<br>

#### 2. 模块常量

| 常量 | 值 | 说明 |
|---|---|---|
| **SINGLE_PROCESS** | 1 | 单进程模式 |
| **MULTI_PROCESS** | 2 | 多进程模式（可与 **READ_ONLY** 位或） |
| **LOG_NONE** | — | 关闭日志 |
| **LOG_ERROR** | — | 仅错误 |
| **LOG_INFO** | — | 默认级别 |
| **LOG_WARNING** | 2 | 警告以上 |
| **LOG_DEBUG** | 0 | 调试（**release/product 构建被 MMKV 编译掉，传了无效**） |
| **READ_ONLY** | 32 | 只读打开实例（跨平台；适合内置预置文件防误改） |
| **ASHMEM** | 8 | 存 ashmem 纯内存、不落盘（**Android 专用**） |
| **BACKUP** | 16 | 备份模式（**Android 专用**） |
| **MMKV_VERSION** | **"2.4.2"** | 上游 MMKV 版本（编译期来自 **MMKVPredef.h**） |
| **BUILD_AUTHOR** | **"ULTRIUMA"** | 编译者/编译期宏注入 |
| **BUILD_TIME** | **"2026-08-30T07:24:59Z"** | 编译时刻（UTC，编译期 **date -u** 注入） |

<br>

#### 3. 实例方法（**kv:**）

| 方法 | 签名 | 返回 |
|---|---|---|
| **set** | **kv:set(key, value)** | **boolean**（成功）。**value** 按 Lua 类型自动分派：boolean→bool、整数→int64、浮点→double、string→string |
| **getString** | **kv:getString(key)** | **string** / **nil**（类型不匹配返回 nil） |
| **getInt** | **kv:getInt(key [, def=0])** | **integer** |
| **getBool** | **kv:getBool(key [, def=false])** | **boolean** |
| **getDouble** | **kv:getDouble(key [, def=0.0])** | **number** |
| **contains** | **kv:contains(key)** | **boolean** |
| **remove** | **kv:remove(key)** | — |
| **count** | **kv:count()** | **integer**（总 key 数） |
| **allKeys** | **kv:allKeys()** | **table**（key 数组） |
| **clear** | **kv:clear()** | —（清空全部） |
| **sync** | **kv:sync()** | —（强制落盘；mmap 本身异步同步，sync 立即刷） |
| **close** | **kv:close()** | —（销毁实例，见 §4 坑点） |
| **totalSize** | **kv:totalSize()** | **integer**（已用字节数） |

<br>

```lua
local mmkv = require "mmkv"
local cjson = require "cjson"

-- 初始化根目录（用 app 私有目录下的 mmkv 子目录
mmkv.init(activity.getFilesDir().getAbsolutePath() .. "/mmkv", mmkv.LOG_NONE)

-- 取默认实例（高亮安全写法）
local kv = mmkv.defaultMMKV()

-- 如果你介意则使用我提供的安全写法↑
-- 取默认实例在部分编辑器会出现高亮显示问题
-- local kv = mmkv.default()

-- 写（自动按类型分派）
kv:set("name", "androlua")
kv:set("age", 18)
kv:set("score", 95.5)
kv:set("vip", true)

-- 读
print(kv:getString("name"))    --> androlua
print(kv:getInt("age"))        --> 18
print(kv:getDouble("score"))   --> 95.5
print(kv:getBool("vip"))       --> true

-- 编译元数据
print(mmkv.BUILD_AUTHOR)       --> ULTRIUMA
print(mmkv.BUILD_TIME)         --> 2026-08-30T07:24:59Z
print(mmkv.MMKV_VERSION)       --> 2.4.2

-- LuaTable：MMKV 不原生支持，序列化后存字符串
kv:set("cfg", cjson.encode({ a = 1, b = { 2, 3 }, name = "x" }))
local t = cjson.decode(kv:getString("cfg"))

-- 多实例
local userKV = mmkv.withID("usr", mmkv.MULTI_PROCESS)
userKV:set("token", "abc")

-- 元信息
print(kv:count(), kv:totalSize())
print(table.concat(kv:allKeys(), ", "))
```

#### 4. 坑点与注意事项

1. **default 高亮失效**：AndroLua 编辑器把 **default** 误当关键字上色（标准 Lua 5.3 保留字里并无 **default**，
   运行时合法）。改用 **defaultMMKV** 即可正常高亮。两者功能完全相同。
2. **close() 后对象是悬垂指针**：**close()** 内部 **delete this** 销毁 C++ 实例，但 Lua 的 userdata 仍指着
   已释放内存。**close 之后绝不能再调用该 `kv` 的任何方法**，否则 use-after-free，会崩或行为错乱。
   需要再用时重新 **default()** / **withID()** 会重新加载（有一次 mmap + 读文件开销）。
3. **没有 __gc 终结器**：Lua 变量被 GC 回收 **不会** 自动释放 MMKV 实例——它还在 MMKV 全局实例表里，
   只有显式 **close()** 或进程退出才释放。所以 **default()** 单例当 app 级单例用、不关完全 OK；
   而 **withID** 创建的临时实例若大量动态创建从不关，会累积 mmap 虚拟地址 + fd，有耗尽风险，用完应 **close()**。
4. **LOG_DEBUG 在 release 包无效**：MMKV 在 release/product 构建编译掉 debug 日志，传 **LOG_DEBUG** 不报错但无输出。
5. **二进制存储未实现**：当前 **kv** 方法只支持 string/number/bool，**没有 **setData** / **getData**（字节 blob）**。
   要存任意二进制或 LuaTable：
   - 普通数据表 → **cjson.encode** 存字符串（AndroLua 自带 libcjson.so，最通用）；
   - 二进制 blob → 待加 **setData** / **getData**（MMKV C++ 有 **set(MMBuffer)** / **getBytes**，wrapper 尚未暴露）。
   - cjson 限制：函数 / userdata / 元表 / 循环引用无法序列化，纯数据表没问题。
6. **static-libstdc++ 是唯一正确选择**：STL 静态链进 so，不生成对 **libc++_shared.so** 的依赖；
   AndroLua APK 本就不打包它。前提：**libmmkv.so** 是 app 内唯一 C++ so，不与别的 C++ so 跨边界传 STL 对象。

---

#### 6. 待办（TODO）
- [x] ELF LOAD 段对齐（v8a 16KB / v7a 4KB）
> Android 设备的物理内存页过去一直是 4KB（4096 字节）。从 Android 15（API 35）开始，Google 推动设备使用更大的 16KB（16384 字节）内存页。原生库（.so）是加载进内存的，它的 ELF 结构里 LOAD 段的对齐方式必须匹配页大小，否则在 16KB 页大小的设备上会加载失败。为了让应用兼容 16KB 页大小设备，Google 要求用到原生库的应用把库重新用新的工具链对齐到 16KB。
- [ ] 暴露二进制接口 **setData(key, str)** / **getData(key)**（用 **luaL_checklstring** / **lua_pushlstring** 包 **MMBuffer**），
      以支持任意字节 blob 与 LuaTable 字节流存储。
- [ ] （可选）加 **__gc** 终结器，在 Lua GC 回收 userdata 时安全 **close()**（需处理多个 userdata 指向同一 **MMKV*** 只释放一次）。
- [x] （可选）编译加上 **libc++_shared.so**，解决§4.6槽点（代价呢？体积会变大不少，C++共享库可不小，
      并且动态版必须连同 libc++_shared.so 一起部署

<br>

---

#### 7. 杂项

```lua
--[[
 * 基于 MMKV 2.4.2 Lua 绑定的 MMKV Lua 链式调用封装
 * 
 * 在原始 mmkv 模块基础上增加:
 *   - KV 类: 支持 set/get 等方法的链式调用
 *   - getTable/setTable: 通过 cjson 实现 table 类型的存取
 *   - open(): 统一入口，自动校验 mode 并返回 KV 实例
 *
 * file: /lua/LuaMMKV.lua
 *
 * method:
 *   local mmkv = require("mmkv_wrap")
 *   local kv = mmkv.open("mydata")
 *   kv:set("name", "团子"):set("age", 3):setTable("config", { theme = "dark" })
 *   print(kv:getString("name"))  --> 团子
 *   print(kv:getTable("config").theme)  --> dark
 *
 * build: 
 * ULTRIUMA / MMKV_VERSION 2.4.2
--]]

local KV = {}
local cjson = require("json")

-- 加载原始 mmkv 模块，失败则直接报错退出
local success, _M = pcall(function()
  return require("mmkv")
end)

if not success or type(_M) ~= "table" then
  return error(_M)
end

KV.__index = KV

-- 构造 KV 实例，包装一个原始 mmkv kv 对象
-- @param kv 原始 mmkv 实例，为 nil 时取 defaultMMKV
-- @return KV 实例
function KV:new(kv)
  local o = {}
  setmetatable(o, self)
  o._kv = kv or _M.defaultMMKV()
  return o
end

-- 写入键值对，值按 Lua 类型自动分派(bool/int/double/string)
-- @param k 键
-- @param v 值
-- @return self (链式调用)
function KV:set(k, v)
  self._kv:set(k, v)
  return self
end

-- 读取字符串
-- @param k 键
-- @return string 或 nil(类型不匹配时)
function KV:getString(k)
  return self._kv:getString(k)
end

-- 读取整数
-- @param k 键
-- @param def 默认值(缺省为 0)
-- @return integer
function KV:getInt(k, def)
  return self._kv:getInt(k, def)
end

-- 读取布尔值
-- @param k 键
-- @param def 默认值(缺省为 false)
-- @return boolean
function KV:getBool(k, def)
  return self._kv:getBool(k, def)
end

-- 读取浮点数
-- @param k 键
-- @param def 默认值(缺省为 0.0)
-- @return number
function KV:getDouble(k, def)
  return self._kv:getDouble(k, def)
end

-- 读取 table，内部通过 cjson 反序列化
-- @param k 键
-- @param def 默认值(缺省为 {})
-- @return table，解析失败或不存在时返回 def 或 {}
function KV:getTable(k, def)
  if k and self._kv:contains(k) then
    local ok, val = pcall(function()
      return cjson.decode(self._kv:getString(k) or "")
    end)
    if ok and type(val) == "table" then
      return val
    end
  end
  return def or {}
end

-- 写入 table，内部通过 cjson 序列化为 JSON 字符串
-- 非 table 值写入空对象 "{}"
-- @param k 键
-- @param t table
-- @return self (链式调用)
function KV:setTable(k, t)
  self._kv:set(k, type(t) == "table" and cjson.encode(t) or "{}")
  return self
end

-- 批量写入数据，遍历传入的 table 逐个 set
-- table 类型的值自动走 cjson 序列化(setTable 逻辑)
-- @param data 键值对 table，如 { name = "团子", age = 3, config = { theme = "dark" } }
-- @return self (链式调用)
function KV:apply(data)
  if type(data) ~= "table" then
    error("apply expects a table, got " .. type(data))
  end
  for k, v in pairs(data) do
    if type(v) == "table" then
      self._kv:set(k, cjson.encode(v))
     else
      self._kv:set(k, v)
    end
  end
  return self
end


-- 判断键是否存在
-- @param k 键
-- @return boolean
function KV:contains(k)
  return self._kv:contains(k)
end

-- 移除指定键
-- @param k 键
-- @return self (链式调用)
function KV:remove(k)
  self._kv:remove(k)
  return self
end

-- 获取键的总数量
-- @return integer
function KV:count()
  return self._kv:count()
end

-- 获取所有键
-- @return table (键数组)
function KV:allKeys()
  return self._kv:allKeys()
end

-- 清空全部键值对
-- @return self (链式调用)
function KV:clear()
  self._kv:clear()
  return self
end

-- 强制落盘(mmap 异步同步的即时刷盘)
-- @return self (链式调用)
function KV:sync()
  self._kv:sync()
  return self
end

-- 获取已用字节数
-- @return integer
function KV:totalSize()
  return self._kv:totalSize()
end

-- 销毁实例，释放底层资源
-- @return self (链式调用)
function KV:close()
  self._kv:close()
  return self
end

--[[
 * 统一入口，根据参数类型自动分派实例获取方式，支持:
 * open()            -> 默认实例, 默认 mode
 * open(mode)        -> 默认实例, 指定 mode
 * open(id)          -> 命名实例, 默认 mode
 * open(mode, id)    -> 命名实例, 指定 mode
 * open(id, mode)    -> 命名实例, 指定 mode
--]]
-- @param arg1 mode(number) 或 id(string)
-- @param arg2 id(string) 或 mode(number)
-- @return KV 实例
_M.open = function(arg1, arg2)
  local VALID_MASK = _M.SINGLE_PROCESS | _M.MULTI_PROCESS | _M.READ_ONLY | _M.ASHMEM | _M.BACKUP

  local function checkMode(m)
    if m ~= nil and (m & ~VALID_MASK) ~= 0 then
      error("invalid mode: " .. tostring(m))
    end
    return m
  end

  if type(arg1) == "number" then
    if type(arg2) == "string" then
      return KV:new(_M.withID(arg2, checkMode(arg1)))
    end
    return KV:new(_M.defaultMMKV(checkMode(arg1)))
   elseif type(arg1) == "string" then
    if type(arg2) == "number" then
      return KV:new(_M.withID(arg1, checkMode(arg2)))
    end
    return KV:new(_M.withID(arg1))
   else
    return KV:new()
  end
end

return _M
```

```lua
--[[
 * MMKV Lua 链式调用封装 使用文档
 * 基于 MMKV 2.4.2 | BUILD: ULTRIUMA
--]]

-- ==================== 初始化 ====================
local mmkv = require("mmkv_wrap")

-- open 之前必须先 init，指定存储根目录
mmkv.init("/sdcard/mmkv", mmkv.LOG_INFO)

-- ==================== 打开实例 ====================
local kv = mmkv.open()                  -- 默认实例
local kv = mmkv.open("mydata")          -- 命名实例
local kv = mmkv.open(mmkv.MULTI_PROCESS)               -- 指定 mode
local kv = mmkv.open("mydata", mmkv.MULTI_PROCESS)      -- 命名 + mode
local kv = mmkv.open(mmkv.MULTI_PROCESS, "mydata")      -- 顺序无所谓

-- ==================== 基本读写 ====================
kv:set("name", "团子")
kv:set("age", 3)
kv:set("weight", 40.5)
kv:set("isCat", true)

kv:getString("name")      --> "团子"
kv:getInt("age")           --> 3
kv:getDouble("weight")     --> 40.5
kv:getBool("isCat")        --> true

-- 带默认值
kv:getInt("notExist", -1)       --> -1
kv:getBool("notExist", false)   --> false

-- ==================== Table 读写 ====================
kv:setTable("config", { theme = "dark", fontSize = 14 })

local cfg = kv:getTable("config")
print(cfg.theme)      --> "dark"
print(cfg.fontSize)   --> 14

-- 不存在的 key 返回默认值
kv:getTable("notExist", {})     --> {}

-- ==================== 批量写入 ====================
kv:apply({
  name = "团子",
  age = 3,
  config = { theme = "dark" },    -- table 自动走 setTable
}):sync()

-- ==================== 链式调用 ====================
kv:set("a", 1)
  :set("b", 2)
  :setTable("c", { x = 1 })
  :remove("a")
  :sync()

-- ==================== 查询/管理 ====================
kv:contains("name")    --> true
kv:count()             --> 键总数
kv:allKeys()           --> { "name", "age", ... }
kv:totalSize()         --> 已用字节数

-- ==================== 清空/销毁 ====================
kv:clear()             -- 清空所有键值对
kv:sync()              -- 强制落盘
kv:close()             -- 销毁实例，释放资源

-- ==================== 遍历 ====================
for _, key in ipairs(kv:allKeys()) do
  print(key, kv:getString(key))
end

-- ==================== 多进程模式 ====================
local kv2 = mmkv.open("shared", mmkv.MULTI_PROCESS | mmkv.READ_ONLY)

-- ==================== mode 常量 ====================
mmkv.SINGLE_PROCESS    -- 1  单进程(默认)
mmkv.MULTI_PROCESS     -- 2  多进程
mmkv.READ_ONLY         -- 32 只读
mmkv.ASHMEM            -- 8  纯内存不落盘(Android)
mmkv.BACKUP            -- 16 备份模式(Android)
-- mode 可位或组合，如 MULTI_PROCESS | READ_ONLY

-- ==================== 日志级别 ====================
mmkv.LOG_NONE          -- 关闭
mmkv.LOG_ERROR         -- 仅错误
mmkv.LOG_INFO          -- 默认
mmkv.LOG_WARNING       -- 警告以上
mmkv.LOG_DEBUG         -- 调试(release 构建无效)

-- ==================== 版本信息 ====================
mmkv.version()         --> "2.4.2"
mmkv.MMKV_VERSION      --> "2.4.2"
mmkv.BUILD_AUTHOR      --> "ULTRIUMA"
mmkv.BUILD_TIME        --> "2026-08-30T07:24:59Z"
```