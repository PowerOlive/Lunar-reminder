package gedoor.kunfei.lunarreminder.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.services.calendar.model.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import gedoor.kunfei.lunarreminder.App;
import gedoor.kunfei.lunarreminder.data.FinalFields;
import gedoor.kunfei.lunarreminder.R;
import gedoor.kunfei.lunarreminder.data.GEvent;
import gedoor.kunfei.lunarreminder.data.Properties;
import gedoor.kunfei.lunarreminder.help.InitTheme;
import gedoor.kunfei.lunarreminder.help.ReminderHelp;
import gedoor.kunfei.lunarreminder.util.ChineseCalendar;

import static gedoor.kunfei.lunarreminder.App.googleEvent;
import static gedoor.kunfei.lunarreminder.App.listEvent;

/**
 * 显示事件
 */

public class EventReadActivity extends BaseActivity {
    private static final int REQUEST_REMINDER = 1;

    ChineseCalendar cc = new ChineseCalendar();
    ArrayList<HashMap<String, String>> listReminderDis = new ArrayList<>();
    long eventID;
    int position;
    String lunarRepeatNum;

    @BindView(R.id.tvw_chinese_date)
    TextView textChineseDate;
    @BindView(R.id.text_reminder_me)
    TextView textReminderMe;
    @BindView(R.id.tvw_repeat_num)
    TextView textRepeat;
    @BindView(R.id.list_vw_reminder)
    ListView listViewReminder;
    @BindView(R.id.reminder_toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new InitTheme(this, false);
        setContentView(R.layout.activity_event_read);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener((View view) -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener((View view) -> {
                    Intent intent = new Intent(this, EventEditActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putLong("id", eventID);
                    bundle.putInt("position", position);
                    intent.putExtras(bundle);
                    startActivityForResult(intent, REQUEST_REMINDER);
                }
        );
        googleEvent = new Event();
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            eventID = bundle.getLong("id");
            position = bundle.getInt("position");
            InitGoogleEvent();
        } else {
            finish();
        }
    }

    @SuppressLint("SetTextI18n")
    private void InitGoogleEvent() {
        if (!App.getEvents(mContext)) {
            Toast.makeText(mContext, "获取缓存事件出错, 请下拉强制刷新事件", Toast.LENGTH_LONG).show();
            finish();
        }
        GEvent gEvent = new GEvent(this, listEvent.get(position));
        googleEvent.setExtendedProperties(new Properties(gEvent.getLunarRepeatId(), gEvent.getLunarRepeatNum()).getProperties());
        googleEvent.setId(gEvent.getId());
        textReminderMe.setText(gEvent.getSummary());
        cc.setTime(gEvent.getStart());
        textChineseDate.setText(cc.getChinese(ChineseCalendar.CHINESE_MONTH) + cc.getChinese(ChineseCalendar.CHINESE_DATE));

        lunarRepeatNum = gEvent.getLunarRepeatNum();
        textRepeat.setText(lunarRepeatNum);

        ArrayList<LinkedHashMap<String, Object>> listReminders = gEvent.getReminders();
        listReminderDis.clear();
        if (listReminders != null) {
            for (LinkedHashMap<String, Object> reminder : listReminders) {
                HashMap<String, String> listMap = new HashMap<>();
                listMap.put("txTitle", new ReminderHelp((String) reminder.get("method"), (Double) reminder.get("minutes")).getTitle());
                listReminderDis.add(listMap);
            }
        } else {
            HashMap<String, String> listMap = new HashMap<>();
            listMap.put("txTitle", getString(R.string.defaultReminder));
            listReminderDis.add(listMap);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, listReminderDis, R.layout.item_reminder, new String[]{"txTitle"}, new int[]{R.id.reminder_item_title});
        listViewReminder.setAdapter(adapter);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this  adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_event_read, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putInt(FinalFields.OPERATION, FinalFields.OPERATION_DELETE);
            intent.putExtras(bundle);
            this.setResult(RESULT_OK, intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_REMINDER:
                    this.setResult(RESULT_OK, data);
                    finish();
            }
        }
    }
}
