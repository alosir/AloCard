package com.alocard.app.ui.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** 底部导航栏目的地（DESIGN.md 6.1：卡片 / 额度 / 账单 / 设置） */
enum class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    CARDS(Routes.HOME, "卡片", Icons.Default.Home),
    QUOTA(Routes.QUOTA, "额度", Icons.Default.Star),
    BILLS(Routes.GROUPS, "账单", Icons.Default.DateRange),
    SETTINGS(Routes.SETTINGS, "设置", Icons.Default.Settings),
}

/**
 * Material navigation bar（DESIGN.md 6.1）：
 * 顶边 1dp rule + 抬起面底色，选中项图标与文字转墨色。
 */
@Composable
fun AloCardBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(
        modifier = Modifier.height(64.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        TopDestination.entries.forEach { dest ->
            val selected = when (dest.route) {
                // 卡片详情/新增等也属于「卡片」标签的栈
                Routes.HOME -> currentRoute in listOf(
                    Routes.HOME, Routes.CARD_DETAIL, Routes.ADD_CARD,
                )
                Routes.GROUPS -> currentRoute in listOf(
                    Routes.GROUPS, Routes.GROUP_DETAIL, Routes.GROUP_FORM, Routes.BILL_HISTORY,
                )
                else -> currentRoute == dest.route
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(dest.route) },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        }
    }
}
