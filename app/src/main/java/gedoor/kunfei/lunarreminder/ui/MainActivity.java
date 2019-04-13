package gedoor.kunfei.lunarreminder.ui;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.StringRes;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import gedoor.kunfei.lunarreminder.async.DeleteReminderEvents;
import gedoor.kunfei.lunarreminder.async.GetReminderEvents;
import gedoor.kunfei.lunarreminder.async.InsertReminderEvents;
import gedoor.kunfei.lunarreminder.async.InsertSolarTermsEvents;
import gedoor.kunfei.lunarreminder.async.LoadCalendars;
import gedoor.kunfei.lunarreminder.async.LoadReminderEventList;
import gedoor.kunfei.lunarreminder.async.LoadSolarTermsList;
import gedoor.kunfei.lunarreminder.async.UpdateReminderEvents;
import gedoor.kunfei.lunarreminder.data.FinalFields;
import gedoor.kunfei.lunarreminder.R;
import gedoor.kunfei.lunarreminder.data.GEvent;
import gedoor.kunfei.lunarreminder.help.DonateByAliPay;
import gedoor.kunfei.lunarreminder.help.InitTheme;
import gedoor.kunfei.lunarreminder.ui.view.SimpleAdapterEvent;
import gedoor.kunfei.lunarreminder.util.SharedPreferencesUtil;

import static gedoor.kunfei.lunarreminder.App.eventRepeatNum;
import static gedoor.kunfei.lunarreminder.App.eventRepeatType;
import static gedoor.kunfei.lunarreminder.App.getEvents;
import static gedoor.kunfei.lunarreminder.App.googleEvent;
import static gedoor.kunfei.lunarreminder.App.listEvent;
import static gedoor.kunfei.lunarreminder.App.qrCodeAliPay;
import static gedoor.kunfei.lunarreminder.data.FinalFields.LunarRepeatId;

public class MainActivity extends BaseActivity {
    private static final int REQUEST_REMINDER = 1;
    private static final int REQUEST_SETTINGS = 2;
    public static final int REQUEST_ABOUT = 3;

    private static final int CALENDAR1=1;
    private static final int CALENDAR2=2;

    private int calendarChose=CALENDAR1;

    private InitTheme initTheme;
    private SimpleAdapterEvent adapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private MenuItem menuShowAll;

