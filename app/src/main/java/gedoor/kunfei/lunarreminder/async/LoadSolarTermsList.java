package gedoor.kunfei.lunarreminder.async;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import gedoor.kunfei.lunarreminder.R;
import gedoor.kunfei.lunarreminder.ui.BaseActivity;
import gedoor.kunfei.lunarreminder.util.SharedPreferencesUtil;

/**
 * 载入节气
 */

public class LoadSolarTermsList extends CalendarAsyncTask {
    private ArrayList<HashMap<String, String>> list;
    String jqStr;

    public LoadSolarTermsList(BaseActivity activity, String jqStr) {
        super(activity);
        this.jqStr = jqStr;
    }

    @SuppressLint("WrongConstant")
    @Override
    protected void doInBackground() throws IOException {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        int intBgColor = sharedPreferences.getInt(activity.getString(R.string.pref_key_solar_terms_calendar_color), 0);
        String strBgColor = String.format("#%06X", 0xFFFFFF & intBgColor);
        Gson gson = new Gson();
        list = gson.fromJson(jqStr, new TypeToken<ArrayList<HashMap<String, String>>>(){}.getType());
        for (HashMap<String, String> hp : list) {
            hp.put("bgColor", strBgColor);
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        activity.list.clear();
        activity.list.addAll(list);
        activity.eventListFinish();
    }
}
