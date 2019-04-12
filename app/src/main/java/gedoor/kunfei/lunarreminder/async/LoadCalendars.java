package gedoor.kunfei.lunarreminder.async;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;

import java.io.IOException;
import java.util.TimeZone;

import gedoor.kunfei.lunarreminder.R;
import gedoor.kunfei.lunarreminder.ui.BaseActivity;
import gedoor.kunfei.lunarreminder.util.SharedPreferencesUtil;

public class LoadCalendars extends CalendarAsyncTask {
    private static final String TAG = "AsyncLoadCalendars";
    private String calendarName;
    private String calendarPrefKey;
    private String calendarId;

    public LoadCalendars(BaseActivity activity, String calendarName, String calendarPrefKey) {
        super(activity);
        this.calendarName = calendarName;
        this.calendarPrefKey = calendarPrefKey;
    }

    @Override
    protected void doInBackground() throws IOException {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        CalendarList feed = client.calendarList().list().setFields("items(id,summary)").execute();
        for (CalendarListEntry calendar : feed.getItems()) {
            Log.d(TAG, "return calendar summary:" + calendar.getSummary() + " timeZone:" + calendar.getTimeZone());
            if (calendar.getSummary().equals(calendarName)) {
                Log.d(TAG, "Lunar Birthday calendar already exist:" + calendar.getId());
                calendarId = calendar.getId();
                editor.putString(calendarPrefKey, calendarId);
                editor.apply();
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if (calendarId == null) {
            new InsertCalendar(activity, calendarName, calendarPrefKey).execute();
        } else if (calendarName.equals(activity.getString(R.string.lunar_reminder_calendar_name))) {
            activity.loadReminderCalendar();
        } else {
            activity.loadSolarTerms();
        }
    }
}
