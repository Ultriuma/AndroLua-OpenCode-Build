local _M = {}
local PREFIX = "com.androlua.Welcome_"
local TAGS = { "Icon", "Neko", "Slot1", "Slot2", "Slot3", "Slot4", "Slot5", "Slot6", "Slot7", "Slot8" }
local ENABLED = 1 -- PackageManager.COMPONENT_ENABLED_STATE_ENABLED
local DISABLED = 2 -- PackageManager.COMPONENT_ENABLED_STATE_DISABLED
local DONT_KILL = 1 -- PackageManager.DONT_KILL_APP

local function component(tag)
  return luajava.newInstance("android.content.ComponentName", activity.getPackageName(), PREFIX .. tag)
end

-- 启用目标别名，其余全部禁用（同一时刻只允许一个带 LAUNCHER 的组件启用）
_M.setIcon = function(tag)
  if tag ~= _M.getCurrentIcon() then
    local tag = tag or "Icon"
    local pm = activity.getPackageManager()
    activity.showToast("配置已推送，接下来的桌面重绘将会杀死我，为避免产生闪烁现回到桌面")
    activity.moveTaskToBack(true)
    for _, t in ipairs(TAGS) do
      pm.setComponentEnabledSetting(component(t), (t == tag) and ENABLED or DISABLED, DONT_KILL)
    end
  end
end

-- 获取全部的图标
_M.getIconTags = function()
  return TAGS
end

-- 当前生效的图标（0=DEFAULT 与 1=ENABLED 均视为启用，Welcome_Icon 在 manifest 里默认 enabled）
_M.getCurrentIcon = function()
  local pm = activity.getPackageManager()
  for _, t in ipairs(TAGS) do
    if pm.getComponentEnabledSetting(component(t)) ~= DISABLED then
      return t
    end
  end
  return "Icon"
end

_M.initTaskIcon = function()
  pcall(function()
    local resId = activity.getResources().getIdentifier(utf8.lower(_M.getCurrentIcon()), "drawable", activity.getPackageName())
    local bm = luajava.bindClass("android.graphics.BitmapFactory").decodeResource(activity.getResources(), resId)
    activity.setTaskDescription(luajava.newInstance("android.app.ActivityManager$TaskDescription", "", bm))
  end)
end

return _M