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
import com.alosir.card.ui.screen.amountentry.AmountEntryScreen
import com.alosir.card.ui.screen.billhistory.BillHistoryScreen
import com.alosir.card.ui.screen.carddetail.CardDetailScreen
import com.alosir.card.ui.screen.groupdetail.GroupDetailScreen
import com.alosir.card.ui.screen.groupform.GroupFormScreen
import com.alosir.card.ui.screen.grouplist.GroupListScreen
import com.alosir.card.ui.screen.paymentmark.PaymentMarkScreen
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
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
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
                    onEntry = { navController.navigate(Routes.amountEntry(it)) },
                    onPay = { navController.navigate(Routes.paymentMark(it)) },
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
                    onAmountEntry = { navController.navigate(Routes.amountEntry(it)) },
                    onPaymentMark = { navController.navigate(Routes.paymentMark(it)) },
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
            composable(
                Routes.AMOUNT_ENTRY,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
            ) { entry ->
                val groupId = entry.arguments?.getLong("groupId") ?: -1L
                AmountEntryScreen(
                    groupId = groupId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(
                Routes.PAYMENT_MARK,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
            ) { entry ->
                val groupId = entry.arguments?.getLong("groupId") ?: -1L
                PaymentMarkScreen(
                    groupId = groupId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
        }
    }
}
