package gedoor.kunfei.lunarreminder.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupMenu;

import butterknife.BindView;
import butterknife.ButterKnife;
import gedoor.kunfei.lunarreminder.async.DeleteEvents;
import gedoor.kunfei.lunarreminder.async.GetEvents;
import gedoor.kunfei.lunarreminder.async.InsertEvents;
import gedoor.kunfei.lunarreminder.async.LoadCalendars;
import gedoor.kunfei.lunarreminder.async.UpdateEvents;
import gedoor.kunfei.lunarreminder.data.FinalFields;
import gedoor.kunfei.lunarreminder.R;
import gedoor.kunfei.lunarreminder.ui.view.SimpleAdapterEvent;
import pub.devrel.easypermissions.AfterPermissionGranted;

import static gedoor.kunfei.lunarreminder.LunarReminderApplication.calendarID;
import static gedoor.kunfei.lunarreminder.LunarReminderApplication.eventRepeat;
import static gedoor.kunfei.lunarreminder.LunarReminderApplication.googleEvent;
import static gedoor.kunfei.lunarreminder.LunarReminderApplication.googleEvents;

public class MainActivity extends BaseActivity {
    private static final int REQUEST_REMINDER = 1;
    private static final int REQUEST_SETTINGS = 2;
    public static final int REQUEST_ABOUT = 3;

    private SimpleAdapterEvent adapter;

    @BindView(R.id.list_view_events)
    ListView listViewEvents;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefresh;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.drawer)
    DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener((View view)->{drawer.openDrawer(Gravity.START);});
        fab.setOnClickListener((View view) -> {
            googleEvent = null;
            Intent intent = new Intent(this, EventEditActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("position", -1);
            intent.putExtras(bundle);
            startActivityForResult(intent, REQUEST_REMINDER);
        });

        adapter = new SimpleAdapterEvent(this, list, R.layout.item_event,
                new String[]{"start", "summary"},
                new int[]{R.id.event_item_date, R.id.event_item_title});
        listViewEvents.setAdapter(adapter);

        //列表点击
        listViewEvents.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            String mId = list.get(position).get("id");
            if (mId.equals("")) {
                return;
            }
            Intent intent = new Intent(this, EventReadActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("position", Integer.parseInt(mId));
            bundle.putLong("id", position);
            intent.putExtras(bundle);
            startActivityForResult(intent, REQUEST_REMINDER);
        });
        //列表长按
        listViewEvents.setOnItemLongClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            String mId = list.get(position).get("id");
            if (mId.equals("")) {
                return true;
            }
            PopupMenu popupMenu = new PopupMenu(this, view);
            Menu menu = popupMenu.getMenu();
            menu.add(Menu.NONE, Menu.FIRST, 0, "修改");
            menu.add(Menu.NONE, Menu.FIRST + 1, 1, "删除");
            popupMenu.setOnMenuItemClickListener((MenuItem item) -> {
                switch (item.getItemId()) {
                    case Menu.FIRST:
                        Intent intent = new Intent(this, EventEditActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putInt("position", Integer.parseInt(mId));
                        bundle.putLong("id", position);
                        intent.putExtras(bundle);
                        startActivityForResult(intent, REQUEST_REMINDER);
                        return true;
                    case Menu.FIRST + 1:
                        swOnRefresh();
                        new DeleteEvents(this, calendarID, googleEvents.get(Integer.parseInt(mId))).execute();
                        return true;
                }
                return true;
            });
            popupMenu.show();
            return true;
        });
        //下拉刷新
        swipeRefresh.setOnRefreshListener(() -> new GetEvents(this).execute());

        initGoogleAccount();
    }

    @Override
    public void initFinish() {
        swOnRefresh();
        loadGoogleCalendar();
    }

    @Override
    public void syncSuccess() {

    }

    @Override
    public void syncError() {
        swNoRefresh();
    }

    @Override
    public void eventListFinish() {
        refreshView();
    }

    //载入事件
    public void loadGoogleCalendar() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean isFirstOpen = sharedPreferences.getBoolean(getString(R.string.pref_key_first_open), true);
        if (calendarID == null) {
            new LoadCalendars(this, getString(R.string.lunar_calendar_name)).execute();
//        } else if (listCache != null && cacheEvents) {
//            googleEvents = listCache;
//            new LoadEventsList(this).execute();
        } else {
            new GetEvents(this).execute();
        }
        if (isFirstOpen) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }
    }

    // 添加菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //菜单
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_showAllEvents:
                showAllEvents = !showAllEvents;
                swOnRefresh();
                new GetEvents(this).execute();
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivityForResult(intent, REQUEST_SETTINGS);
                return true;
            case R.id.action_about:
                Intent intent_about = new Intent(this, AboutActivity.class);
                this.startActivityForResult(intent_about, REQUEST_ABOUT);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //刷新动画开始
    public void swOnRefresh() {
        swipeRefresh.setProgressViewOffset(false, 0, 52);
        swipeRefresh.setRefreshing(true);
    }

    //刷新动画停止
    public void swNoRefresh() {
        swipeRefresh.setRefreshing(false);
    }

    //刷新事件列表
    public void refreshView() {
        adapter.notifyDataSetChanged();
        swNoRefresh();
    }

    @Override
    public void userRecoverable() {
        swNoRefresh();
    }

    @SuppressLint("WrongConstant")
    @AfterPermissionGranted(REQUEST_PERMS)
    private void methodRequiresPermission() {
        afterPermissionGranted();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_REMINDER:
                    swOnRefresh();
                    Bundle bundle = data.getExtras();
                    switch (bundle.getInt(FinalFields.OPERATION)) {
                        case FinalFields.OPERATION_INSERT:
                            new InsertEvents(this, calendarID, googleEvent, eventRepeat).execute();
                            break;
                        case FinalFields.OPERATION_UPDATE:
                            new UpdateEvents(this, calendarID, googleEvent, eventRepeat).execute();
                            break;
                        case FinalFields.OPERATION_DELETE:
                            new DeleteEvents(this, calendarID, googleEvent).execute();
                            break;
                    }
                    break;

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
