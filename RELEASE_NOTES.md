## 闪卡背诵 v1.5.1

- 预测性返回：Activity 级显式开启 `enableOnBackInvokedCallback`。
- 通用设置新增系统检测：若 Android 系统开关 `enable_back_animation=0`，会提示开启「预测性返回动画」，并提供开发者选项入口。
- 说明：该开关属于 Android 系统设置，应用无权修改；只有系统开启后，返回手势才会显示上一页预览。
