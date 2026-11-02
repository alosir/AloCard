package com.alocard.app.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.alocard.app.ui.common.dotGrid
import com.alocard.app.ui.components.CreditOverview
import com.alocard.app.ui.components.EmptyState
import com.alocard.app.ui.components.LedgerGroupDivider
import com.alocard.app.ui.components.LedgerRow
import com.alocard.app.ui.components.SortSegment
import com.alocard.app.ui.components.TodoBanner
import com.alocard.app.ui.theme.AloCardTheme
import com.alocard.app.ui.theme.tabColorFor

/**
 * 首页 —— 卡片列表（P1）。
 *
 * 结构（DESIGN.md 6.1，自上而下）：
 * 顶栏（品牌 + 今日日期）→ 额度总览条 → 待办聚合条 → 排序分段控件 → 账簿行列表 → FAB。
 * 默认按免息期降序——用户打开第一眼就该得到「今天该刷哪张」的答案。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGroupClick: (Long) -> Unit,
    onAddCard: () -> Unit,
    onTodoClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 通知权限状态：每次回到前台重查（PRD F7.4 规则 4）
    val context = LocalContext.current
    var notifGranted by remember { mutableStateOf(isNotificationGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            notifGranted = isNotificationGranted(context)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AloCard",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    Text(
                        text = uiState.todayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCard,
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加卡片") },
            )
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .dotGrid(AloCardTheme.colors.dot)
        ) {
            if (uiState.groups.isEmpty()) {
                EmptyState(
                    title = "还没有卡片",
                    description = "加一张信用卡，AloCard 会算出它距离下次出账还有几天、免息期有多少天。",
                    actionLabel = "添加第一张卡",
                    onAction = onAddCard,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (!notifGranted) {
                        item {
                            NotificationPermissionBanner(
                                onClick = {
                                    context.startActivity(notificationSettingsIntent(context))
                                },
                            )
                        }
                    }
                    item {
                        CreditOverview(
                            usedCents = uiState.totals.usedCents,
                            totalCents = uiState.totals.totalLimitCents,
                            remainCents = uiState.totals.remainCents,
                        )
                    }
                    if (uiState.todoSummary.isNotEmpty()) {
                        item {
                            TodoBanner(
                                summary = uiState.todoSummary,
                                onClick = {
                                    uiState.todoTargetGroupId?.let(onTodoClick)
                                },
                            )
                        }
                    }
                    item {
                        SortSegment(
                            selected = uiState.sortMode,
                            onSelect = viewModel::onSortModeChange,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    items(uiState.groups, key = { it.groupId }) { group ->
                        group.cards.forEachIndexed { index, card ->
                            LedgerRow(
                                cardName = card.displayName,
                                cardTail = card.tail,
                                sharedCount = group.cards.size,
                                gracePeriodDays = group.gracePeriodDays,
                                dueText = group.dueText,
                                dueSubText = group.dueSubText,
                                dueState = group.dueState,
                                billState = group.billState,
                                billDate = group.billDate,
                                dueDate = group.dueDate,
                                today = uiState.today,
                                tabColor = tabColorFor(hue = group.hue),
                                tabGlyph = group.groupName.firstOrNull()?.toString() ?: "卡",
                                onClick = { onGroupClick(group.groupId) },
                            )
                            if (index < group.cards.lastIndex) {
                                LedgerGroupDivider()
                            }
                        }
                        // 组与组之间 4dp 间隔（DESIGN.md 6.2）
                        Spacer(Modifier.height(4.dp))
                        LedgerGroupDivider()
                    }

                    // FAB 占位，避免最后一行被 FAB 遮住
                    item { Spacer(Modifier.height(84.dp)) }
                }
            }
        }
    }
}

/**
 * 通知权限缺失提示条（PRD F7.4）。
 * 通知是本产品唯一的主动触达通道，权限缺失若静默失效用户不会发现少了提醒——
 * 因此醒目提示、不设「不再提示」。
 */
@Composable
private fun NotificationPermissionBanner(onClick: () -> Unit) {
    val colors = AloCardTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.dueBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Notifications,
            contentDescription = null,
            tint = colors.due,
            modifier = Modifier.width(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "未开启通知权限，将无法收到还款提醒",
            style = MaterialTheme.typography.labelMedium,
            color = colors.due,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "去设置",
            style = MaterialTheme.typography.labelMedium,
            color = colors.due,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        )
    }
}

private fun isNotificationGranted(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

private fun notificationSettingsIntent(context: Context): Intent =
    Intent(
        Settings.ACTION_APP_NOTIFICATION_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
