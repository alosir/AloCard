package com.alosir.card.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alosir.card.ui.theme.AloCardTheme

/** 底部导航栏目的地：卡片 / 额度 / 账单 / 设置 */
enum class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    CARDS(Routes.HOME, "首页", Icons.Default.Home),
    QUOTA(Routes.QUOTA, "额度", Icons.Default.Star),
    BILLS(Routes.GROUPS, "账单", Icons.Default.DateRange),
    SETTINGS(Routes.SETTINGS, "设置", Icons.Default.Settings),
}

/**
 * 底部导航栏 · v2。
 * 修复旧版图标贴上边缘：图标区上间距 10dp、图标 23dp、图标与文字间距 5dp、
 * 选中项金色。自绘而非 Material NavigationBar，以精确控制垂直节奏。
 */
@Composable
fun AloCardBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val colors = AloCardTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.paperRaised)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp)
            .padding(top = 10.dp, bottom = 12.dp),
    ) {
        TopDestination.entries.forEach { dest ->
            val selected = when (dest.route) {
                Routes.HOME -> currentRoute in listOf(
                    Routes.HOME, Routes.CARD_DETAIL, Routes.ADD_CARD,
                )
                Routes.GROUPS -> currentRoute in listOf(
                    Routes.GROUPS, Routes.GROUP_DETAIL, Routes.GROUP_FORM, Routes.BILL_HISTORY,
                )
                else -> currentRoute == dest.route
            }
            val tint = if (selected) colors.goldText else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigate(dest.route) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    dest.icon,
                    contentDescription = dest.label,
                    tint = tint,
                    modifier = Modifier.size(23.dp),
                )
                Text(
                    dest.label,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = tint,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
    }
}
