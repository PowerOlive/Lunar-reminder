package gedoor.kunfei.lunarreminder.async;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import gedoor.kunfei.lunarreminder.R;
import gedoor.kunfei.lunarreminder.ui.BaseActivity;
import gedoor.kunfei.lunarreminder.util.EventTimeUtil;
import gedoor.kunfei.lunarreminder.util.SharedPreferencesUtil;

import static gedoor.kunfei.lunarreminder.data.FinalFields.solarTermsF;
import static gedoor.kunfei.lunarreminder.data.FinalFields.solarTermsJ;

/**
 * Created by GKF on 2017/4/18.
 * 获取节气
 */

public class InsertSolarTermsEvents extends CalendarAsyncTask {
    private String calendarId;
    private ArrayList<HashMap<String, String>> list = new ArrayList<>();

    public InsertSolarTermsEvents(BaseActivity activity, String calendarId) {
        super(activity);
        this.calendarId = calendarId;
    }

    @SuppressLint("WrongConstant")
    @Override
    protected void doInBackground() throws IOException {
        getCalendarColor();
        // deleteEvents();
        Calendar c = Calendar.getInstance();
        String urlStr = "http://data.weather.gov.hk/gts/time/calendar/text/T" + c.get(Calendar.YEAR) + "c.txt";
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int code = connection.getResponseCode();
        if (HttpURLConnection.HTTP_OK == code) {
            connection.connect();
            InputStream is = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "Big5"));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                for (int i = 0; i < 24; i++) {
                    if (line.contains(solarTermsF[i])) {
                        String st[] = line.split(" ");
                        String dt[] = st[0].split("[年月日]");
                        int year = Integer.parseInt(dt[0]);
                        int month = Integer.parseInt(dt[1]);
                        int day = Integer.parseInt(dt[2]);
                        c.set(Calendar.YEAR, year);
                        c.set(Calendar.MONTH, month - 1);
                        c.set(Calendar.DAY_OF_MONTH, day);
                        String start[] = new EventTimeUtil(c).getDate().split("-");
                        Event event = new Event();
                        event.setSummary(solarTermsJ[i]);
                        event.setStart(new EventTimeUtil(c).getEventStartDT());
                        event.setEnd(new EventTimeUtil(c).getEventEndDT());
                        DateTime startDT = new DateTime(new EventTimeUtil(c).getDateTime());
                        c.add(Calendar.DATE, 1);
                        DateTime endDT = new DateTime(new EventTimeUtil(c).getDateTime());
                        Events events = client.events().list(calendarId).setSingleEvents(true).setOrderBy("startTime")
                                .setTimeMin(startDT).setTimeMax(endDT).execute();
                        if (events.getItems().size() == 0) {
                            client.events().insert(calendarId, event).execute();
                        }
                        HashMap<String, String> hp = new HashMap<>();
                        hp.put("start", (start[0] + "\n" + start[1] + "-" + start[2]));
                        hp.put("summary", solarTermsJ[i]);
                        hp.put("id", activity.getString(R.string.solar_terms_calendar_name));
                        list.add(hp);
                    }
                }
            }
            is.close();
            Gson gson = new Gson();
            String str = gson.toJson(list);
            SharedPreferencesUtil.saveData(activity, "jq", str);
        }
        connection.disconnect();

    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        activity.loadSolarTerms();
    }

    private void getCalendarColor() throws IOException {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        CalendarListEntry calendarListEntry = client.calendarList().get(calendarId).execute();
        editor.putInt(activity.getString(R.string.pref_key_solar_terms_calendar_color), Color.parseColor(calendarListEntry.getBackgroundColor()));
        editor.apply();
    }

    private void deleteEvents() throws IOException {
        Events events = client.events().list(calendarId).setFields("items(id)").execute();
        List<Event> items = events.getItems();
        for (Event event : items) {
            client.events().delete(calendarId,event.getId()).execute();
        }
    }

}
