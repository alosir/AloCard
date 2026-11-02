package com.alocard.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * AloCard 应用入口。
 *
 * 用 @HiltAndroidApp 触发 Hilt 代码生成。
 * 不在这里做任何初始化——本产品无账号、无网络、无第三方 SDK，
 * 冷启动只需要 Room 与 Hilt，两者都是惰性的。
 */
@HiltAndroidApp
class AloCardApplication : Application()
