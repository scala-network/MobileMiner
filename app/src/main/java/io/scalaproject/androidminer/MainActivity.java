/*
 *  Monero Miner App (c) 2018 Uwe Post
 *  based on the XMRig Monero Miner https://github.com/xmrig/xmrig
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StrictMode;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Spannable;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.anastr.speedviewlib.SpeedView;
import com.github.anastr.speedviewlib.TubeSpeedometer;
import com.github.anastr.speedviewlib.components.Section;
import com.github.anastr.speedviewlib.components.indicators.LineIndicator;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.polyak.iconswitch.IconSwitch;

import org.acra.ACRA;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.scalaproject.androidminer.api.IProviderListener;
import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderData;
import io.scalaproject.androidminer.api.ProviderManager;
import io.scalaproject.androidminer.controls.SimpleTriangleIndicator;
import io.scalaproject.androidminer.widgets.Toolbar;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

public class MainActivity extends BaseActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener
{
    private static final String LOG_TAG = "MainActivity";

    private TextView tvCPUTemperature, tvBatteryTemperature, tvLogWidget, tvLogLayout;
    IconSwitch isPerformanceMode;

    private boolean bIsPerformanceMode = false;

    private boolean payoutEnabled;
    protected IProviderListener payoutListener;

    private Timer timerStatusHashrate = null;
    private TimerTask timerTaskStatusHashrate = null;

    private Timer timerRefreshHashrate = null;
    private TimerTask timerTaskRefreshHashrate = null;

    //private Timer timerMiningSanity = null;
    //private TimerTask timerTaskMiningSanity = null;

    private Timer timerMiningTime = null;
    private TimerTask timerTaskMiningTime = null;

    private boolean validArchitecture = true;

    private MiningService.MiningServiceBinder binder;
    private boolean bPayoutDataReceived = false;

    private boolean bIsRestartEvent = false;
    private boolean bForceMiningOnPauseBattery = false;
    private boolean bForceMiningOnPauseNetwork = false;

    private boolean bValidCPUTemperatureSensor = true;
    private boolean bValidBatteryTemperatureSensor = true;
    private boolean bIsCelsius = true;
    private boolean bForceMiningNoTempSensor = false;

    // Graphics

    ArrayList<Entry> lValuesHr = new ArrayList<>();
    int xHr = 0;

    ArrayList<BarEntry> lValuesTempBattery = new ArrayList<>();
    ArrayList<BarEntry> lValuesTempCPU = new ArrayList<>();
    int xTemp = 0;

    // Settings
    private boolean bDisableTemperatureControl = false;
    private boolean bDisableAMAYC = false;
    private int nMaxCPUTemp = Config.DefaultMaxCPUTemp;
    private int nMaxBatteryTemp = Config.DefaultMaxBatteryTemp;
    private int nSafeCPUTemp = 0;
    private int nSafeBatteryTemp = 0;
    private int nCores = 0;

    private int nSharesCount = 0;
    private int nLastShareCount = 0;

    private int nNbMaxCores = 0;

    private float fSumHr = 0.0f;
    private int nHrCount = 0;
    private float fMaxHr = 0.0f;

    // Temperature Control
    private Timer timerTemperatures = null;
    private TimerTask timerTaskTemperatures = null;
    private final List<String> listCPUTemp = new ArrayList<>();
    private final List<String> listBatteryTemp = new ArrayList<>();

    private boolean isCharging = false;
    static boolean isOnWifi = false;

    public static Context contextOfApplication;

    private boolean isServerConnectionBound = false;
    private boolean isBatteryReceiverRegistered = false;
    private boolean isNetworkReceiverRegistered = false;

    private PowerManager.WakeLock wl;

    public static Context getContextOfApplication() {
        return contextOfApplication;
    }

    private Button btnStart;
    private Boolean bMiningStoppedByUser = false;

    private static int m_nLastCurrentState = Config.STATE_STOPPED;
    private static int m_nCurrentState = Config.STATE_STOPPED;

    private static NotificationManager notificationManager = null;
    private NotificationCompat.Builder notificationBuilder = null;

    private File imagePath = null;

    private boolean isFromLogView = false;

    private int miningMinutes = 0;

    public static boolean isDeviceMiningBackground() {
        return (m_nCurrentState == Config.STATE_CALCULATING || m_nCurrentState == Config.STATE_MINING || m_nCurrentState == Config.STATE_COOLING || m_nCurrentState == Config.STATE_PAUSED);
    }

    private final static int MAX_HASHRATE_TIMER = 34;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        contextOfApplication = getApplicationContext();
        if (wl != null) {
            if (wl.isHeld()) {
                wl.release();
            }
        }

        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        assert pm != null;
        wl = pm.newWakeLock(PARTIAL_WAKE_LOCK, "app:sleeplock");
        wl.acquire(10*60*1000L /*10 minutes*/);

        if(!isServerConnectionBound) {
            Intent intent = new Intent(this, MiningService.class);
            bindService(intent, serverConnection, BIND_AUTO_CREATE);
            startService(intent);
            isServerConnectionBound = true;
        }

        if(!isBatteryReceiverRegistered) {
            registerReceiver(batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            isBatteryReceiverRegistered = true;
        }

        if(!isNetworkReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
            registerReceiver(networkInfoReceiver, intentFilter);
            isNetworkReceiverRegistered = true;
        }

        setContentView(R.layout.activity_main);

        BottomNavigationView navigationView = findViewById(R.id.main_navigation);
        navigationView.getMenu().getItem(0).setChecked(true);
        navigationView.setOnNavigationItemSelectedListener(this);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        toolbar.setOnButtonListener(new Toolbar.OnButtonListener() {
            @Override
            public void onButtonMain(int type) {
                if (type == Toolbar.BUTTON_MAIN_CLOSE) {
                    backHomeMenu();
                }
            }

            @Override
            public void onButtonOptions(int type) {
                switch(type) {
                    case Toolbar.BUTTON_OPTIONS_SHARE: {
                        Bitmap bitmap = takeScreenshot();
                        saveBitmap(bitmap);
                        onShareHashrate();

                        break;
                    }
                    case Toolbar.BUTTON_OPTIONS_SHOW_CORES: {
                        refreshHashrate();

                        break;
                    }
                    case Toolbar.BUTTON_OPTIONS_STATS: {
                        PoolItem selectedPool = ProviderManager.getSelectedPool();

                        String walletAddress = Config.read(Config.CONFIG_ADDRESS);
                        String poolUrl = walletAddress.isEmpty() ? selectedPool.getPoolUrl() : selectedPool.getWalletURL(Config.read(Config.CONFIG_ADDRESS));

                        if(!poolUrl.startsWith("http"))
                            poolUrl = "https://" + poolUrl;

                        Uri uri = Uri.parse(poolUrl);
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));

                        break;
                    }
                    case Toolbar.BUTTON_OPTIONS_COPY: {
                        Utils.copyToClipboard("Mining Log", tvLogWidget.getText().toString());
                        Utils.showToast(contextOfApplication, "Mining Log copied.", Toast.LENGTH_SHORT, Tools.TOAST_YOFFSET_BOTTOM);
                        break;
                    }
                    default: {
                        // Do nothing
                    }
                }
            }
        });

        toolbar.setTitle("Wallet Address");
        toolbar.setButtonMain(Toolbar.BUTTON_MAIN_LOGO);
        toolbar.setButtonOptions(Toolbar.BUTTON_OPTIONS_SHARE);

        // Leave this here to prevent a crash when app is restored from idle state
        SharedPreferences preferences = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        Config.initialize(preferences);

        SwipeRefreshLayout pullToRefreshHr = findViewById(R.id.pullToRefreshHr);
        pullToRefreshHr.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                BottomNavigationView navigationView = findViewById(R.id.main_navigation);
                if(navigationView.getMenu().findItem(R.id.menu_home).isChecked()) {
                    refreshHashrate();
                } else if (navigationView.getMenu().findItem(R.id.menu_stats).isChecked()){
                    StatsFragment.updateStatsListener();
                }

                pullToRefreshHr.setRefreshing(false);
            }
        });

        // Layouts

        LinearLayout llViewLog = findViewById(R.id.llViewLog);
        llViewLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDetailedLog();
                navigationView.setVisibility(View.GONE);
            }
        });

        LinearLayout llUpdateHashrate = findViewById(R.id.llUpdateHashrate);
        llUpdateHashrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshHashrate();
            }
        });

        // Controls

        LinearLayout llPerformanceMode = findViewById(R.id.llPerformanceMode);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            findViewById(R.id.ivPerformanceMode).setVisibility(View.GONE);

            Switch swPerformanceMode = findViewById(R.id.swPerformanceMode);
            swPerformanceMode.setVisibility(View.GONE);

            isPerformanceMode = (IconSwitch)getLayoutInflater().inflate(R.layout.control_iconswitch, null);

            llPerformanceMode.addView(isPerformanceMode);

            isPerformanceMode.setCheckedChangeListener(new IconSwitch.CheckedChangeListener() {
                @Override
                public void onCheckChanged(IconSwitch.Checked current) {
                    bIsPerformanceMode = current == IconSwitch.Checked.RIGHT;
                    updatePerformanceMode();
                }
            });
        } else {
            Switch swPerformanceMode = findViewById(R.id.swPerformanceMode);
            swPerformanceMode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bIsPerformanceMode = ((Switch)v).isChecked();
                    updatePerformanceMode();
                }
            });
        }

        payoutEnabled = true;
        ProgressBar pbStatus = findViewById(R.id.progress_status);

        pbStatus.setMax(MAX_HASHRATE_TIMER * 2);
        pbStatus.setProgress(0);

        // Output log
        tvLogWidget = findViewById(R.id.tvLogWidget);
        tvLogLayout = findViewById(R.id.tvLogLayout);

        // CPU Cores
        nNbMaxCores = Runtime.getRuntime().availableProcessors();
        String core_config = Config.read(Config.CONFIG_CORES);

        nCores = core_config.isEmpty() ? nNbMaxCores : Integer.parseInt(core_config);

        // Create a dummy meter to add "gaps" to the Cores meter, to separate every core value
        TubeSpeedometer meterCoresGap = findViewById(R.id.meter_cores_gap);
        meterCoresGap.setMaxSpeed(nNbMaxCores);
        meterCoresGap.setTickNumber(nNbMaxCores + 1); // Keep this line to patch a bug in the meter implementation
        meterCoresGap.setOnPrintTickLabel((integer, aFloat) -> {
            String tick = "▮";
            Spannable textSpan = new SpannableString(tick);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            return textSpan;
        });
        meterCoresGap.invalidate();


        TubeSpeedometer meterCores = findViewById(R.id.meter_cores);
        meterCores.makeSections(1, getResources().getColor(R.color.c_yellow), Section.Style.SQUARE);
        meterCores.setMaxSpeed(nNbMaxCores);
        meterCores.speedTo(0, 0);

        SpeedView meterTicks = findViewById(R.id.meter_hashrate_ticks);

        // Hashrate
        TubeSpeedometer meterHashrate = findViewById(R.id.meter_hashrate);
        meterHashrate.makeSections(1, getResources().getColor(R.color.c_blue), Section.Style.SQUARE);

        LineIndicator indicator_speed = new LineIndicator(contextOfApplication, 0.15f);
        indicator_speed.setColor(getResources().getColor(R.color.c_white));
        indicator_speed.setWidth(14.0f);
        meterHashrate.setIndicator(indicator_speed);

        // Average Meter
        TubeSpeedometer meterHashrate_avg = findViewById(R.id.meter_hashrate_avg);
        meterHashrate_avg.makeSections(1, getResources().getColor(android.R.color.transparent), Section.Style.SQUARE);

        SimpleTriangleIndicator indicator_avg = new SimpleTriangleIndicator(contextOfApplication);
        indicator_avg.setWidth(40.0f);
        indicator_avg.setColor(getResources().getColor(R.color.txt_main));
        meterHashrate_avg.setIndicator(indicator_avg);

        // Max Meter
        TubeSpeedometer meterHashrate_max = findViewById(R.id.meter_hashrate_max);
        meterHashrate_max.makeSections(1, getResources().getColor(android.R.color.transparent), Section.Style.SQUARE);

        SimpleTriangleIndicator indicator_max = new SimpleTriangleIndicator(contextOfApplication);
        indicator_max.setWidth(40.0f);
        indicator_max.setColor(getResources().getColor(R.color.c_orange));
        meterHashrate_max.setIndicator(indicator_max);

        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());

        // CPU Temperature

        tvCPUTemperature = findViewById(R.id.cputemp);

        RelativeLayout rlWarningCPUTemperature = findViewById(R.id.rlWarningCPUTemp);
        rlWarningCPUTemperature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!bValidCPUTemperatureSensor) {
                    // Inflate the layout of the popup window
                    View popupView = inflater.inflate(R.layout.warning_cpu_temp_sensor, null);
                    Utils.showPopup(v, inflater, popupView);
                }
            }
        });

        // Battery Temperature

        tvBatteryTemperature = findViewById(R.id.batterytemp);

        RelativeLayout rlWarningBatteryTemperature = findViewById(R.id.rlWarningBatteryTemp);
        rlWarningBatteryTemperature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!bValidBatteryTemperatureSensor) {
                    // Inflate the layout of the popup window
                    View popupView = inflater.inflate(R.layout.warning_battery_temp_sensor, null);
                    Utils.showPopup(v, inflater, popupView);
                }
            }
        });

        btnStart = findViewById(R.id.start);

        if (!Arrays.asList(Config.SUPPORTED_ARCHITECTURES).contains(Tools.getABI())) {
            String sArchError = "Your architecture is not supported: " + Tools.getABI();
            appendLogOutputTextWithDate(sArchError);
            refreshLogOutputView();
            setStatusText(sArchError);

            validArchitecture = false;
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ProviderManager.loadPools(getApplicationContext());

        payoutListener = new IProviderListener() {
            public void onStatsChange(ProviderData d) {
                if (!payoutEnabled) {
                    return;
                }

                PoolItem pi = ProviderManager.getSelectedPool();
                if(pi == null) {
                    return;
                }

                bPayoutDataReceived = true;

                enablePayoutWidget(true, "XLA");
                updatePayoutWidget(d);
            }

            @Override
            public boolean onEnabledRequest() {
                return payoutEnabled;
            }
        };

        ProviderManager.request.setListener(payoutListener).start();
        ProviderManager.afterSave();

        initChartHashrate();
        initChartTemperature();

        startTimerTemperatures();

        createNotificationManager();

        resetAvgMaxHashrate();

        updateMeterHashrate(0.0f);

        updateUI();

        updateCores();

        toolbar.setTitle(getWorkerName(), true);
    }

    @Override
    protected void onDestroy() {
        if (isServerConnectionBound) {
            unbindService(serverConnection);
        }

        if(isBatteryReceiverRegistered) {
            unregisterReceiver(batteryInfoReceiver);
        }

        if(isNetworkReceiverRegistered) {
            unregisterReceiver(networkInfoReceiver);
        }

        super.onDestroy();
    }

    private boolean ignorePerformanceModeEvent = false;
    private void updatePerformanceMode() {
        if(ignorePerformanceModeEvent) {
            ignorePerformanceModeEvent = false;
            return;
        }

        if(bIsPerformanceMode) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogCustom);
            builder.setTitle(getString(R.string.performance_mode))
                    .setMessage(getString(R.string.performance_mode_text))
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            enablePerformanceMode(true);
                        }
                    })
                    .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ignorePerformanceModeEvent = true;

                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                isPerformanceMode.setChecked(IconSwitch.Checked.LEFT);
                            }
                            else {
                                Switch swPerformanceMode = findViewById(R.id.swPerformanceMode);
                                swPerformanceMode.setChecked(false);
                            }

                        }
                    })
                    .show();
        } else {
            enablePerformanceMode(false);
        }
    }

    private void enablePerformanceMode(boolean enable) {
        TubeSpeedometer meterCores = findViewById(R.id.meter_cores);
        TubeSpeedometer meterHashrate = findViewById(R.id.meter_hashrate);
        TubeSpeedometer meterCoresGap = findViewById(R.id.meter_cores_gap);
        TubeSpeedometer meterHashrate_avg = findViewById(R.id.meter_hashrate_avg);
        TubeSpeedometer meterHashrate_max = findViewById(R.id.meter_hashrate_max);
        SpeedView meterTicks = findViewById(R.id.meter_hashrate_ticks);

        LinearLayout llTitleHashrate = findViewById(R.id.llTitleHashrate);
        LinearLayout llChartHashrate = findViewById(R.id.llChartHashrate);

        TextView tvTemperature = findViewById(R.id.tvTemperature);
        LinearLayout llChartTemperature = findViewById(R.id.llChartTemperature);

        if(enable) {
            meterHashrate.setVisibility(View.INVISIBLE);
            meterCores.setVisibility(View.INVISIBLE);
            meterCoresGap.setVisibility(View.INVISIBLE);
            meterHashrate_avg.setVisibility(View.INVISIBLE);
            meterHashrate_max.setVisibility(View.INVISIBLE);
            meterTicks.setVisibility(View.INVISIBLE);

            llTitleHashrate.setVisibility(View.GONE);
            llChartHashrate.setVisibility(View.GONE);

            tvTemperature.setVisibility(View.GONE);
            llChartTemperature.setVisibility(View.GONE);

            ProviderManager.request.setListener(payoutListener).stop();

            stopTimerRefreshHashrate();

            //stopTimerMiningSanity();

            LineChart chartHashrate = findViewById(R.id.chartHashrate);
            chartHashrate.clear();

            BarChart chartTemperature = findViewById(R.id.chartTemperature);
            chartTemperature.clear();
        } else {
            meterHashrate.setVisibility(View.VISIBLE);
            meterCores.setVisibility(View.VISIBLE);
            meterCoresGap.setVisibility(View.VISIBLE);
            meterHashrate_avg.setVisibility(View.VISIBLE);
            meterHashrate_max.setVisibility(View.VISIBLE);
            meterTicks.setVisibility(View.VISIBLE);

            llTitleHashrate.setVisibility(View.VISIBLE);
            llChartHashrate.setVisibility(View.VISIBLE);

            tvTemperature.setVisibility(View.VISIBLE);
            llChartTemperature.setVisibility(View.VISIBLE);

            refreshHashrate();

            updateTemperaturesChart();

            ProviderManager.request.setListener(payoutListener).start();

            startTimerRefreshHashrate();

            //startTimerMiningSanity();
        }

        updatePayoutWidgetStatus();

        setMinerStatus(m_nCurrentState);
    }

    private void backHomeMenu() {
        isFromLogView = false;

        showMenuHome();

        BottomNavigationView navigationView = findViewById(R.id.main_navigation);
        navigationView.setVisibility(View.VISIBLE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setButtonMain(Toolbar.BUTTON_MAIN_LOGO);
    }

    private void initChartHashrate() {
        LineChart chartHashrate = findViewById(R.id.chartHashrate);
        chartHashrate.getDescription().setEnabled(false);
        chartHashrate.setTouchEnabled(true);
        chartHashrate.setDragEnabled(true);
        chartHashrate.setScaleEnabled(true);
        chartHashrate.setDrawGridBackground(false);
        chartHashrate.setHighlightPerDragEnabled(true);
        chartHashrate.setPinchZoom(true);
        chartHashrate.animateX(1000);
        chartHashrate.getAxisRight().setEnabled(false);
        chartHashrate.setVisibleXRangeMaximum(10);
        chartHashrate.setNoDataText("No chart data.");
        chartHashrate.setNoDataTextColor(getResources().getColor(R.color.txt_inactive));

        XAxis xAxis = chartHashrate.getXAxis();
        xAxis.setEnabled(false);

        Legend l = chartHashrate.getLegend();
        l.setEnabled(false);

        YAxis leftAxis = chartHashrate.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(R.color.txt_secondary));
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawAxisLine(true);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularityEnabled(false);

        chartHashrate.setDragDecelerationFrictionCoef(0.9f);
    }

    private void initChartTemperature() {
        BarChart chartTemperature = findViewById(R.id.chartTemperature);
        chartTemperature.getDescription().setEnabled(false);
        chartTemperature.setTouchEnabled(true);
        chartTemperature.setDragEnabled(true);
        chartTemperature.setScaleEnabled(true);
        chartTemperature.setDrawGridBackground(false);
        chartTemperature.setHighlightPerDragEnabled(true);
        chartTemperature.setPinchZoom(false);
        chartTemperature.animateX(1000);
        chartTemperature.getAxisRight().setEnabled(false);
        chartTemperature.setAutoScaleMinMaxEnabled(true);
        chartTemperature.setNoDataText("No chart data.");
        chartTemperature.setNoDataTextColor(getResources().getColor(R.color.txt_inactive));

        XAxis xAxis = chartTemperature.getXAxis();
        xAxis.setEnabled(false);

        Legend l = chartTemperature.getLegend();
        l.setEnabled(true);
        l.setTextColor(getResources().getColor(R.color.txt_main));

        YAxis leftAxis = chartTemperature.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(R.color.txt_secondary));
        leftAxis.setAxisMaximum(bIsCelsius ? 90f : Utils.convertCelciusToFahrenheit(90));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawAxisLine(true);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularityEnabled(false);

        setTemperaturesChartLimits();
    }

    private void updateChartTemperatureAxis() {
        BarChart chartTemperature = findViewById(R.id.chartTemperature);

        YAxis leftAxis = chartTemperature.getAxisLeft();
        leftAxis.setAxisMaximum(bIsCelsius ? 90f : Utils.convertCelciusToFahrenheit(90));

        chartTemperature.invalidate();
    }

    private void resetCharts() {
        lValuesHr.clear();
        xHr = 0;

        LineChart chartHashrate = findViewById(R.id.chartHashrate);
        chartHashrate.invalidate();

        lValuesTempBattery.clear();
        lValuesTempCPU.clear();
        xTemp = 0;

        updateChartTemperatureAxis();
    }

    private void addHashrateValue(float hr) {
        lValuesHr.add(new Entry(xHr, hr));
        xHr++;

        // Only keep 100 last values to avoid overflow
        int MAX_HR_VALUES = 100;
        if(lValuesHr.size() > MAX_HR_VALUES)
            lValuesHr.remove(0);

        if(bIsPerformanceMode)
            return;

        LineDataSet set1;
        LineChart chartHashrate = findViewById(R.id.chartHashrate);
        LineData data = chartHashrate.getData();
        YAxis leftAxis = chartHashrate.getAxisLeft();

        if (data != null && data.getDataSetCount() > 0 && lValuesHr.size() > 1) {
            set1 = (LineDataSet) data.getDataSetByIndex(0);
            set1.setValues(lValuesHr);
            data.notifyDataChanged();
        } else {
            // Set Min/Max YAxis
            leftAxis.setAxisMaximum(hr * 1.25f);
            leftAxis.setAxisMinimum(hr * 0.75f);

            set1 = new LineDataSet(lValuesHr, "Hashrate");

            set1.setLabel("");
            set1.setAxisDependency(YAxis.AxisDependency.LEFT);
            set1.setColor(getResources().getColor(R.color.bg_green));
            set1.setCircleColor(Color.WHITE);
            set1.setLineWidth(2f);
            set1.setCircleRadius(3f);
            set1.setFillAlpha(65);
            set1.setFillColor(getResources().getColor(R.color.bg_green));
            set1.setHighLightColor(Color.rgb(244, 117, 117));
            set1.setDrawCircleHole(false);

            data = new LineData(set1);
            data.setDrawValues(false);

            chartHashrate.setData(data);
        }

        // Update maximum/minimum hashrate if needed
        if(hr > leftAxis.getAxisMaximum() * 0.9) {
            leftAxis.setAxisMaximum(hr * 1.25f);
        }

        if(hr < leftAxis.getAxisMinimum() * 1.1) {
            leftAxis.setAxisMinimum(hr * 0.75f);
        }

        chartHashrate.fitScreen();

        data.setHighlightEnabled(false);

        chartHashrate.notifyDataSetChanged();
        chartHashrate.setMaxVisibleValueCount(10);
        chartHashrate.setVisibleXRangeMaximum(10);
        chartHashrate.moveViewToX(data.getEntryCount());
        chartHashrate.invalidate();
    }

    private void setTemperaturesChartLimits() {
        BarChart chartTemperature = findViewById(R.id.chartTemperature);
        YAxis leftAxis = chartTemperature.getAxisLeft();

        leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines

        if(!bDisableTemperatureControl) {
            LimitLine ll1 = new LimitLine(bIsCelsius ? nMaxCPUTemp : Utils.convertCelciusToFahrenheit(nMaxCPUTemp), "CPU");
            ll1.setLineWidth(1f);
            ll1.enableDashedLine(10f, 10f, 0f);
            ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
            ll1.setLineColor(getResources().getColor(R.color.c_red));
            ll1.setTextColor(getResources().getColor(R.color.txt_main));

            LimitLine ll2 = new LimitLine(bIsCelsius ? nMaxBatteryTemp : Utils.convertCelciusToFahrenheit(nMaxBatteryTemp), "Battery");
            ll2.setLineWidth(1f);
            ll2.enableDashedLine(10f, 10f, 0f);
            ll2.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
            ll2.setLineColor(getResources().getColor(R.color.c_red));
            ll2.setTextColor(getResources().getColor(R.color.txt_main));

            leftAxis.addLimitLine(ll1);
            leftAxis.addLimitLine(ll2);
        }
    }

    private void updateTemperaturesChart() {
        BarDataSet set1, set2;

        BarChart chartTemperature = findViewById(R.id.chartTemperature);
        BarData data = chartTemperature.getBarData();

        if (data != null && data.getDataSetCount() > 0) {
            set1 = (BarDataSet) data.getDataSetByIndex(0);
            set2 = (BarDataSet) data.getDataSetByIndex(1);
            set1.setValues(lValuesTempCPU);
            set2.setValues(lValuesTempBattery);
            data.notifyDataChanged();
        } else {
            set1 = new BarDataSet(lValuesTempCPU, "CPU");
            set1.setColor(getResources().getColor(R.color.c_blue));
            set1.setAxisDependency(YAxis.AxisDependency.LEFT);

            set2 = new BarDataSet(lValuesTempBattery, "Battery");
            set2.setColor(getResources().getColor(R.color.c_grey));
            set2.setAxisDependency(YAxis.AxisDependency.LEFT);

            data = new BarData(set1, set2);
            data.setDrawValues(false);

            chartTemperature.setData(data);
        }

        float groupSpace = 0.06f;
        float barSpace = 0.02f; // x2 dataset
        float barWidth = 0.45f; // x2 dataset
        // (0.45 + 0.02) * 2 + 0.06 = 1.00 -> interval per "group"

        // specify the width each bar should have
        data.setBarWidth(barWidth);
        data.setDrawValues(false);
        data.setHighlightEnabled(false);

        // barData.getGroupWith(...) is a helper that calculates the width each group needs based on the provided parameters
        chartTemperature.getXAxis().setAxisMaximum(0 + chartTemperature.getBarData().getGroupWidth(groupSpace, barSpace) * set1.getEntryCount());
        chartTemperature.groupBars(0, groupSpace, barSpace);

        chartTemperature.fitScreen();

        chartTemperature.setMaxVisibleValueCount(20);
        chartTemperature.setVisibleXRangeMaximum(20);
        chartTemperature.moveViewToX(data.getEntryCount());

        chartTemperature.invalidate();
    }

    private void addTemperaturesValue(float cpu, float battery) {
        if(!isDeviceMining())
            return;

        float cpu_temp = bIsCelsius ? cpu : Utils.convertCelciusToFahrenheit(Math.round(cpu));
        float battery_temp = bIsCelsius ? battery : Utils.convertCelciusToFahrenheit(Math.round(battery));

        lValuesTempCPU.add(new BarEntry(xTemp, cpu_temp));
        lValuesTempBattery.add(new BarEntry(xTemp, battery_temp));
        xTemp++;

        // Only keep 100 last values to avoid overflow
        int MAX_TEMP_VALUES = 50;
        if(lValuesTempCPU.size() > MAX_TEMP_VALUES)
            lValuesTempCPU.remove(0);

        if(lValuesTempBattery.size() > MAX_TEMP_VALUES)
            lValuesTempBattery.remove(0);

        if(bIsPerformanceMode)
            return;

        updateTemperaturesChart();
    }

    public void refreshHashrate() {
        sendInput("h");
    }

    public void startTimerTemperatures() {
        if(timerTemperatures != null) {
            return;
        }

        timerTaskTemperatures = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        updateTemperatures();
                    }
                });
            }
        };

        timerTemperatures = new Timer();
        timerTemperatures.scheduleAtFixedRate(timerTaskTemperatures, 0, Config.CHECK_TEMPERATURE_DELAY);
    }

    public void stopTimerTemperatures() {
        if(timerTemperatures != null) {
            timerTemperatures.cancel();
            timerTemperatures = null;
            timerTaskTemperatures = null;
        }

        listCPUTemp.clear();
        listBatteryTemp.clear();
    }

    private void setStatusText(String status) {
        if (status != null && !status.isEmpty()) {
            Utils.showToast(contextOfApplication, status, Toast.LENGTH_SHORT);
        }
    }

    public void onEditPayoutGoal(View view) {
        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogCustom);
        LayoutInflater li = LayoutInflater.from(alertDialogBuilder.getContext());
        View promptsView = li.inflate(R.layout.prompt_edit_payout_goal, null);
        alertDialogBuilder.setView(promptsView);

        EditText edMiningGoal = promptsView.findViewById(R.id.mininggoal);

        if (!Config.read(Config.CONFIG_MINING_GOAL).isEmpty()) {
            edMiningGoal.setText(Config.read(Config.CONFIG_MINING_GOAL));
        } else {
            TextView tvPayoutGoal = findViewById(R.id.tvPayoutGoal);
            edMiningGoal.setText(tvPayoutGoal.getText());
        }

        // set dialog message
        alertDialogBuilder
                .setTitle("Payout Goal")
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String mininggoal = edMiningGoal.getText().toString().trim();
                        if(!mininggoal.isEmpty()) {
                            Config.write(Config.CONFIG_MINING_GOAL, mininggoal);
                        }

                        Utils.hideKeyboardFrom(contextOfApplication, promptsView);
                        updateStatsListener();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Utils.hideKeyboardFrom(contextOfApplication, promptsView);
                    }
                });


        alertDialogBuilder.show();
    }

    private void updatePayoutWidget(ProviderData d) {
        if(d.isNew) {
            enablePayoutWidget(false, "");
        }
        else if(d.miner.paid == null) {
            enablePayoutWidget(false, "Loading...");
        }
        else {
            enablePayoutWidget(true, "XLA");

            // Payout
            String sBalance = d.miner.balance;
            sBalance = sBalance.replace("XLA", "").trim();
            TextView tvBalance = findViewById(R.id.balance_payout);
            tvBalance.setText(sBalance.isEmpty() ? Tools.getLongValueString(0.0) : sBalance);

            float fMinPayout;
            if(Config.read(Config.CONFIG_MINING_GOAL).equals("")) {
                fMinPayout = Utils.convertStringToFloat(d.pool.minPayout);
            }
            else
                fMinPayout = Utils.convertStringToFloat(Config.read(Config.CONFIG_MINING_GOAL).trim());

            TextView tvPayoutGoal = findViewById(R.id.tvPayoutGoal);
            tvPayoutGoal.setText(String.valueOf(Math.round(fMinPayout)));

            TextView tvPercentagePayout = findViewById(R.id.percentage);
            ProgressBar pbPayout = findViewById(R.id.progresspayout);

            float fBalance = Utils.convertStringToFloat(sBalance);
            if (fBalance > 0 && fMinPayout > 0) {
                pbPayout.setProgress(Math.round(fBalance));
                pbPayout.setMax(Math.round(fMinPayout));

                String sPercentagePayout = String.valueOf(Math.round(fBalance / fMinPayout *100));
                tvPercentagePayout.setText(sPercentagePayout);
            } else {
                pbPayout.setProgress(0);
                pbPayout.setMax(100);
                tvPercentagePayout.setText("0");
            }
        }
    }

    public void enablePayoutWidget(boolean enable, String text) {
        TextView tvMessage = findViewById(R.id.payoutmessage);

        if (enable) {
            TextView tvBalance = findViewById(R.id.balance_payout);
            tvBalance.setVisibility(View.VISIBLE);

            TextView tvXLAUnit = findViewById(R.id.xlaunit);
            tvXLAUnit.setVisibility(View.VISIBLE);

            TextView tvPercentage = findViewById(R.id.percentage);
            tvPercentage.setVisibility(View.VISIBLE);

            TextView tvPercentageUnit = findViewById(R.id.percentageunit);
            tvPercentageUnit.setVisibility(View.VISIBLE);

            tvMessage.setVisibility(View.GONE);
        }
        else {
            TextView tvBalance = findViewById(R.id.balance_payout);
            tvBalance.setVisibility(View.INVISIBLE);

            TextView tvXLAUnit = findViewById(R.id.xlaunit);
            tvXLAUnit.setVisibility(View.INVISIBLE);

            TextView tvPercentage = findViewById(R.id.percentage);
            tvPercentage.setVisibility(View.INVISIBLE);

            TextView tvPercentageUnit = findViewById(R.id.percentageunit);
            tvPercentageUnit.setVisibility(View.INVISIBLE);

            ProgressBar pbPayout = findViewById(R.id.progresspayout);
            pbPayout.setProgress(0);
            pbPayout.setMax(100);

            if(text.equals("")) {
                tvMessage.setVisibility(View.GONE);
            }
            else {
                tvMessage.setVisibility(View.VISIBLE);
                tvMessage.setText(text);
            }
        }
    }

    private boolean doesPoolSupportAPI() {
        PoolItem pi = ProviderManager.getSelectedPool();

        if(pi == null)
            return false;

        return (pi.getPoolType() != 0);
    }

    private void updatePayoutWidgetStatus() {
        LinearLayout llPayoutWidget = findViewById(R.id.layout_payout);
        llPayoutWidget.setVisibility(View.GONE);

        TextView tvPayout = findViewById(R.id.payout);
        tvPayout.setVisibility(View.GONE);

        payoutEnabled = false;

        if(doesPoolSupportAPI() && !bIsPerformanceMode) {
            tvPayout.setVisibility(View.VISIBLE);
            llPayoutWidget.setVisibility(View.VISIBLE);
        }
        else {
            tvPayout.setVisibility(View.GONE);
            llPayoutWidget.setVisibility(View.GONE);

            return;
        }

        if (Config.read(Config.CONFIG_ADDRESS).equals("")) {
            enablePayoutWidget(false, "");
            payoutEnabled = false;
            return;
        }

        PoolItem pi = ProviderManager.getSelectedPool();

        if (!Config.read(Config.CONFIG_INIT).equals("1") || pi == null) {
            enablePayoutWidget(false, "");
            payoutEnabled = false;
            return;
        }

        if (pi.getPoolType() == 0) {
            enablePayoutWidget(false, "");
            payoutEnabled = false;
            return;
        }

        if (!bPayoutDataReceived) {
            enablePayoutWidget(false, "Loading...");
        }

        payoutEnabled = true;
    }

    private boolean validateSettings() {
        PoolItem pi = ProviderManager.getSelectedPool();

        if(!Config.read(Config.CONFIG_INIT).equals("1")) {
            Utils.showToast(contextOfApplication, getString(R.string.save_settings_first), Toast.LENGTH_SHORT);
            return false;
        }

        String walletaddress = Config.read(Config.CONFIG_ADDRESS);
        if(walletaddress.isEmpty()) {
            Utils.showToast(contextOfApplication, getString(R.string.no_wallet_defined), Toast.LENGTH_SHORT);
            return false;
        }

        if(!Utils.verifyAddress(walletaddress)) {
            Utils.showToast(contextOfApplication, getString(R.string.invalid_address), Toast.LENGTH_SHORT);
            return false;
        }

        if(pi == null) {
            Utils.showToast(contextOfApplication, getString(R.string.no_pool), Toast.LENGTH_SHORT);
            return false;
        }

        if(pi.getPool().isEmpty()) {
            Utils.showToast(contextOfApplication, getString(R.string.no_pool_url), Toast.LENGTH_SHORT);
            return false;
        }

        if(pi.getPort().isEmpty()) {
            Utils.showToast(contextOfApplication, getString(R.string.no_pool_port), Toast.LENGTH_SHORT);
            return false;
        }

        return  true;
    }

    public void updateUI() {
        loadSettings();

        updatePayoutWidgetStatus();
        refreshLogOutputView();

        setTemperaturesChartLimits();
    }

    private String getWorkerName() {
        String workerName = Config.read(Config.CONFIG_WORKERNAME);

        if(workerName.isEmpty())
            return "Your device";

        return workerName;
    }

    private void updateCores() {
        TubeSpeedometer meterCores = findViewById(R.id.meter_cores);
        meterCores.speedTo(nCores, 0);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        SwipeRefreshLayout pullToRefreshHr = findViewById(R.id.pullToRefreshHr);
        LinearLayout llMain = findViewById(R.id.layout_main);
        LinearLayout llLog = findViewById(R.id.layout_mining_log);

        Toolbar toolbar = findViewById(R.id.toolbar);

        switch (item.getItemId()) {
            case R.id.menu_home: { //Main view
                showMenuHome();
                break;
            }
            case R.id.menu_log: {
                showDetailedLog();
                break;
            }
            case R.id.menu_stats: {
                StatsFragment fragment_stats = (StatsFragment) getSupportFragmentManager().findFragmentByTag("fragment_stats");
                if (fragment_stats == null) {
                    fragment_stats = new StatsFragment();
                }

                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment_stats, "fragment_stats").commit();

                toolbar.setButtonOptions(Toolbar.BUTTON_OPTIONS_STATS);
                toolbar.setTitle(getResources().getString(R.string.stats), true);

                llMain.setVisibility(View.VISIBLE);
                llLog.setVisibility(View.GONE);

                pullToRefreshHr.setEnabled(true);

                break;
            }
            case R.id.menu_settings: {
                SettingsFragment settings_fragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("settings_fragment");
                if (settings_fragment == null) {
                    settings_fragment = new SettingsFragment();
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, settings_fragment, "settings_fragment").commit();

                toolbar.setButtonOptions(Toolbar.BUTTON_OPTIONS_NONE);
                toolbar.setTitle(getResources().getString(R.string.settings), true);

                llMain.setVisibility(View.VISIBLE);
                llLog.setVisibility(View.GONE);

                pullToRefreshHr.setEnabled(false);

                break;
            }
            case R.id.menu_help: {
                AboutFragment about_fragment = (AboutFragment) getSupportFragmentManager().findFragmentByTag("about_fragment");
                if (about_fragment == null) {
                    about_fragment = new AboutFragment();
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, about_fragment, "about_fragment").commit();

                toolbar.setButtonOptions(Toolbar.BUTTON_OPTIONS_NONE);
                toolbar.setTitle(getResources().getString(R.string.about), true);

                llMain.setVisibility(View.VISIBLE);
                llLog.setVisibility(View.GONE);

                pullToRefreshHr.setEnabled(false);

                break;
            }
        }

        updateUI();

        return true;
    }

    private void showMenuHome() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null) {
                getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            }
        }

        LinearLayout llMain = findViewById(R.id.layout_main);
        LinearLayout llLog = findViewById(R.id.layout_mining_log);

        llMain.setVisibility(View.VISIBLE);
        llLog.setVisibility(View.GONE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setButtonOptions(Toolbar.BUTTON_OPTIONS_SHARE);
        toolbar.setTitle(getWorkerName(), true);

        SwipeRefreshLayout pullToRefreshHr = findViewById(R.id.pullToRefreshHr);
        pullToRefreshHr.setEnabled(true);

        updateStatsListener();
        updateUI();
    }

    private void showDetailedLog() {
        isFromLogView = true;

        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null) {
                getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            }
        }

        LinearLayout llMain = findViewById(R.id.layout_main);
        LinearLayout llLog = findViewById(R.id.layout_mining_log);

        llMain.setVisibility(View.GONE);
        llLog.setVisibility(View.VISIBLE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setButtonMain(Toolbar.BUTTON_MAIN_CLOSE);
        toolbar.setButtonOptions(Toolbar.BUTTON_OPTIONS_COPY);
        toolbar.setTitle(getResources().getString(R.string.mininglog), true);

        SwipeRefreshLayout pullToRefreshHr = findViewById(R.id.pullToRefreshHr);
        pullToRefreshHr.setEnabled(true);

        updateStatsListener();
        updateUI();
    }

    public void updateStatsListener() {
        ProviderManager.afterSave();
        ProviderManager.request.setListener(payoutListener).start();

        if(!ProviderManager.data.isNew) {
            updatePayoutWidget(ProviderManager.data);
            enablePayoutWidget(true, "XLA");
        }

        StatsFragment.updateStatsListener();
    }

    @Override
    public void onBackPressed() {
        if(isFromLogView) {
            backHomeMenu();
        } else {
            if (isDeviceMiningBackground()) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogCustom);
                builder.setTitle(getString(R.string.stopmining))
                        .setMessage(getString(R.string.closeApp))
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                stopMining();
                                closeApp();
                            }
                        })
                        .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Do nothing
                            }
                        })
                        .show();
            } else {
                closeApp();
            }
        }
    }

    private void closeApp() {
        super.onBackPressed();
    }

    public void loadSettings() {
        // Load AMAYC Settings
        bDisableTemperatureControl = Config.read(Config.CONFIG_DISABLE_TEMPERATURE_CONTROL, "0").equals("1");
        nMaxCPUTemp = Integer.parseInt(Config.read(Config.CONFIG_MAX_CPU_TEMP, Integer.toString(Config.DefaultMaxCPUTemp)).trim());
        nMaxBatteryTemp = Integer.parseInt(Config.read(Config.CONFIG_MAX_BATTERY_TEMP, Integer.toString(Config.DefaultMaxBatteryTemp)).trim());
        int nCooldownThreshold = Integer.parseInt(Config.read(Config.CONFIG_COOLDOWN_THRESHOLD, Integer.toString(Config.DefaultCooldownTheshold)).trim());

        nSafeCPUTemp = nMaxCPUTemp - Math.round((float)nMaxCPUTemp * (float)nCooldownThreshold / 100.0f);
        nSafeBatteryTemp = nMaxBatteryTemp - Math.round((float)nMaxBatteryTemp * (float)nCooldownThreshold / 100.0f);

        nCores = Integer.parseInt(Config.read(Config.CONFIG_CORES, "0"));

        bIsCelsius = Config.read(Config.CONFIG_TEMPERATURE_UNIT, "C").equals("C");

        TextView tvCPUTemperatureUnit = findViewById(R.id.cputempunit);
        tvCPUTemperatureUnit.setText(bIsCelsius ? getString(R.string.celsius) : getString(R.string.fahrenheit));

        TextView tvBatteryTemperatureUnit = findViewById(R.id.batterytempunit);
        tvBatteryTemperatureUnit.setText(bIsCelsius ? getString(R.string.celsius) : getString(R.string.fahrenheit));

        // Disable ACRA Debug Reporting
        ACRA.getErrorReporter().setEnabled(Config.read(Config.CONFIG_SEND_DEBUG_INFO, "0").equals("1"));

        updateCores();
    }

    private void showDisclaimerTemperatureSensors() {
        LayoutInflater li = LayoutInflater.from(this);
        View disclaimerView = li.inflate(R.layout.disclaimer_temperature_sensor, null);

        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogCustom);
        alertDialogBuilder.setView(disclaimerView);

        String disclaimerText = !bValidCPUTemperatureSensor ? getResources().getString(R.string.warning_cpu_temp_sensor) : getResources().getString(R.string.warning_battery_temp_sensor);
        final TextView tvDisclaimer = disclaimerView.findViewById(R.id.tvDisclaimer);
        tvDisclaimer.setHint(disclaimerText);

        Switch swDisclaimer = disclaimerView.findViewById(R.id.chkDontShowAgain);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Config.write(Config.CONFIG_TEMPERATURE_SENSOR_SHOW_WARNING, swDisclaimer.isChecked() ? "0" : "1");
                        bForceMiningNoTempSensor = true;
                        startMining();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }).show();
    }

    private void startMining() {
        if (binder == null) return;

        if (!Config.read(Config.CONFIG_INIT).equals("1")) {
            setStatusText(getString(R.string.save_settings_first));
            return;
        }

        // Cause a crash to test ACRA
        //String sCrashString = null;
        //Log.e("ACRA Test", sCrashString.toString() );

        if (!Utils.verifyAddress(Config.read(Config.CONFIG_ADDRESS))) {
            setStatusText(getString(R.string.invalid_address));
            return;
        }

        if((!bValidCPUTemperatureSensor || !bValidBatteryTemperatureSensor) && !bForceMiningNoTempSensor && !Config.read(Config.CONFIG_TEMPERATURE_SENSOR_SHOW_WARNING).equals("0")) {
            showDisclaimerTemperatureSensors();
            return;
        }

        if (Config.read(Config.CONFIG_PAUSE_ON_BATTERY).equals("1") && !isCharging && !bForceMiningOnPauseBattery) {
            askToForceMiningBattery();
            return;
        }

        if (Config.read(Config.CONFIG_PAUSE_ON_NETWORK).equals("1") && !isOnWifi() && !bForceMiningOnPauseNetwork) {
            askToForceMiningNetwork();
            return;
        }

        bForceMiningOnPauseBattery = false;
        bForceMiningOnPauseNetwork = false;
        bForceMiningNoTempSensor = false;
        bMiningStoppedByUser = false;
        clearMinerLog = true;
        nSharesCount = 0;
        miningMinutes = 0;

        resetOptions();

        resetHashrateTicks();
        resetAvgMaxHashrate();

        resetCharts();

        loadSettings();

        startMiningService();

        startTimerMiningTime();

        TextView tvAcceptedShares = findViewById(R.id.acceptedshare);
        tvAcceptedShares.setText("0");
        tvAcceptedShares.setTextColor(getResources().getColor(R.color.txt_inactive));

        updateMiningTime();

        showNotificationPause();

        setMinerStatus(Config.STATE_MINING);

        //startTimerMiningSanity();

        updateUI();
    }

    private void startMiningService() {
        String password = Config.read(Config.CONFIG_WORKERNAME);
        String address = Config.read(Config.CONFIG_ADDRESS);

        String username = address + Config.read(Config.CONFIG_USERNAME_PARAMETERS);

        MiningService s = binder.getService();
        MiningService.MiningConfig cfg = s.newConfig(
                username,
                password,
                nCores,
                1, // Default
                1 // Default
        );

        s.startMining(cfg);
    }

    private void askToForceMiningBattery() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogCustom);
        builder.setTitle(getString(R.string.confirmstartmining))
                .setMessage(getString(R.string.deviceonbattery))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        bForceMiningOnPauseBattery = true;

                        if(isDevicePaused()) {
                            clearMinerLog = false;
                            resumeMining();
                        }
                        else
                            startMining();
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        bForceMiningOnPauseBattery = false;
                    }
                })
                .show();
    }

    private void askToForceMiningNetwork() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogCustom);
        builder.setTitle(getString(R.string.confirmstartmining))
                .setMessage(getString(R.string.devicenotonwifi))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        bForceMiningOnPauseNetwork = true;

                        if(isDevicePaused()) {
                            clearMinerLog = false;
                            resumeMining();
                        }
                        else
                            startMining();
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        bForceMiningOnPauseNetwork = false;
                    }
                })
                .show();
    }

    private void resetOptions() {
        bDisableAMAYC = false;
        listCPUTemp.clear();
        listBatteryTemp.clear();

        nLastShareCount = 0;

        fSumHr = 0.0f;
        nHrCount = 0;
        fMaxHr = 0.0f;
    }

    public void stopMining() {
        if(binder == null) {
            return;
        }

        bMiningStoppedByUser = true;

        setMinerStatus(Config.STATE_STOPPED);

        binder.getService().stopMining();

        resetOptions();

        //stopTimerMiningSanity();

        stopTimerMiningTime();

        appendLogOutputTextWithDate(getResources().getString(R.string.stopped));

        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateUI();

        ProviderManager.request.setListener(payoutListener).start();

        if(!isServerConnectionBound) {
            Intent intent = new Intent(this, MiningService.class);
            bindService(intent, serverConnection, BIND_AUTO_CREATE);
            startService(intent);
            isServerConnectionBound = true;
        }

        if(!isBatteryReceiverRegistered) {
            registerReceiver(batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            isBatteryReceiverRegistered = true;
        }

        if(!isNetworkReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
            registerReceiver(networkInfoReceiver, intentFilter);
            isNetworkReceiverRegistered = true;
        }

        SettingsFragment frag = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("settings_fragment");
        if(frag != null) {
            frag.updateAddress();
        }

        refreshLogOutputView();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void toggleMiningState() {
        if (binder == null)
            return;

        if (binder.getService().getMiningServiceState()) {
            stopMining();
        } else {
            startMining();
        }
    }

    private void updateMiningButtonState() {
        if(bIsRestartEvent)
            return;

        Drawable buttonDrawableStart = btnStart.getBackground();
        buttonDrawableStart = DrawableCompat.wrap(buttonDrawableStart);

        if (m_nCurrentState == Config.STATE_STOPPED ) {
            updateHashrate(0.0f, 0.0f);
            DrawableCompat.setTint(buttonDrawableStart, getResources().getColor(R.color.bg_green));
            btnStart.setBackground(buttonDrawableStart);
            btnStart.setText(R.string.start);
        } else if(m_nCurrentState == Config.STATE_PAUSED) {
            updateHashrate(0.0f, 0.0f);
            DrawableCompat.setTint(buttonDrawableStart, getResources().getColor(R.color.bg_green));
            btnStart.setBackground(buttonDrawableStart);
            btnStart.setText(R.string.resume);
        }
        else {
            updateHashrate(-1.0f, -1.0f);
            DrawableCompat.setTint(buttonDrawableStart, getResources().getColor(R.color.bg_lighter));
            btnStart.setBackground(buttonDrawableStart);
            btnStart.setText(R.string.stop);
        }
    }

    private void updateMiningTime() {
        int days =  miningMinutes/24/60;
        int hours = miningMinutes/60%24;
        int minutes = miningMinutes%60;

        String miningTime = "";
        if(days > 0)
            miningTime += days + "d";

        if(hours > 0)
            miningTime += hours + "h";

        if(minutes > 0)
            miningTime += minutes + "m";

        if(miningTime.isEmpty())
            miningTime = "0m";

        TextView tvMiningTime = findViewById(R.id.miningtime);
        tvMiningTime.setTextColor(miningTime.equals("0m") ? getResources().getColor(R.color.txt_inactive) : getResources().getColor(R.color.c_white));
        tvMiningTime.setText(miningTime);
    }

    private void setMinerStatus(Integer status) {
        LinearLayout llHashrate = findViewById(R.id.layout_hashrate);
        LinearLayout llStatus = findViewById(R.id.layout_status);

        if(status == Config.STATE_STOPPED) {
            llStatus.setVisibility(View.GONE);
            llHashrate.setVisibility(View.VISIBLE);

            TextView tvHashrate = findViewById(R.id.hashrate);
            tvHashrate.setText("0");
            tvHashrate.setTextSize(55);
            tvHashrate.setTextColor(getResources().getColor(R.color.txt_inactive));

            View v = findViewById(R.id.main_navigation);
            v.setKeepScreenOn(false);

            updateMeterHashrate(0.0f);

            TubeSpeedometer meterHashrate_avg = findViewById(R.id.meter_hashrate_avg);
            TubeSpeedometer meterHashrate_max = findViewById(R.id.meter_hashrate_max);
            meterHashrate_avg.setVisibility(View.GONE);
            meterHashrate_max.setVisibility(View.GONE);

            stopTimerStatusHashrate();
        }
        else if(status ==Config.STATE_MINING) {
            TextView tvHashrate = findViewById(R.id.hashrate);

            if(tvHashrate.getText().equals("0")) {
                setMinerStatus(Config.STATE_CALCULATING);
            } else {
                llStatus.setVisibility(View.GONE);
                llHashrate.setVisibility(View.VISIBLE);

                tvHashrate.setTextColor(getResources().getColor(R.color.c_white));

                stopTimerStatusHashrate();
            }

            if (Config.read(Config.CONFIG_KEEP_SCREEN_ON_WHEN_MINING).equals("1")) {
                View v = findViewById(R.id.main_navigation);
                v.setKeepScreenOn(true);
            }
        }
        else {
            llStatus.setVisibility(View.VISIBLE);
            llHashrate.setVisibility(View.GONE);

            ProgressBar pbStatus = findViewById(R.id.progress_status);
            pbStatus.setScaleY(1f);

            updateMeterHashrate(0.0f);

            TextView tvStatus = findViewById(R.id.miner_status);
            TextView tvStatusProgess = findViewById(R.id.hr_progress);

            if (status == Config.STATE_PAUSED && isDeviceMining()) {
                tvStatus.setText(getResources().getString(R.string.paused));
                stopTimerStatusHashrate();

                pbStatus.setIndeterminate(true);
                pbStatus.setProgress(0);
                tvStatusProgess.setVisibility(View.INVISIBLE);
            } else if (status == Config.STATE_COOLING && isDeviceMining()) {
                tvStatus.setText(getResources().getString(R.string.cooling));
                stopTimerStatusHashrate();

                tvStatusProgess.setVisibility(View.INVISIBLE);
                pbStatus.setIndeterminate(true);
                pbStatus.setScaleY(3f);
                pbStatus.setProgress(0);
            } else if (status == Config.STATE_CALCULATING) {
                tvStatus.setText(getResources().getString(R.string.processing));
                tvStatusProgess.setVisibility(View.VISIBLE);

                pbStatus.setIndeterminate(false);
                startTimerStatusHashrate();
            }
        }

        m_nLastCurrentState = m_nCurrentState;
        m_nCurrentState = status;

        updateNotification();
    }

    /*public void startTimerMiningSanity() {
        if(timerMiningSanity != null)
            return;

        timerTaskMiningSanity = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        boolean isAlive = binder.getService().isMiningProcessAlive();

                        if(!isAlive && isDeviceMiningBackground() && !bMiningStoppedByUser) {
                            appendLogOutputTextWithDate(getString(R.string.mining_aborted));

                            boolean bRestartOnProcessAborted = Config.read(Config.CONFIG_DISABLE_RESTART_MINING_ABORTED, "0").equals("0");

                            if(bRestartOnProcessAborted) {
                                clearMinerLog = false;
                                appendLogOutputTextWithDate(getString(R.string.restarting_mining_process));
                                appendLogOutputText(System.getProperty("line.separator"));

                                TextView tvAcceptedShares = findViewById(R.id.acceptedshare);
                                nSharesCount = Integer.parseInt(tvAcceptedShares.getText().toString());

                                startMiningService();
                            } else {
                                stopMining();
                                updateMiningButtonState();
                            }
                        }
                    }
                });
            }
        };

        timerMiningSanity = new Timer();

        timerMiningSanity.scheduleAtFixedRate(timerTaskMiningSanity, 5000, Config.CHECK_MINING_SANITY_DELAY);
    }

    public void stopTimerMiningSanity() {
        if(timerMiningSanity != null) {
            timerMiningSanity.cancel();
            timerMiningSanity = null;
            timerTaskMiningSanity = null;
        }
    }*/

    public void startTimerRefreshHashrate() {
        if(timerRefreshHashrate != null)
            return;

        String refreshDelay = Config.read(Config.CONFIG_HASHRATE_REFRESH_DELAY);
        if(refreshDelay.isEmpty() || refreshDelay.equals(String.valueOf(Config.DefaultRefreshDelay)))
            return;

        timerTaskRefreshHashrate = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        refreshHashrate();
                    }
                });
            }
        };

        timerRefreshHashrate = new Timer();

        int nRefreshDelay = Integer.parseInt(refreshDelay);
        timerRefreshHashrate.scheduleAtFixedRate(timerTaskRefreshHashrate, 0, nRefreshDelay * 1000);
    }

    public void stopTimerRefreshHashrate() {
        if(timerRefreshHashrate != null) {
            timerRefreshHashrate.cancel();
            timerRefreshHashrate = null;
            timerTaskRefreshHashrate = null;
        }
    }

    public void startTimerStatusHashrate() {
        if(timerStatusHashrate != null) {
            return;
        }

        stopTimerRefreshHashrate();

        timerTaskStatusHashrate = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        incrementProgressHashrate();
                    }
                });
            }
        };

        timerStatusHashrate = new Timer();
        timerStatusHashrate.scheduleAtFixedRate(timerTaskStatusHashrate, 0, 500);
    }

    public void stopTimerStatusHashrate() {
        if(timerStatusHashrate != null) {
            timerStatusHashrate.cancel();
            timerStatusHashrate = null;
            timerTaskStatusHashrate = null;

            ProgressBar pbStatus = findViewById(R.id.progress_status);
            pbStatus.setProgress(0);

            TextView tvStatusProgess = findViewById(R.id.hr_progress);
            tvStatusProgess.setVisibility(View.VISIBLE);
            tvStatusProgess.setText("0%");

            startTimerRefreshHashrate();
        }
    }

    public void startTimerMiningTime() {
        if(timerMiningTime != null)
            return;

        timerTaskMiningTime = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        miningMinutes++;
                        updateMiningTime();
                    }
                });
            }
        };

        timerMiningTime = new Timer();

        timerMiningTime.scheduleAtFixedRate(timerTaskMiningTime, 60000, Config.CHECK_MINING_TIME_DELAY);
    }

    public void stopTimerMiningTime() {
        if(timerMiningTime != null) {
            timerMiningTime.cancel();
            timerMiningTime = null;
            timerTaskMiningTime = null;
        }
    }

    private void resetHashrateTicks() {
        SpeedView meterTicks = findViewById(R.id.meter_hashrate_ticks);
        TubeSpeedometer meterHashrate = findViewById(R.id.meter_hashrate);
        TubeSpeedometer meterHashrate_avg = findViewById(R.id.meter_hashrate_avg);
        TubeSpeedometer meterHashrate_max = findViewById(R.id.meter_hashrate_max);

        meterTicks.setMaxSpeed(500);
        meterTicks.setTickNumber(0);
        meterTicks.setTextColor(getResources().getColor(android.R.color.transparent));

        meterHashrate.setMaxSpeed(500);
        meterHashrate_avg.setMaxSpeed(500);
        meterHashrate_max.setMaxSpeed(500);
    }

    private float getMaxHr(float fMaxHr) {
        return nNbMaxCores * fMaxHr / nCores * 1.1f;
    }

    private void updateHashrateTicks(float fHr, float fMaxHr) {
        if(bIsPerformanceMode)
            return;

        SpeedView meterTicks = findViewById(R.id.meter_hashrate_ticks);
        float fCurrentMax = meterTicks.getMaxSpeed();

        if((meterTicks.getTickNumber() == 0 || (fCurrentMax > 0 && fHr >= fCurrentMax * 0.9)) && fMaxHr > 0) {
            float hrMax = getMaxHr(fMaxHr);

            // This is not normal, we need to recompute it
            if(fHr > hrMax) {
                hrMax = getMaxHr(fHr);
            }

            if(hrMax <= 10) { // in some case xlarig returns a wrong low hrMax, but it needs to be > tick number, so we force a dummy max number
                hrMax = 11;
            }

            TubeSpeedometer meterHashrate = findViewById(R.id.meter_hashrate);
            TubeSpeedometer meterHashrate_avg = findViewById(R.id.meter_hashrate_avg);
            TubeSpeedometer meterHashrate_max = findViewById(R.id.meter_hashrate_max);

            Log.i(LOG_TAG, "hrMax: " + hrMax);

            meterTicks.setMaxSpeed(hrMax);

            meterTicks.setTickNumber(10);
            meterTicks.setTextColor(getResources().getColor(R.color.txt_main));

            meterHashrate.setMaxSpeed(hrMax);
            meterHashrate.setWithTremble(!(hrMax < 15));

            meterHashrate_avg.setMaxSpeed(hrMax);
            meterHashrate_max.setMaxSpeed(hrMax);
        }
    }

    private void updateMeterHashrate(float fHr) {
        if(bIsPerformanceMode)
            return;

        TubeSpeedometer meterHashrate = findViewById(R.id.meter_hashrate);

        float hrMax = meterHashrate.getMaxSpeed();
        meterHashrate.setWithTremble(!(hrMax < 15) && fHr > 0.0f);

        meterHashrate.speedTo(fHr, 2000);
    }

    private void incrementProgressHashrate() {
        ProgressBar pbStatus = findViewById(R.id.progress_status);
        pbStatus.setProgress(pbStatus.getProgress() + 1);

        String sProgessPercent = String.valueOf(Math.round((float)pbStatus.getProgress() / (float)pbStatus.getMax() *100.0f));

        TextView tvStatusProgess = findViewById(R.id.hr_progress);
        tvStatusProgess.setText(String.format("%s%%", sProgessPercent));
    }

    private boolean isDeviceMining() {
        return (m_nCurrentState == Config.STATE_CALCULATING || m_nCurrentState == Config.STATE_MINING);
    }

    private boolean isDevicePaused() {
        return m_nCurrentState == Config.STATE_PAUSED;
    }

    private boolean isDeviceCooling() {
        return m_nCurrentState == Config.STATE_COOLING;
    }

    private void updateHashrate(float fSpeed, float fMax) {
        if(!isDeviceMining() || fSpeed < 0.0f)
            return;

        SpeedView meterTicks = findViewById(R.id.meter_hashrate_ticks);
        if(meterTicks.getTickNumber() == 0) {
            updateHashrateTicks(fSpeed, fMax);

            // Start timer
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateHashrateMeter(fSpeed, fMax);
                    addHashrateValue(fSpeed);
                }
            }, 2000);
        }
        else {
            updateHashrateTicks(fSpeed, fMax);
            updateHashrateMeter(fSpeed, fMax);
            addHashrateValue(fSpeed);
        }
    }

    private void updateHashrateMeter(float fSpeed, float fMax) {
        updateMeterHashrate(Math.round(fSpeed));

        TextView tvHashrate = findViewById(R.id.hashrate);
        tvHashrate.setText(String.format(Locale.getDefault(), "%.1f", fSpeed));
        tvHashrate.setTextSize(fSpeed > 999f ? 44: 55);

        setMinerStatus(Config.STATE_MINING);

        if(fSpeed <= 0.0f) {
            tvHashrate.setTextColor(getResources().getColor(R.color.txt_inactive));
        }
        else {
            tvHashrate.setTextColor(getResources().getColor(R.color.c_white));
        }

        updateAvgMaxHashrate(fSpeed, fMax);
    }

    private void resetAvgMaxHashrate() { updateAvgMaxHashrate(0.0f, 0.0f); }

    private void updateAvgMaxHashrate(float fSpeed, float fMax) {
        TextView tvAvgHr = findViewById(R.id.avghr);
        TextView tvMaxHr = findViewById(R.id.maxhr);

        // Average Hashrate
        if(fSpeed > 0.0f) {
            nHrCount++;
            fSumHr += fSpeed;

            float fAvgHr = fSumHr / (float) nHrCount;
            tvAvgHr.setText(String.format(Locale.getDefault(), "%.1f", fAvgHr));

            if(!bIsPerformanceMode) {
                TubeSpeedometer meterHashrate_avg = findViewById(R.id.meter_hashrate_avg);

                if (meterHashrate_avg.getVisibility() == View.GONE)
                    meterHashrate_avg.setVisibility(View.VISIBLE);
                meterHashrate_avg.setSpeedAt(fAvgHr);
            }
        }
        else {
            TubeSpeedometer meterHashrate_avg = findViewById(R.id.meter_hashrate_avg);

            tvAvgHr.setText(String.format(Locale.getDefault(), "%.1f", 0.0f));
            meterHashrate_avg.setVisibility(View.GONE);
        }

        // Max Hashrate
        TubeSpeedometer meterHashrate_max = findViewById(R.id.meter_hashrate_max);

        if(fMax > 0.0f) {
            if(fMax > fMaxHr)
                fMaxHr = fMax;

            tvMaxHr.setText(String.format(Locale.getDefault(), "%.1f", fMaxHr));

            if(!bIsPerformanceMode) {
                if (meterHashrate_max.getVisibility() == View.GONE)
                    meterHashrate_max.setVisibility(View.VISIBLE);
                meterHashrate_max.setSpeedAt(fMaxHr);
            }
        }
        else {
            tvMaxHr.setText(String.format(Locale.getDefault(), "%.1f", 0.0f));
            meterHashrate_max.setVisibility(View.GONE);
        }
    }

    private Spannable formatLogOutputText(String text) {
        // Remove date and milliseconds from log
        String formatText = "]";
        if(text.contains(formatText)) {
            StringBuilder sb = new StringBuilder(text);
            text = sb.delete(1, 12).toString();

            int i = text.indexOf(formatText);
            text = sb.delete(i-4, i).toString();
        }

        if(text.contains("paused, press")) {
            if(isDeviceCooling()) {
                text = text.replace("paused, press", getResources().getString(R.string.miningpaused));
                text = text.replace("to resume", "");
                text = text.replace("r ", "");
            } else {
                text = text.replace(", press", "");
                text = text.replace("to resume", "");
                text = text.replace("r ", "");
            }
        }

        if(m_nLastCurrentState == Config.STATE_COOLING && text.contains("resumed")) {
            text = text.replace("resumed", getResources().getString(R.string.resumedmining));
        }

        if (text.contains("threads:")) {
            text = text.replace("threads:", "* THREADS ");
        }

        if (text.contains("COMMANDS")) {
            //text = text + System.getProperty("line.separator");
            return null;
        }

        if (text.contains("POOL")) {
            PoolItem selectedPool = ProviderManager.getSelectedPool();
            if(selectedPool != null) {
                text = text + " POOL URL " + selectedPool.getPoolUrl() + ":" + selectedPool.getPort() + System.getProperty("line.separator");
            }

            text = text + System.getProperty("line.separator");
        }

        boolean speed = false;
        if (text.contains("speed")) {
            text = text.replace("speed ", "");
            text = text.replace("H/s ", "");
            speed = true;
        }

        // For some reason some devices display "miner" instead of "speed"
        if (text.contains("miner")) {
            text = text.replace("miner ", "");
            text = text.replace("H/s ", "");
            speed = true;
        }

        // Remove consecutive spaces
        text = text.replaceAll("( )+", " ");

        if(text.contains("*")) {
            text = text.replace("* ", "");
            Spannable textSpan = new SpannableString(text);

            List<String> listHeader = Arrays.asList("ABOUT", "LIBS", "HUGE PAGES", "1GB PAGES", "CPU", "MEMORY", "DONATE", "POOL", "COMMANDS", "THREADS");
            for (String tmpFormat : listHeader) {
                if (text.contains(tmpFormat)) {
                    int i = text.indexOf(tmpFormat);
                    int imax = i + tmpFormat.length();
                    textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_white)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_grey)), imax, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    String tmpFormat2 = "POOL URL";
                    if(tmpFormat.equals("POOL") && text.contains(tmpFormat2)) {
                        i = text.indexOf(tmpFormat2);
                        imax = i + tmpFormat2.length();
                        textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_white)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_grey)), imax, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    return textSpan;
                }
            }
        }

        Spannable textSpan = new SpannableString(text);

        // Format time
        formatText = "]";
        if(text.contains(formatText)) {
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_dark_grey)), 0, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), 0, 10, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        if (speed) {
            int i = text.indexOf("]");
            int max = text.lastIndexOf("s");
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_blue)), i+1, max+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, max+1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "cpu accepted";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_green)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "cpu READY";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_white)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "net use pool";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_grey)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "net new job from";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_grey)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "rx init dataset";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_grey)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "rx allocated";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_grey)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "rx dataset ready";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_grey)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "cpu use profile";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_grey)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.amayc_ok);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_white)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.amayc_too_hot);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_orange)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.maxtemperaturereached);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_white)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.miningpaused);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_grey)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.resumedmining);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_white)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "paused";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_white)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.stopped);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_white)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "resumed";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_white)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.mining_aborted);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_red)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.restarting_mining_process);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_white)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "AMYAC error";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_yellow)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "AMYAC response";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_yellow)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.amaycerror);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_yellow)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.statictempcontrol);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_white)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        return textSpan;
    }

    private void refreshLogOutputView() {
        LinearLayout llNoActivity = findViewById(R.id.llNoActivity);
        llNoActivity.setVisibility(tvLogWidget.getText().toString().isEmpty() ? View.VISIBLE : View.GONE);

        NestedScrollView svLog = findViewById(R.id.svLog);
        NestedScrollView svLog2 = findViewById(R.id.svLog2);

        if(svLog != null) {
            svLog.post(new Runnable() {
                @Override
                public void run() {
                    svLog.fullScroll(View.FOCUS_DOWN);
                }
            });
        }

        if(tvLogLayout != null) {
            TextView tvNoActivity = findViewById(R.id.tvNoActivity);
            tvNoActivity.setVisibility(tvLogLayout.getText().toString().isEmpty() ? View.VISIBLE : View.GONE);

            if(svLog2 != null) {
                svLog2.post(new Runnable() {
                    @Override
                    public void run() {
                        svLog2.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        }
    }

    private void appendLogOutputText(String line) {
        if(binder != null) {
            if (tvLogWidget.getText().length() > Config.logMaxLength ){
                String outputLog = binder.getService().getOutput();

                Spannable sText = formatLogOutputText(outputLog);
                if(sText != null) {
                    tvLogWidget.setText(sText);
                    tvLogLayout.setText(sText);
                }
            }
        }

        if(!line.equals("")) {
            String outputLog = line + System.getProperty("line.separator");

            Spannable sText = formatLogOutputText(outputLog);
            if(sText != null) {
                tvLogWidget.append(sText);
                tvLogLayout.append(sText);
            }
        }

        refreshLogOutputView();
    }

    private final ServiceConnection serverConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            binder = (MiningService.MiningServiceBinder) iBinder;
            if (validArchitecture) {
                btnStart.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if(!validateSettings())
                            return;

                        if (isDevicePaused()) {
                            if (Config.read(Config.CONFIG_PAUSE_ON_BATTERY).equals("1") && !isCharging && !bForceMiningOnPauseBattery) {
                                askToForceMiningBattery();
                                return;
                            }

                            if (Config.read(Config.CONFIG_PAUSE_ON_NETWORK).equals("1") && !isOnWifi() && !bForceMiningOnPauseNetwork) {
                                askToForceMiningNetwork();
                                return;
                            }

                            clearMinerLog = false;
                            resumeMining();
                        }
                        else {
                            toggleMiningState();
                        }

                        updateMiningButtonState();
                    }
                });

                updateMiningButtonState();

                binder.getService().setMiningServiceStateListener(new MiningService.MiningServiceStateListener() {
                    @Override
                    public void onStateChange(Boolean state) {
                        Log.i(LOG_TAG, "onMiningStateChange: " + state);
                        runOnUiThread(() -> {
                            updateMiningButtonState();
                            if (state) {
                                if (clearMinerLog) {
                                    tvLogWidget.setText("");
                                    tvLogLayout.setText("");

                                    TextView tvAcceptedShares = findViewById(R.id.acceptedshare);
                                    tvAcceptedShares.setText("0");
                                    tvAcceptedShares.setTextColor(getResources().getColor(R.color.txt_inactive));

                                    updateHashrate(-1.0f, -1.0f);
                                }

                                setStatusText("Miner Started.");
                            } else {
                                setStatusText("Miner Stopped.");
                            }

                            bIsRestartEvent = false;
                        });
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onStatusChange(String status, float speed, float max, int accepted, int difficulty, int connection) {
                        runOnUiThread(() -> {
                            appendLogOutputText(status);

                            int nShares = nSharesCount + accepted;
                            String sAccepted = Integer.toString(nShares);

                            TextView tvAcceptedShares = findViewById(R.id.acceptedshare);

                            if(!tvAcceptedShares.getText().equals(sAccepted)) {
                                tvAcceptedShares.setText(sAccepted);
                                tvAcceptedShares.startAnimation(getBlinkAnimation());
                            }

                            if(nLastShareCount != accepted) {
                                nLastShareCount = accepted;
                            }

                            if(accepted == 1) {
                                tvAcceptedShares.setTextColor(getResources().getColor(R.color.c_white));
                            }

                            if(status.contains("10s/60s/15m"))
                                updateHashrate(speed, max);
                        });
                    }
                });
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            binder = null;
        }
    };

    private Animation getBlinkAnimation() {
        Animation animation = new AlphaAnimation(1, 0);         // Change alpha from fully visible to invisible
        animation.setDuration(800);
        animation.setInterpolator(new LinearInterpolator());    // do not alter animation rate
        animation.setRepeatCount(1);
        animation.setRepeatMode(Animation.REVERSE);             // Reverse animation at the end so the button will fade back in

        return animation;
    }

    private void updateTemperatureControls(float cpuTemp) {
        ImageView ivWarningCPUTemp = findViewById(R.id.ivWarningCPUTemp);
        ImageView ivWarningBatteryTemp = findViewById(R.id.ivWarningBatteryTemp);

        if (cpuTemp > 0.0) {
            ivWarningCPUTemp.setVisibility(View.GONE);

            int nCPUTemp = Math.round(cpuTemp);
            tvCPUTemperature.setText(bIsCelsius ? Integer.toString(nCPUTemp) : Integer.toString(Utils.convertCelciusToFahrenheit(nCPUTemp)));

            if(!bDisableTemperatureControl) {
                if(nCPUTemp <= nMaxCPUTemp * 0.9) {
                    tvCPUTemperature.setTextColor(getResources().getColor(R.color.c_green));
                } else if (nCPUTemp > nMaxCPUTemp * 0.9 && nCPUTemp < nMaxCPUTemp) {
                    tvCPUTemperature.setTextColor(getResources().getColor(R.color.c_orange));
                } else {
                    tvCPUTemperature.setTextColor(getResources().getColor(R.color.c_red));
                }
            } else {
                tvCPUTemperature.setTextColor(getResources().getColor(R.color.c_white));
            }
        }
        else {
            bValidCPUTemperatureSensor = false;

            ivWarningCPUTemp.setVisibility(View.VISIBLE);

            tvCPUTemperature.setText("-");
            tvCPUTemperature.setTextColor(getResources().getColor(R.color.txt_inactive));
        }

        if (batteryTemp > 0.0) {
            ivWarningBatteryTemp.setVisibility(View.GONE);

            int nBatteryTemp = Math.round(batteryTemp);
            tvBatteryTemperature.setText(bIsCelsius ? Integer.toString(nBatteryTemp) : Integer.toString(Utils.convertCelciusToFahrenheit(nBatteryTemp)));

            if(!bDisableTemperatureControl) {
                if(nBatteryTemp <= nMaxBatteryTemp * 0.9) {
                    tvBatteryTemperature.setTextColor(getResources().getColor(R.color.c_green));
                } else if (nBatteryTemp > nMaxBatteryTemp * 0.9 && nBatteryTemp < nMaxBatteryTemp) {
                    tvBatteryTemperature.setTextColor(getResources().getColor(R.color.c_orange));
                } else {
                    tvBatteryTemperature.setTextColor(getResources().getColor(R.color.c_red));
                }
            } else {
                tvBatteryTemperature.setTextColor(getResources().getColor(R.color.c_white));
            }
        }
        else {
            bValidBatteryTemperatureSensor = false;

            ivWarningBatteryTemp.setVisibility(View.VISIBLE);

            tvBatteryTemperature.setText("-");
            tvBatteryTemperature.setTextColor(getResources().getColor(R.color.txt_inactive));
        }
    }

    private void updateTemperatures() {
        float cpuTemp = Tools.getCurrentCPUTemperature();
        int nCPUTemp = Math.round(cpuTemp);
        int nBatteryTemp = Math.round(batteryTemp);

        updateTemperatureControls(cpuTemp);

        addTemperaturesValue(cpuTemp, batteryTemp);

        if(bDisableTemperatureControl)
            return;

        // Check if temperatures are now safe to resume mining
        if(isDeviceCooling()) {
            if (nCPUTemp <= nSafeCPUTemp && nBatteryTemp <= nSafeBatteryTemp) {
                enableCooling(false);
            }

            return;
        }

        if(!isDeviceMining())
            return;

        // Check if current temperatures exceed maximum temperatures
        if (nCPUTemp >= nMaxCPUTemp || nBatteryTemp >= nMaxBatteryTemp) {
            enableCooling(true);

            return;
        }

        if(bDisableAMAYC)
            return;

        if(nCPUTemp != 0) {
            listCPUTemp.add(Integer.toString(nCPUTemp));
        }

        if(nBatteryTemp != 0) {
            listBatteryTemp.add(Integer.toString(nBatteryTemp));
        }

        // Send temperatures to AMAYC engine (asynchronously)
        int MAX_NUM_ARRAY = 6;
        if(listCPUTemp.size() >= MAX_NUM_ARRAY || listBatteryTemp.size() >= MAX_NUM_ARRAY)
        {
            String uri = getResources().getString(R.string.amaycPostLink);
            if(!listCPUTemp.isEmpty() && !listBatteryTemp.isEmpty()) {
                //https://amaycapi.hayzam.in/check2?arrayc=[35,42,45,50]&arrayb=[32,43,45,38]
                uri = uri + "check2?arrayc=" + listCPUTemp.toString() + "&arrayb=" + listBatteryTemp.toString();
            } else {
                if(!listCPUTemp.isEmpty()) {
                    uri = uri + "check1?array=" + listCPUTemp.toString();
                } else if(!listBatteryTemp.isEmpty()){
                    uri = uri + "check1?array=" + listBatteryTemp.toString();
                }
            }

            getAMAYCStatus(uri);

            listCPUTemp.clear();
            listBatteryTemp.clear();
        }
    }

    public void getAMAYCStatus(String uri) {
        Log.i(LOG_TAG, "AMAYC uri: " + uri);

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, uri,
                response -> {
                    try {
                        Log.i(LOG_TAG, "AMAYC response: " + response);

                        JSONObject obj = new JSONObject(response);

                        if (uri.contains("check2")) {
                            if(obj.has("predicted_next")) {
                                JSONArray predictedNext = obj.getJSONArray("predicted_next");

                                if (predictedNext.length() == 2) {
                                    int cpupred = (int)Math.round(predictedNext.getDouble(0));
                                    int batterypred = (int)Math.round(predictedNext.getDouble(1));

                                    if (cpupred >= nMaxCPUTemp || batterypred >= nMaxBatteryTemp) {
                                        appendLogOutputTextWithDate(getString(R.string.amayc_too_hot));
                                        enableCooling(true);
                                    } else {
                                        appendLogOutputTextWithDate(getString(R.string.amayc_ok));
                                    }
                                }
                            }
                        } else if (uri.contains("check1")) {
                            if(obj.has("predicted_next")) {
                                double predictedNext = obj.getDouble("predicted_next");

                                if (!listCPUTemp.isEmpty()) {
                                    int cpupred = (int)Math.round(predictedNext);
                                    if (cpupred >= nMaxCPUTemp) {
                                        appendLogOutputTextWithDate(getString(R.string.amayc_too_hot));
                                        enableCooling(true);
                                    } else {
                                        appendLogOutputTextWithDate("AMAYC temperature check: OK");
                                    }
                                } else if (!listBatteryTemp.isEmpty()) {
                                    int batterypred = (int)Math.round(predictedNext);
                                    if (batterypred >= nMaxBatteryTemp) {
                                        appendLogOutputTextWithDate(getString(R.string.amayc_too_hot));
                                        enableCooling(true);
                                    } else {
                                        appendLogOutputTextWithDate("AMAYC temperature check: OK");
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        disableAmaycOnError("AMAYC response: " + e.getMessage());
                    }
                }, this::parseVolleyError);

        queue.add(stringRequest);
    }

    private void parseVolleyError(VolleyError error) {
        String message = "";
        try {
            String jsonMessage = "";
            if(error != null && error.networkResponse != null && error.networkResponse.data != null) {
                String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);

                if (!responseBody.isEmpty()) {
                    JSONObject data = new JSONObject(responseBody);
                    JSONArray errors = data.getJSONArray("errors");
                    jsonMessage = errors.getJSONObject(0).getString("message");
                } else {
                    jsonMessage = error.getMessage();
                }
            }

            if(error != null)
                jsonMessage = error.getMessage();

            assert jsonMessage != null;
            if(jsonMessage.isEmpty())
                jsonMessage = "Unknown";

            message = "AMYAC error: " + jsonMessage;
        } catch (JSONException e) {
            message = "AMYAC error JSONException: " + e.getMessage();
        } finally {
            disableAmaycOnError(message);
        }
    }

    private void disableAmaycOnError(String error) {
        bDisableAMAYC = true;
        appendLogOutputTextWithDate(error);
        appendLogOutputTextWithDate(getResources().getString(R.string.statictempcontrol));
    }

    private void appendLogOutputTextWithDate(String text) {
        appendLogOutputText("[" + Utils.getDateTime() + "] " + text);
    }

    private void enableCooling(boolean enable) {
        if(enable) {
            setMinerStatus(Config.STATE_COOLING);

            pauseMining();

            appendLogOutputTextWithDate(getResources().getString(R.string.maxtemperaturereached));
        }
        else {
            if (Config.read(Config.CONFIG_PAUSE_ON_BATTERY).equals("1") && !isCharging) {
                setStatusText(getResources().getString(R.string.pauseonmining));
                return;
            }

            if (Config.read(Config.CONFIG_PAUSE_ON_NETWORK).equals("1") && !isOnWifi()) {
                setStatusText(getResources().getString(R.string.pauseonnetwork));
                return;
            }

            resumeMining();

            listCPUTemp.clear();
            listBatteryTemp.clear();
        }
    }

    public void pauseMining() {
        if (!isDevicePaused()) {
            if(!isDeviceCooling()) {
                setMinerStatus(Config.STATE_PAUSED);

                updateMiningButtonState();
            }

            if (binder != null) {
                binder.getService().sendInput("p");
            }

            showNotificationResume();

            updateNotification();
        }
    }

    public void resumeMining() {
        if (isDevicePaused() || isDeviceCooling()) {
            setMinerStatus(Config.STATE_MINING);

            if (binder != null) {
                binder.getService().sendInput("r");
            }

            updateMiningButtonState();

            bForceMiningOnPauseBattery = false;
            bForceMiningOnPauseNetwork = false;

            showNotificationPause();

            updateNotification();

            refreshHashrate();
        }
    }

    private void sendInput(String s) {
        if (s.equals("p")) {
            pauseMining();
        }
        else if (s.equals("r")) {
            if(isDeviceCooling()) {
                setStatusText(getResources().getString(R.string.amaycpaused));
                return;
            }

            resumeMining();
        }
        else {
            if (binder != null) {
                binder.getService().sendInput(s);
            }
        }
    }

    public static final String OPEN_ACTION = "OPEN_ACTION";
    public static final String PAUSE_ACTION = "PAUSE_ACTION";
    public static final String RESUME_ACTION = "RESUME_ACTION";
    public static final String STOP_ACTION = "STOP_ACTION";

    private void createNotificationManager() {
        String CHANNEL_ID = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            CHANNEL_ID = "MINING_STATUS";
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, getResources().getString(R.string.miningstatus), NotificationManager.IMPORTANCE_LOW);
            notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }
        else
            notificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationBuilder = new NotificationCompat.Builder(contextOfApplication, CHANNEL_ID);
    }

    private void showNotificationPause() {
        createNotificationManager();

        NotificationsReceiver.activity = this;

        // Open intent
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setAction(OPEN_ACTION);
        PendingIntent pendingIntentOpen = PendingIntent.getActivity(contextOfApplication, 1, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Pause intent
        Intent pauseIntent = new Intent(this, NotificationsReceiver.class);
        pauseIntent.setAction(PAUSE_ACTION);
        PendingIntent pendingIntentPause = PendingIntent.getBroadcast(contextOfApplication, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Stop intent
        Intent stopIntent = new Intent(this, NotificationsReceiver.class);
        stopIntent.setAction(STOP_ACTION);
        PendingIntent pendingIntentStop = PendingIntent.getBroadcast(contextOfApplication, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentTitle(getResources().getString(R.string.devicemining));
        notificationBuilder.setContentIntent(pendingIntentOpen);
        notificationBuilder.addAction(R.mipmap.ic_pause_miner,"Pause", pendingIntentPause);
        notificationBuilder.addAction(R.mipmap.ic_stop_miner,"Stop", pendingIntentStop);
        notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round));
        notificationBuilder.setSmallIcon(R.mipmap.ic_notification);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setOnlyAlertOnce(true);
        notificationBuilder.build();

        notificationManager.notify(1, notificationBuilder.build());
    }

    private void showNotificationResume() {
        createNotificationManager();

        NotificationsReceiver.activity = this;

        // Open intent
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setAction(OPEN_ACTION);
        PendingIntent pendingIntentOpen = PendingIntent.getActivity(contextOfApplication, 1, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Resume intent
        Intent resumeIntent = new Intent(this, NotificationsReceiver.class);
        resumeIntent.setAction(RESUME_ACTION);
        PendingIntent pendingIntentResume = PendingIntent.getBroadcast(contextOfApplication, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Stop intent
        Intent stopIntent = new Intent(this, NotificationsReceiver.class);
        stopIntent.setAction(STOP_ACTION);
        PendingIntent pendingIntentStop = PendingIntent.getBroadcast(contextOfApplication, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentTitle(getResources().getString(R.string.devicemining));
        notificationBuilder.setContentIntent(pendingIntentOpen);
        notificationBuilder.addAction(R.mipmap.ic_start_miner, getResources().getString(R.string.resume), pendingIntentResume);
        notificationBuilder.addAction(R.mipmap.ic_stop_miner, getResources().getString(R.string.stop), pendingIntentStop);
        notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round));
        notificationBuilder.setSmallIcon(R.mipmap.ic_notification);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setOnlyAlertOnce(true);
        notificationBuilder.build();

        notificationManager.notify(1, notificationBuilder.build());
    }

    public static void hideNotifications() {
        if(notificationManager != null)
            notificationManager.cancelAll();
    }

    private void updateNotification() {
        if(notificationManager == null)
            return;

        if(!isDeviceMiningBackground()) {
            hideNotifications();
            return;
        }

        LinearLayout llStatus = findViewById(R.id.layout_status);
        TextView tvHashrate = findViewById(R.id.hashrate);
        TextView tvStatus = findViewById(R.id.miner_status);

        String status = llStatus.getVisibility() == View.GONE ? "Hashrate: " + tvHashrate.getText().toString() + " H/s" : tvStatus.getText().toString();

        notificationBuilder.setSmallIcon(R.mipmap.ic_notification);
        notificationBuilder.setContentText(status);
        notificationManager.notify(1, notificationBuilder.build());
    }

    public Bitmap takeScreenshot() {
        View rootView = findViewById(android.R.id.content).getRootView();
        rootView.setDrawingCacheEnabled(true);
        return rootView.getDrawingCache();
    }

    private void saveBitmap(Bitmap bitmap) {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        imagePath = new File(contextOfApplication.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "scala_miner_screenshot.png");
        FileOutputStream fos;

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }

            if (imagePath.exists()) {
                imagePath.delete();
            }

            fos = new FileOutputStream(imagePath, false);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    private void onShareHashrate() {
        Uri uri = Uri.fromFile(imagePath);
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("image/*");
        String shareBody = getResources().getString(R.string.hashrate_share);
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_title));
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);

        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_via)));
    }

    private boolean clearMinerLog = true;
    static boolean lastIsCharging = false;
    static float batteryTemp = 0.0f;
    private final BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent batteryStatusIntent) {
            if(context == null || batteryStatusIntent == null)
                return;

            batteryTemp = (float) (batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10;

            int status = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

            if (lastIsCharging == isCharging)
                return;

            lastIsCharging = isCharging;

            setStatusText((isCharging ? getResources().getString(R.string.devicecharging) : getResources().getString(R.string.deviceonbatterypower)));

            if (Config.read(Config.CONFIG_PAUSE_ON_BATTERY).equals("0")) {
                clearMinerLog = true;
            } else {
                boolean state = false;
                if (binder != null) {
                    state = binder.getService().getMiningServiceState();
                }

                if (isCharging) {
                    resumeMining();
                } else if (state) {
                    pauseMining();
                }
            }
        }
    };

    private boolean isOnWifi() {
        if(isOnWifiInit)
            return isOnWifi;

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return mWifi.isConnected();
    }

    static boolean lastIsOnWifi = false;
    static boolean isOnWifiInit = false;
    private final BroadcastReceiver networkInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent networkStatusIntent) {
            if (context == null || networkStatusIntent == null)
                return;

            final String action = networkStatusIntent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                isOnWifi = networkStatusIntent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);
            }

            isOnWifiInit = true;

            if (lastIsOnWifi == isOnWifi)
                return;

            lastIsOnWifi = isOnWifi;

            boolean bPauseOnNetwork = Config.read(Config.CONFIG_PAUSE_ON_NETWORK).equals("1");
            if(bPauseOnNetwork) {
                if(isOnWifi) {
                    setStatusText(getResources().getString(R.string.connectedwifi));
                    resumeMining();
                } else {
                    boolean state = binder.getService().getMiningServiceState();
                    if (state) {
                        setStatusText(getResources().getString(R.string.disconnectedwifi));
                        pauseMining();
                    }
                }
            }
        }
    };
}
