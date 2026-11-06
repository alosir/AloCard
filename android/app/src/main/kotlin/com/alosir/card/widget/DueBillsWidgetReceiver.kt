package com.alosir.card.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.alosir.card.MainActivity
import com.alosir.card.R
import com.alosir.card.domain.util.Money
import com.alosir.card.sub.work.DailySurfacesRefresher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** 待还小部件的一行：label（逾期/今天/明天/N天后）+ 金额 */
data class DueWidgetRow(val label: String, val amountCents: Long)

/**
 * 最近待还记录小组件（2x2）。
 * 逾期合并为一行；后续按今天/明天/N天后分组，显示剩余待还金额。
 * 深空灰半透明磨砂圆角矩形背景，点击打开 App。
 */
class DueBillsWidgetReceiver : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DueWidgetEntryPoint {
        fun refresher(): DailySurfacesRefresher
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val ep = EntryPointAccessors.fromApplication(context, DueWidgetEntryPoint::class.java)
        kotlinx.coroutines.runBlocking {
            runCatching { ep.refresher().refresh() }
        }
    }

    companion object {
        const val MAX_ROWS = 5

        fun render(context: Context, rows: List<DueWidgetRow>) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, DueBillsWidgetReceiver::class.java)
            )
            if (ids.isEmpty()) return
            ids.forEach { manager.updateAppWidget(it, buildViews(context, rows)) }
        }

        private fun buildViews(context: Context, rows: List<DueWidgetRow>): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_due_bills)
            views.setTextViewText(R.id.due_title, "最近待还")
            views.removeAllViews(R.id.due_rows)
            rows.take(MAX_ROWS).forEach { row ->
                val item = RemoteViews(context.packageName, R.layout.widget_due_row).apply {
                    setTextViewText(R.id.due_label, row.label)
                    setTextViewText(R.id.due_amount, Money.format(row.amountCents))
                }
                views.addView(R.id.due_rows, item)
            }
            if (rows.isEmpty()) {
                views.setTextViewText(R.id.due_title, "暂无待还账单")
            }
            val openIntent = android.app.PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.due_rows, openIntent)
            views.setOnClickPendingIntent(R.id.due_title, openIntent)
            return views
        }
    }
}
