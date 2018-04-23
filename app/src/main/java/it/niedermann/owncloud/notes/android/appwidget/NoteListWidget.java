package it.niedermann.owncloud.notes.android.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import it.niedermann.owncloud.notes.R;
import it.niedermann.owncloud.notes.android.activity.EditNoteActivity;
import it.niedermann.owncloud.notes.android.activity.NotesListViewActivity;

public class NoteListWidget extends AppWidgetProvider {
    public static final String WIDGET_MODE_KEY = "NLW_mode";
    public static final String WIDGET_CATEGORY_KEY = "NLW_cat";
    public static final String DARK_THEME_KEY = "NLW_darkTheme";
    public static final int NLW_DISPLAY_ALL = 0;
    public static final int NLW_DISPLAY_STARRED = 1;
    public static final int NLW_DISPLAY_CATEGORY = 2;
    private static boolean darkTheme;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews views;

        for (int appWidgetId : appWidgetIds) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            int displayMode = sp.getInt(NoteListWidget.WIDGET_MODE_KEY + appWidgetId, -1);

            // onUpdate has been triggered before the user finished configuring the widget
            if (displayMode == -1) {
                return;
            }

            String category = sp.getString(NoteListWidget.WIDGET_CATEGORY_KEY + appWidgetId, null);
            darkTheme = sp.getBoolean(NoteListWidget.DARK_THEME_KEY + appWidgetId, false);

            Intent serviceIntent = new Intent(context, NoteListWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            serviceIntent.putExtra(NoteListWidget.WIDGET_MODE_KEY + appWidgetId, displayMode);
            serviceIntent.putExtra(NoteListWidget.DARK_THEME_KEY + appWidgetId, darkTheme);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));

            if (displayMode == NLW_DISPLAY_CATEGORY) {
                serviceIntent.putExtra(NoteListWidget.WIDGET_CATEGORY_KEY + appWidgetId, category);
            }

            // Launch application when user taps the header icon or app title
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(new ComponentName(context.getPackageName(),
                                                  NotesListViewActivity.class.getName()));

            // Open the main app if the user taps the widget header
            PendingIntent openAppI = PendingIntent.getActivity( context, 0, intent,
                                                                PendingIntent.FLAG_UPDATE_CURRENT);

            // Launch create note activity if user taps "+" icon on header
            PendingIntent newNoteI = PendingIntent.getActivity( context,0,
                                                                (new Intent(context, EditNoteActivity.class)),
                                                                PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent templatePI = PendingIntent.getActivity(context,0,
                                                                (new Intent(context, EditNoteActivity.class)),
                                                                PendingIntent.FLAG_UPDATE_CURRENT);

            if (darkTheme) {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_note_list_dark);
                views.setOnClickPendingIntent(R.id.widget_note_header_icon_dark, openAppI);
                views.setOnClickPendingIntent(R.id.widget_note_list_title_tv_dark, openAppI);
                views.setOnClickPendingIntent(R.id.widget_note_list_create_icon_dark, newNoteI);
                views.setPendingIntentTemplate(R.id.note_list_widget_lv_dark, templatePI);
                views.setRemoteAdapter(appWidgetId, R.id.note_list_widget_lv_dark, serviceIntent);
                views.setEmptyView(R.id.note_list_widget_lv_dark, R.id.widget_note_list_placeholder_tv_dark);
            } else {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_note_list);
                views.setOnClickPendingIntent(R.id.widget_note_header_icon, openAppI);
                views.setOnClickPendingIntent(R.id.widget_note_list_title_tv, openAppI);
                views.setOnClickPendingIntent(R.id.widget_note_list_create_icon, newNoteI);
                views.setPendingIntentTemplate(R.id.note_list_widget_lv, templatePI);
                views.setRemoteAdapter(appWidgetId, R.id.note_list_widget_lv, serviceIntent);
                views.setEmptyView(R.id.note_list_widget_lv, R.id.widget_note_list_placeholder_tv);
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        AppWidgetManager awm = AppWidgetManager.getInstance(context);
        int appWidgetIds[] = awm.getAppWidgetIds(new ComponentName(context, NoteListWidget.class));

        for (int appWidgetId : appWidgetIds) {
            if (intent.getAction().equals(awm.ACTION_APPWIDGET_UPDATE)) {
                if (darkTheme) {
                    awm.notifyAppWidgetViewDataChanged(appWidgetId, R.id.note_list_widget_lv_dark);
                } else {
                    awm.notifyAppWidgetViewDataChanged(appWidgetId, R.id.note_list_widget_lv);
                }
            }
        }

        super.onReceive(context, intent);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        for (int appWidgetId : appWidgetIds) {
            editor.remove(WIDGET_MODE_KEY + appWidgetId);
            editor.remove(WIDGET_CATEGORY_KEY + appWidgetId);
            editor.remove(DARK_THEME_KEY + appWidgetId);
        }

        editor.apply();
    }
}
