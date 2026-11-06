package com.alosir.card.ui.navigation

/**
 * 路由表（PRD 第 7 章页面清单 + 技术规范 §2.5 的 P14）。
 * 扁平结构——产品只有十余个界面，不值得引入嵌套导航图。
 */
object Routes {
    // 底部导航 4 项
    const val HOME = "home"           // P1 首页
    const val QUOTA = "quota"         // P7 额度明细
    const val GROUPS = "groups"       // P14 账单组管理
    const val SETTINGS = "settings"   // P11 设置

    // 卡片/账单组流程
    const val CARD_DETAIL = "card/{groupId}"     // P2 卡片详情（按账单组）
    const val ADD_CARD = "card/add"              // P3 新增卡片
    const val GROUP_DETAIL = "group/{groupId}"   // P5 账单组详情
    const val GROUP_FORM = "group/form?groupId={groupId}" // P4 新建/编辑账单组
    const val BILL_HISTORY = "history/{groupId}" // P6 账单历史

    // 金额操作已改为详情页内浮层（SlideUpSheet），不再使用独立路由

    // 实时卡面识别（CameraX + ML Kit）
    const val SCAN = "scan"

    // 安全
    const val PIN_SETUP = "pin/setup"            // P10 设置安全密码
    const val PIN_CHANGE = "pin/change"          // P10 修改安全密码
    const val PIN_VERIFY = "pin/verify"          // 验证安全密码（查看卡号闸门）

    fun cardDetail(groupId: Long) = "card/$groupId"
    fun groupDetail(groupId: Long) = "group/$groupId"
    fun groupForm(groupId: Long? = null) =
        if (groupId == null) "group/form?groupId=-1" else "group/form?groupId=$groupId"
    fun billHistory(groupId: Long) = "history/$groupId"
}
