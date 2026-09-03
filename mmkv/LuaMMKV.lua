--[[
 * 基于 MMKV 2.4.2 Lua 绑定的 MMKV Lua 链式调用封装
 *
 * 在原始 mmkv 模块基础上增加:
 *   - KV 类: 支持 set/get 等方法的链式调用
 *   - getTable/setTable: 通过 cjson 实现 table 类型的存取
 *   - open(): 统一入口，自动校验 mode 并返回 KV 实例
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