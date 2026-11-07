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
import com.alosir.card.domain.util.Money
import com.alosir.card.sub.work.DailySurfacesRefresher
import com.alosir.card.ui.components.BankLogos
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

data class DueWidgetRow(
    val bankName: String,
    val label: String,
    val amountCents: Long,
    val groupId: Long,
)

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
    ) = refresh(context)

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) = refresh(context)

    private fun refresh(context: Context) {
        val ep = EntryPointAccessors.fromApplication(context, DueWidgetEntryPoint::class.java)
        kotlinx.coroutines.runBlocking {
            runCatching { ep.refresher().refresh() }
        }
    }

    companion object {
        fun render(context: Context, rows: List<DueWidgetRow>, totalCents: Long) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, DueBillsWidgetReceiver::class.java)
            )
            ids.forEach { id ->
                val options = manager.getAppWidgetOptions(id)
                val heightCells = WidgetSizing.cellsForDp(
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 140)
                )
                val limit = WidgetSizing.dueItemLimit(heightCells)
                manager.updateAppWidget(id, buildViews(context, rows, totalCents, limit))
            }
        }

        private fun buildViews(
            context: Context,
            rows: List<DueWidgetRow>,
            totalCents: Long,
            limit: Int,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_due_bills)
            views.setTextViewText(R.id.due_total, Money.format(totalCents))
            views.setTextViewText(R.id.due_count, "共 ${rows.size} 笔")
            views.removeAllViews(R.id.due_rows)
            rows.take(limit).forEach { row ->
                val item = RemoteViews(context.packageName, R.layout.widget_due_row).apply {
                    setImageViewResource(
                        R.id.due_logo,
                        BankLogos.drawableFor(row.bankName) ?: R.drawable.bank_placeholder,
                    )
                    setTextViewText(R.id.due_label, row.label)
                    setTextViewText(R.id.due_amount, Money.format(row.amountCents))
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(MainActivity.EXTRA_GROUP_ID, row.groupId)
                    }
                    val pending = android.app.PendingIntent.getActivity(
                        context,
                        row.groupId.toInt(),
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                            android.app.PendingIntent.FLAG_IMMUTABLE,
                    )
                    setOnClickPendingIntent(R.id.due_row_root, pending)
                }
                views.addView(R.id.due_rows, item)
            }
            if (rows.isEmpty()) {
                views.setTextViewText(R.id.due_count, "暂无待还")
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
            views.setOnClickPendingIntent(R.id.due_footer, openIntent)
            return views
        }
    }
}
