package gedoor.kunfei.lunarreminder.async;


import android.annotation.SuppressLint;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Events;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Calendar;

import gedoor.kunfei.lunarreminder.ui.BaseActivity;
import gedoor.kunfei.lunarreminder.util.ACache;
import gedoor.kunfei.lunarreminder.util.ChineseCalendar;
import gedoor.kunfei.lunarreminder.util.EventTimeUtil;

import static gedoor.kunfei.lunarreminder.LunarReminderApplication.googleEvents;

/**
 * 获取提醒事件
 */

public class GetReminderEvents extends CalendarAsyncTask {
    private static final String TAG = "AsyncGetEvents";
    private String calendarId;

    public GetReminderEvents(BaseActivity activity, String calendarId) {
        super(activity);
        this.calendarId = calendarId;
    }

    @SuppressLint("WrongConstant")
    @Override
    protected void doInBackground() throws IOException {
        if (activity.showAllEvents) {
            Events events = client.events().list(calendarId).setSingleEvents(true).setOrderBy("startTime")
                    .execute();
            googleEvents = events.getItems();
        } else {
            ChineseCalendar cc = new ChineseCalendar(Calendar.getInstance());
            cc.add(Calendar.DATE, 1);
            DateTime startDT = new DateTime(new EventTimeUtil(cc).getDateTime());
            cc.add(ChineseCalendar.CHINESE_YEAR, 1);
            cc.add(Calendar.DATE, -1);
            DateTime endDT = new DateTime(new EventTimeUtil(cc).getDateTime());
            Events events = client.events().list(calendarId).setSingleEvents(true).setOrderBy("startTime")
                    .setTimeMin(startDT).setTimeMax(endDT).execute();
            googleEvents = events.getItems();
            ACache mCache = ACache.get(activity);
            Gson gson = new Gson();
            String gEvents = gson.toJson(googleEvents);
            mCache.put("events", gEvents);
        }
        new LoadReminderEventList(activity).execute();
    }

}