    @BindView(R.id.main_view)
    View mainView;
    @BindView(R.id.main_toolbar)
    Toolbar toolbar;
    @BindView(R.id.list_view_events)
    ListView listViewEvents;
    @BindView(R.id.text_view_no_events)
    TextView viewNoEvents;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefresh;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.drawer)
    DrawerLayout drawer;
    @BindView(R.id.navigation_view)
    NavigationView navigationView;
    @BindView(R.id.google_user)
    TextView tvGoogleAccount;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        initTheme = new InitTheme(this, false);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        setupActionBar();
        initDrawer();
        initFloatingActionButton();
        initListView();

    }
    //显示日历
    private void setCalendarChooser(int key) {
        swOnRefresh();
        switch (key) {
            case CALENDAR1:
                setTitle(R.string.app_name);
                loadReminderCalendar();
                showReminderHelp(true);
                navigationView.setCheckedItem(R.id.navReminder);
                calendarChose = CALENDAR1;
                break;
            case CALENDAR2:
                setTitle(R.string.solar_terms_24);
                loadSolarTerms();
                showReminderHelp(false);
                navigationView.setCheckedItem(R.id.navSolarTerms);
                calendarChose = CALENDAR2;
                break;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // 这个必须要，没有的话进去的默认是个箭头。。正常应该是三横杠的
        mDrawerToggle.syncState();
        //初始化googleAccount
        initGoogleAccount();
        //评分对话框
        apkScoring();
    }

    @Override
    public void initFinish() {
        tvGoogleAccount.setText(mGoogleAccount);
        setCalendarChooser(calendarChose);
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

    //载入提醒事件
    @Override
    public void loadReminderCalendar() {
        Boolean isFirstOpen = sharedPreferences.getBoolean(getString(R.string.pref_key_first_open), true);
        if (sharedPreferences.getBoolean(getString(R.string.pref_key_cache_events), true) && !isFirstOpen) {
            getEvents(mContext);
        }
        getCalendarId();
        if (lunarReminderCalendarId == null) {
            new LoadCalendars(this, getString(R.string.lunar_reminder_calendar_name), getString(R.string.pref_key_lunar_reminder_calendar_id)).execute();
        } else if (listEvent != null) {
            new LoadReminderEventList(this).execute();
        } else {
            new GetReminderEvents(this, lunarReminderCalendarId).execute();
        }
    }

    //载入节气
    @Override
    public void loadSolarTerms() {
        getCalendarId();
        String strJQ = SharedPreferencesUtil.getString(this, "jq", null);
        if (solarTermsCalendarId == null) {
            new LoadCalendars(this, getString(R.string.solar_terms_calendar_name), getString(R.string.pref_key_solar_terms_calendar_id)).execute();
        } else if (strJQ != null) {
            new LoadSolarTermsList(this, strJQ).execute();
        }else {
            new InsertSolarTermsEvents(this, solarTermsCalendarId).execute();
        }
    }

    //初始化ListView
    private void initListView() {
        adapter = new SimpleAdapterEvent(mContext, list, R.layout.item_event,
                new String[]{"start", "summary"},
                new int[]{R.id.event_item_date, R.id.event_item_title});
        listViewEvents.setAdapter(adapter);
        listViewEvents.setEmptyView(viewNoEvents);

        //列表点击
        listViewEvents.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            String mId = list.get(position).get("id");
            if (mId.equals("") | mId.equals(getString(R.string.solar_terms_calendar_name))) {
                return;
            }
            Intent intent = new Intent(mContext, EventReadActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("position", Integer.parseInt(mId));
            bundle.putLong("id", position);
            intent.putExtras(bundle);
            startActivityForResult(intent, REQUEST_REMINDER);
        });
        //列表长按AlertDialog
        listViewEvents.setOnItemLongClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            String mId = list.get(position).get("id");
            if (mId.equals("") | mId.equals(getString(R.string.solar_terms_calendar_name))) {
                return true;
            }
            String[] actions = {getString(R.string.action_edit), getString(R.string.action_del)};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(list.get(position).get("summary"));
            builder.setItems(actions, (DialogInterface dialog, int which) -> {
                if (which == 0) {
                    Intent intent = new Intent(mContext, EventEditActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putInt("position", Integer.parseInt(mId));
                    bundle.putLong("id", position);
                    intent.putExtras(bundle);
                    startActivityForResult(intent, REQUEST_REMINDER);
                } else if (which == 1) {
                    Snackbar.make(mainView, getString(R.string.confirmDeletion), Snackbar.LENGTH_LONG)
                            .setAction(R.string.ok, v -> {
                                swOnRefresh();
                                new DeleteReminderEvents(this, lunarReminderCalendarId, new GEvent(this, listEvent.get(Integer.parseInt(mId))).getLunarRepeatId()).execute();
                            })
                            .show();
                }
                dialog.dismiss();
            });
            builder.create();
            builder.show();
            return true;
        });
        //下拉刷新
        swipeRefresh.setOnRefreshListener(() -> {
            getCalendarId();
            switch (calendarChose) {
                case CALENDAR1:
                    new GetReminderEvents(this, lunarReminderCalendarId).execute();
                    break;
                case CALENDAR2:
                    new InsertSolarTermsEvents(this, solarTermsCalendarId).execute();
                    break;
            }
        });
    }
    //悬浮按钮
    private void initFloatingActionButton() {
        fab.setOnClickListener((View view) -> {
            googleEvent = null;
            Intent intent = new Intent(mContext, EventEditActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("position", -1);
            intent.putExtras(bundle);
            startActivityForResult(intent, REQUEST_REMINDER);
        });
    }
    //侧边栏初始化
    private void initDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerToggle.syncState();
        drawer.addDrawerListener(mDrawerToggle);

        setUpNavigationView();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    //侧边栏按钮
    private void setUpNavigationView() {
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            drawer.closeDrawers();
            switch (menuItem.getItemId()) {
                case R.id.navReminder:
                    if (calendarChose != CALENDAR1) {
                        setCalendarChooser(CALENDAR1);
                    }
                    break;
                case R.id.navSolarTerms:
                    if (calendarChose != CALENDAR2) {
                        setCalendarChooser(CALENDAR2);
                    }
                    break;
                case R.id.action_settings:
                    Intent intent = new Intent(this, SettingsActivity.class);
                    this.startActivityForResult(intent, REQUEST_SETTINGS);
                    break;
                case R.id.action_about:
                    Intent intent_about = new Intent(this, AboutActivity.class);
                    this.startActivityForResult(intent_about, REQUEST_ABOUT);
                    break;
                case R.id.action_donate:
                    DonateByAliPay.openAlipayPayPage(this, qrCodeAliPay);
                    break;
            }
            return true;
        });
    }

    public void setTitle(@StringRes int title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    private void showReminderHelp(Boolean show) {
        if (menuShowAll == null) {
            return;
        }
        if (show) {
            fab.show();
            menuShowAll.setEnabled(true);
            menuShowAll.setVisible(true);
        } else {
            fab.hide();
            menuShowAll.setEnabled(false);
            menuShowAll.setVisible(false);
        }
    }

    // 添加菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }
    //菜单状态
    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        menuShowAll = menu.getItem(0);
        menuShowAll.setChecked(showAllEvents);
        return super.onPrepareOptionsMenu(menu);
    }
    //菜单
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_showAllEvents:
                showAllEvents = !showAllEvents;
                item.setChecked(showAllEvents);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("showAllEvents", showAllEvents);
                editor.apply();
                swOnRefresh();
                getCalendarId();
                new GetReminderEvents(this, lunarReminderCalendarId).execute();
                return true;
            case android.R.id.home:
                if (drawer.isDrawerOpen(GravityCompat.START)
                        ) {
                    drawer.closeDrawers();
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
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

    private void apkScoring() {
        int openNum = sharedPreferences.getInt(getString(R.string.pref_key_open_num), 0);
        boolean apkScoring = sharedPreferences.getBoolean(getString(R.string.pref_key_apk_score), false);
        if (openNum < 100) {
            openNum = openNum + 1;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(getString(R.string.pref_key_open_num), openNum);
            editor.apply();
        }
        if (openNum > 5 & !apkScoring) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.scoring);
            builder.setMessage(R.string.scoringBody);
            builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                String mAddress = "market://details?id=" + getPackageName();
                Intent marketIntent = new Intent("android.intent.action.VIEW");
                marketIntent.setData(Uri.parse(mAddress ));
                startActivity(marketIntent);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(getString(R.string.pref_key_apk_score), true);
                editor.apply();
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> {

            });
            builder.show();
        }
    }

    @Override
    public void userRecoverable() {
        swNoRefresh();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_REMINDER:
                    swOnRefresh();
                    Bundle bundle = data.getExtras();
                    switch (bundle != null ? bundle.getInt(FinalFields.OPERATION) : 0) {
                        case FinalFields.OPERATION_INSERT:
                            new InsertReminderEvents(this, lunarReminderCalendarId, googleEvent, eventRepeatType, eventRepeatNum).execute();
                            break;
                        case FinalFields.OPERATION_UPDATE:
                            new UpdateReminderEvents(this, lunarReminderCalendarId, googleEvent, eventRepeatType, eventRepeatNum).execute();
                            break;
                        case FinalFields.OPERATION_DELETE:
                            new DeleteReminderEvents(this, lunarReminderCalendarId, googleEvent.getExtendedProperties().getPrivate().get(LunarRepeatId)).execute();
                            break;
                    }
                    break;

            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawers();
                return true;
            }
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
