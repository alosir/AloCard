package com.alosir.card.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.alosir.card.MainActivity
import com.alosir.card.R
import com.alosir.card.domain.WidgetSizing
import com.alosir.card.sub.work.DailySurfacesRefresher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class WidgetRow(val bankName: String, val days: Int?)

class GracePeriodWidgetReceiver : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun refresher(): DailySurfacesRefresher
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) = refresh(context)

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) = refresh(context)

    private fun refresh(context: Context) {
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        kotlinx.coroutines.runBlocking {
            runCatching { ep.refresher().refresh() }
        }
    }

    companion object {
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

        fun render(context: Context, rows: List<WidgetRow>) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, GracePeriodWidgetReceiver::class.java)
            )
            ids.forEach { id ->
                val options = manager.getAppWidgetOptions(id)
                val widthCells = WidgetSizing.cellsForDp(
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140)
                )
                val heightCells = WidgetSizing.cellsForDp(
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 140)
                )
                val limit = WidgetSizing.graceItemLimit(widthCells, heightCells)
                manager.updateAppWidget(id, buildViews(context, rows, limit, widthCells))
            }
        }

        private fun buildViews(
            context: Context,
            rows: List<WidgetRow>,
            limit: Int,
            columns: Int,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_grace_period)
            views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title))
            views.setTextViewText(R.id.widget_date, LocalDate.now().format(DATE_FMT))
            views.removeAllViews(R.id.widget_rows)
            rows.take(limit).chunked(columns).forEach { rowItems ->
                val row = RemoteViews(context.packageName, R.layout.widget_grace_period_row)
                rowItems.forEach { item ->
                    val cell = RemoteViews(context.packageName, R.layout.widget_grace_period_cell).apply {
                        setTextViewText(R.id.cell_bank, item.bankName)
                        setTextViewText(R.id.cell_days, item.days?.toString() ?: "—")
                    }
                    row.addView(R.id.row_root, cell)
                }
                views.addView(R.id.widget_rows, row)
            }
            val maxDays = rows.mapNotNull { it.days }.maxOrNull()
            views.setTextViewText(
                R.id.widget_footer,
                maxDays?.let { "最长免息 $it 天" } ?: "添加卡片后开始计算",
            )
            if (rows.isEmpty()) {
                views.setTextViewText(R.id.widget_date, context.getString(R.string.widget_empty_hint))
            }

            val openIntent = pendingIntent(context)
            views.setOnClickPendingIntent(R.id.widget_rows, openIntent)
            views.setOnClickPendingIntent(R.id.widget_title, openIntent)
            return views
        }

        private fun pendingIntent(context: Context): android.app.PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return android.app.PendingIntent.getActivity(
                context,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
