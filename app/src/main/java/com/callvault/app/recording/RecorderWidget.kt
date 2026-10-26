package com.callvault.app.recording

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.callvault.app.R

class RecorderWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget_recorder)
            val pi = PendingIntent.getForegroundService(
                context, 7,
                Intent(context, RecordingService::class.java).setAction(RecordingService.ACTION_TOGGLE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_button, pi)
            manager.updateAppWidget(id, views)
        }
    }
}
