package com.alosir.card.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alosir.card.ui.screen.HomeScreen
import com.alosir.card.ui.screen.addcard.AddCardScreen
import com.alosir.card.ui.screen.billhistory.BillHistoryScreen
import com.alosir.card.ui.screen.carddetail.CardDetailScreen
import com.alosir.card.ui.screen.groupdetail.GroupDetailScreen
import com.alosir.card.ui.screen.groupform.GroupFormScreen
import com.alosir.card.ui.screen.grouplist.GroupListScreen
import com.alosir.card.ui.screen.pin.PinMode
import com.alosir.card.ui.screen.pin.PinScreen
import com.alosir.card.ui.screen.quotadetail.QuotaDetailScreen
import com.alosir.card.ui.screen.settings.SettingsScreen

/**
 * AloCard 导航宿主。
 * 底部导航 4 项（卡片/额度/账单/设置）常驻；其余目的地全屏推进。
 */
@Composable
fun AloCardNavHost(
    launchGroupId: Long = -1L,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route

    // 通知点击直达账单组（PRD F7.1 规则 4）
    LaunchedEffect(launchGroupId) {
        if (launchGroupId > 0) {
            navController.navigate(Routes.cardDetail(launchGroupId))
        }
    }

    // 截屏保护（PRD F8.1 规则 4）：卡片详情与安全密码页禁止截屏
    val activity = LocalContext.current as? android.app.Activity
    LaunchedEffect(currentRoute) {
        activity ?: return@LaunchedEffect
        val secure = currentRoute == Routes.CARD_DETAIL ||
            currentRoute == Routes.PIN_SETUP || currentRoute == Routes.PIN_CHANGE
        if (secure) {
            activity.window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
            )
        } else {
            activity.window.clearFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
    }

    Scaffold(
        bottomBar = {
            // 仅一级页面显示底部导航；二级流程页（加卡/详情/录入等）全屏
            if (currentRoute in listOf(Routes.HOME, Routes.QUOTA, Routes.GROUPS, Routes.SETTINGS)) {
                AloCardBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
            // 完整页面：打开从右侧快速滑入，返回向右快速滑出（项 1）
            enterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = androidx.compose.animation.core.tween(220),
                )
            },
            exitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = androidx.compose.animation.core.tween(220),
                )
            },
            popEnterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = androidx.compose.animation.core.tween(220),
                )
            },
            popExitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = androidx.compose.animation.core.tween(220),
                )
            },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onGroupClick = { navController.navigate(Routes.cardDetail(it)) },
                    onAddCard = { navController.navigate(Routes.ADD_CARD) },
                    onTodoClick = { navController.navigate(Routes.cardDetail(it)) },
                )
            }
            composable(Routes.QUOTA) {
                QuotaDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.GROUPS) {
                GroupListScreen(
                    onGroupClick = { navController.navigate(Routes.groupDetail(it)) },
                    onAddCard = { navController.navigate(Routes.ADD_CARD) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onSetupPin = { navController.navigate(Routes.PIN_SETUP) },
                    onChangePin = { navController.navigate(Routes.PIN_CHANGE) },
                    onVerifyPin = { navController.navigate(Routes.PIN_VERIFY) },
                )
            }
            composable(Routes.SCAN) {
                com.alosir.card.ui.screen.scan.ScanCardScreen(
                    onBack = { navController.popBackStack() },
                    onDetected = { result ->
                        com.alosir.card.ui.screen.scan.ScanResultBus.lastResult = result
                        navController.popBackStack()
                    },
                )
            }

            composable(Routes.PIN_SETUP) {
                PinScreen(
                    mode = PinMode.SETUP,
                    title = "设置安全密码",
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }
            composable(Routes.PIN_CHANGE) {
                PinScreen(
                    mode = PinMode.CHANGE,
                    title = "修改安全密码",
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }
            composable(Routes.PIN_VERIFY) {
                PinScreen(
                    mode = PinMode.VERIFY,
                    title = "安全验证",
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }

            composable(
                Routes.CARD_DETAIL,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
            ) { entry ->
                val groupId = entry.arguments?.getLong("groupId") ?: -1L
                CardDetailScreen(
                    groupId = groupId,
                    onBack = { navController.popBackStack() },
                    onEditGroup = { navController.navigate(Routes.groupForm(it)) },
                    onEditCard = { navController.navigate("${Routes.ADD_CARD}?cardId=$it") },
                    onPinRequired = { navController.navigate(Routes.PIN_VERIFY) },
                )
            }
            composable(
                Routes.ADD_CARD,
                arguments = listOf(
                    navArgument("cardId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                ),
            ) { entry ->
                val cardId = entry.arguments?.getLong("cardId") ?: -1L
                AddCardScreen(
                    cardId = if (cardId > 0) cardId else null,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    onScanCard = { navController.navigate(Routes.SCAN) },
                )
            }
            composable(
                Routes.GROUP_DETAIL,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
            ) { entry ->
                val groupId = entry.arguments?.getLong("groupId") ?: -1L
                GroupDetailScreen(
                    groupId = groupId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.groupForm(it)) },
                    onHistory = { navController.navigate(Routes.billHistory(it)) },
                    onDeleted = { navController.popBackStack() },
                    onEditCard = { navController.navigate("${Routes.ADD_CARD}?cardId=$it") },
                )
            }
            composable(
                Routes.GROUP_FORM,
                arguments = listOf(
                    navArgument("groupId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                ),
            ) { entry ->
                val groupId = entry.arguments?.getLong("groupId") ?: -1L
                GroupFormScreen(
                    groupId = if (groupId > 0) groupId else -1L,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(
                Routes.BILL_HISTORY,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
            ) { entry ->
                val groupId = entry.arguments?.getLong("groupId") ?: -1L
                BillHistoryScreen(
                    groupId = groupId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
