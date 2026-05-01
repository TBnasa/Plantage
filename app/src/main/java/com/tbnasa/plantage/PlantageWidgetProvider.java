package com.tbnasa.plantage;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import com.tbnasa.plantage.model.Leaf;

import java.util.List;

/**
 * PlantageWidgetProvider - Handles the home screen widget updates.
 * Renders the current state of the garden tree.
 */
public class PlantageWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<Leaf> leaves = dbHelper.getAllLeaves();

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_plantage);

        // Render the tree to bitmap
        // Widgets usually have limited size, 512x512 is a good compromise for quality/memory
        Bitmap treeBitmap = PlantageTreeView.renderToBitmap(context, 512, 512, leaves);
        views.setImageViewBitmap(R.id.widget_tree_image, treeBitmap);

        // Click to open app
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_tree_image, pendingIntent);

        // Update status text based on today's leaf
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        Leaf todayLeaf = dbHelper.getLeafByDate(today);
        if (todayLeaf != null && todayLeaf.hasContent()) {
            views.setTextViewText(R.id.widget_status_text, "Gardened Today 🌿");
        } else {
            views.setTextViewText(R.id.widget_status_text, "Needs attention 🌱");
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * Call this when data changes to refresh all widgets.
     */
    public static void refreshAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, PlantageWidgetProvider.class));
        if (ids.length > 0) {
            Intent updateIntent = new Intent(context, PlantageWidgetProvider.class);
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(updateIntent);
        }
    }
}
