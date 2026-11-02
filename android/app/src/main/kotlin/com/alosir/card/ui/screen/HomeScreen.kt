package com.alosir.card.ui.screen

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.alosir.card.domain.util.Money
import com.alosir.card.ui.common.goldBrush
import com.alosir.card.ui.components.BankLedgerRow
import com.alosir.card.ui.components.CreditCardFace
import com.alosir.card.ui.components.CreditOverview
import com.alosir.card.ui.components.EmptyState
import com.alosir.card.ui.components.NoticeBanner
import com.alosir.card.ui.components.SortSegment
import com.alosir.card.ui.components.TodoBanner
import com.alosir.card.ui.components.schemeLabelFor
import com.alosir.card.ui.theme.AloCardTheme
import com.alosir.card.ui.theme.tabColorFor

/**
 * 首页 · v2「鎏金卡夹」。
 *
 * 自上而下：顶栏（品牌 + 今日日期）→ 通知引导条（可关闭）→ 卡面轮播 →
 * 本期应还 → 额度总览（总额度/已用/剩余可用估）→ 排序 → 银行账簿行。
 * 账簿行 = 银行账单组（同银行多卡合并出账），不再单列逐卡账单。
 */
@Composable
fun HomeScreen(
    onGroupClick: (Long) -> Unit,
    onAddCard: () -> Unit,
    onTodoClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AloCardTheme.colors

    // 通知权限状态：每次回到前台重查（PRD F7.4 规则 4）
    val context = LocalContext.current
    var notifGranted by remember { mutableStateOf(isNotificationGranted(context)) }
    var noticeDismissed by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            notifGranted = isNotificationGranted(context)
        }
    }

    // 卡轮播数据：组 × 卡 展开
    val carousel = remember(uiState.groups) {
        uiState.groups.flatMap { g -> g.cards.map { c -> g to c } }
    }
    val pagerState = rememberPagerState(pageCount = { carousel.size })
    val currentGroup = carousel.getOrNull(pagerState.currentPage)?.first

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "Alo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Card",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.goldText,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    uiState.todayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        floatingActionButton = {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(goldBrush())
                    .clickable { onAddCard() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加卡片", tint = colors.onGold)
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.groups.isEmpty()) {
                EmptyState(
                    title = "还没有卡片",
                    description = "加一张信用卡，AloCard 会算出它距离下次出账还有几天、免息期有多少天。",
                    actionLabel = "添加第一张卡",
                    onAction = onAddCard,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (!notifGranted && !noticeDismissed) {
                        item {
                            NoticeBanner(
                                onEnable = { openNotificationSettings(context) },
                                onDismiss = { noticeDismissed = true },
                            )
                        }
                    }
                    // 卡面轮播
                    item {
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                            pageSpacing = 14.dp,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) { page ->
                            val (g, c) = carousel[page]
                            CreditCardFace(
                                bankName = g.bankName,
                                orgText = schemeLabelFor(c.scheme),
                                tail = c.tail,
                                graceDays = g.gracePeriodDays,
                                dueText = g.dueSubText.removeSuffix(" 还款"),
                                onClick = { onGroupClick(g.groupId) },
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            carousel.forEachIndexed { i, _ ->
                                Box(
                                    Modifier
                                        .padding(horizontal = 3.dp)
                                        .height(6.dp)
                                        .width(if (i == pagerState.currentPage) 18.dp else 6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (i == pagerState.currentPage) colors.gold
                                            else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                )
                            }
                        }
                    }
                    // 本期应还（当前轮播组）
                    if (currentGroup != null) {
                        item {
                            val g = currentGroup
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 10.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(colors.paperRaised)
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        "本期应还（${g.bankName}${if (g.cards.size > 1) " · ${g.cards.size} 卡合并" else ""}）",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = g.billAmountCents?.let { Money.format(it - g.paidAmountCents) } ?: "待录入",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldText,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "还款日",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        g.dueSubText.removeSuffix(" 还款"),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
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
                                onClick = { uiState.todoTargetGroupId?.let(onTodoClick) },
                            )
                        }
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "银行账单",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.weight(1f))
                            SortSegment(selected = uiState.sortMode, onSelect = viewModel::onSortModeChange)
                        }
                    }
                    items(uiState.groups, key = { it.groupId }) { group ->
                        BankLedgerRow(
                            bankName = group.bankName,
                            subLabel = if (group.cards.size > 1) "${group.cards.size} 张卡合并出账"
                            else "尾号 ${group.cards.firstOrNull()?.tail ?: "????"}",
                            gracePeriodDays = group.gracePeriodDays,
                            dueDays = group.dueDays,
                            dueText = group.dueText,
                            dueSubText = group.dueSubText,
                            dueState = group.dueState,
                            billState = group.billState,
                            billDate = group.billDate,
                            dueDate = group.dueDate,
                            today = uiState.today,
                            tabColor = tabColorFor(hue = group.hue),
                            tabGlyph = group.bankName.firstOrNull()?.toString() ?: "卡",
                            onClick = { onGroupClick(group.groupId) },
                        )
                    }
                    item { Spacer(Modifier.height(84.dp)) }
                }
            }
        }
    }
}

/** 打开通知设置：部分 ROM 缺少标准设置页，回退到应用详情页，避免闪退 */
private fun openNotificationSettings(context: Context) {
    val packageUri = Uri.fromParts("package", context.packageName, null)
    try {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS, packageUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (e: ActivityNotFoundException) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (ignored: Exception) {
            // 极端 ROM 无可用设置页：静默忽略，不崩溃
        }
    }
}

private fun isNotificationGranted(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
