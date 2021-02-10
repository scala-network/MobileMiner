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
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import android.widget.SeekBar;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

    private Toolbar toolbar;

    private TextView tvHashrate, tvStatus, tvNbCores, tvCPUTemperature, tvCPUTemperatureUnit, tvBatteryTemperature, tvBatteryTemperatureUnit, tvAcceptedShares, tvDifficulty, tvConnection, tvLog, tvLog2, tvStatusProgess;
    private TubeSpeedometer meterCores, meterHashrate, meterHashrate_avg, meterHashrate_max;
    private SeekBar sbCores = null;
    private SwipeRefreshLayout pullToRefreshHr;
    private NestedScrollView svLog, svLog2;

    private LinearLayout llMain, llLog, llHashrate, llStatus;
    private RelativeLayout rlWarningCPUTemperature, rlWarningBatteryTemperature;

    private ProgressBar pbPayout;
    private boolean payoutEnabled;
    protected IProviderListener payoutListener;

    private Timer timerHashrate = null;
    private TimerTask timerTaskHashrate = null;
    private ProgressBar pbStatus;

    private boolean validArchitecture = true;

    private MiningService.MiningServiceBinder binder;
    private boolean bPayoutDataReceived = false;
    private float fMinPoolPayout = -1.0f;

    private boolean bIgnoreCPUCoresEvent = false;
    private boolean bIsRestartEvent = false;
    private boolean bIsRestartDialogShown = false;
    private boolean bForceMiningOnPause = false;

    private boolean bValidCPUTemperatureSensor = true;
    private boolean bValidBatteryTemperatureSensor = true;
    private boolean bIsCelsius = true;
    private boolean bForceMiningNoTempSensor = false;

    // Graphics
    private LineChart chartHashrate;
    private BarChart chartTemperature;

    ArrayList<Entry> lValuesHr = new ArrayList<>();
    int xHr = 0;

    ArrayList<BarEntry> lValuesTempBattery = new ArrayList<>();
    ArrayList<BarEntry> lValuesTempCPU = new ArrayList<>();
    int xTemp = 0;

    // Settings
    private boolean bDisableTemperatureControl = false;
    private boolean bDisableAmayc = false;
    private Integer nMaxCPUTemp = Config.DefaultMaxCPUTemp;
    private Integer nMaxBatteryTemp = Config.DefaultMaxBatteryTemp;
    private Integer nSafeCPUTemp = 0;
    private Integer nSafeBatteryTemp = 0;
    private Integer nCores = 0;

    private Integer nLastShareCount = 0;

    private Integer nNbMaxCores = 0;

    private float fSumHr = 0.0f;
    private Integer nHrCount = 0;
    private float fMaxHr = 0.0f;

    // Temperature Control
    private Timer timerTemperatures = null;
    private TimerTask timerTaskTemperatures = null;
    private final List<String> listCPUTemp = new ArrayList<>();
    private final List<String> listBatteryTemp = new ArrayList<>();
    private boolean isCharging = false;
    private final int MAX_CHART_VALUES = 250;

    public static Context contextOfApplication;

    private boolean isServerConnectionBound = false;
    private boolean isBatteryReceiverRegistered = false;

    private PowerManager.WakeLock wl;

    public static Context getContextOfApplication() {
        return contextOfApplication;
    }

    private Button btnStart;

    private static int m_nLastCurrentState = Config.STATE_STOPPED;
    private static int m_nCurrentState = Config.STATE_STOPPED;
    public int getCurrentState() { return m_nCurrentState; }

    private static NotificationManager notificationManager = null;
    private NotificationCompat.Builder notificationBuilder = null;

    BottomNavigationView navigationView = null;

    private File imagePath = null;

    private boolean isFromLogView = false;

    public static boolean isDeviceMiningBackground() {
        return (m_nCurrentState == Config.STATE_CALCULATING || m_nCurrentState == Config.STATE_MINING || m_nCurrentState == Config.STATE_COOLING);
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

        if(!isBatteryReceiverRegistered) {
            registerReceiver(batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            isBatteryReceiverRegistered = true;
        }

        if(!isServerConnectionBound) {
            Intent intent = new Intent(this, MiningService.class);
            bindService(intent, serverConnection, BIND_AUTO_CREATE);
            startService(intent);
            isServerConnectionBound = true;
        }

        setContentView(R.layout.activity_main);

        navigationView = findViewById(R.id.main_navigation);
        navigationView.getMenu().getItem(0).setChecked(true);
        navigationView.setOnNavigationItemSelectedListener(this);

        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setOnButtonListener(new Toolbar.OnButtonListener() {
            @Override
            public void onButtonMain(int type) {
                switch (type) {
                    case Toolbar.BUTTON_MAIN_CLOSE: {
                        backHomeMenu();
                        break;
                    }
                    default: {
                        // Do nothing
                    }
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
                        showCores();

                        break;
                    }
                    case Toolbar.BUTTON_OPTIONS_STATS: {
                        PoolItem pm = ProviderManager.getSelectedPool();
                        String statsUrlWallet = pm.getStatsURL() + "?wallet=" + Config.read("address");
                        Uri uri = Uri.parse(statsUrlWallet);
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));

                        break;
                    }
                    case Toolbar.BUTTON_OPTIONS_COPY: {
                        Utils.copyToClipboard("Mining Log", tvLog.getText().toString());
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

        // Leave this here to avoid a crash when app is restored from idle state
        SharedPreferences preferences = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        Config.initialize(preferences);

        // Open Settings the first time the app is launched
        /*String minersaddress =  Config.read("address");

        if (minersaddress.equals("") || minersaddress.isEmpty()) {
            navigationView.getMenu().getItem(2).setChecked(true);

            SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("settings_fragment");
            if(fragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment,"settings_fragment").commit();
            }
            else {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment(),"settings_fragment").commit();
            }
        }*/

        pullToRefreshHr = findViewById(R.id.pullToRefreshHr);
        pullToRefreshHr.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(navigationView.getMenu().findItem(R.id.menu_home).isChecked()) {
                    showCores();
                } else if (navigationView.getMenu().findItem(R.id.menu_stats).isChecked()){
                    StatsFragment.updateStatsListener();
                }

                pullToRefreshHr.setRefreshing(false);
            }
        });

        // Layouts
        llMain = findViewById(R.id.layout_main);
        llLog = findViewById(R.id.layout_mining_log);
        llHashrate = findViewById(R.id.layout_hashrate);
        llStatus = findViewById(R.id.layout_status);

        LinearLayout llViewLog = findViewById(R.id.llViewLog);
        llViewLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDetailedLog();
                navigationView.setVisibility(View.GONE);
            }
        });

        // Controls

        payoutEnabled = true;
        pbPayout = findViewById(R.id.progresspayout);
        pbStatus = findViewById(R.id.progress_status);

        pbStatus.setMax(MAX_HASHRATE_TIMER * 2);
        pbStatus.setProgress(0);

        tvStatusProgess = findViewById(R.id.hr_progress);

        // Log
        tvLog = findViewById(R.id.output);
        svLog = findViewById(R.id.svLog);

        tvLog2 = findViewById(R.id.output2);
        svLog2 = findViewById(R.id.svLog2);

        // CPU Cores

        nNbMaxCores = Runtime.getRuntime().availableProcessors();
        String core_config = Config.read("cores");

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

        meterCores = findViewById(R.id.meter_cores);
        meterCores.makeSections(1, getResources().getColor(R.color.c_yellow), Section.Style.SQUARE);
        meterCores.setMaxSpeed(nNbMaxCores);
        meterCores.speedTo(0, 0);

        tvNbCores = findViewById(R.id.nbcores);

        // Hashrate
        meterHashrate = findViewById(R.id.meter_hashrate);
        meterHashrate.makeSections(1, getResources().getColor(R.color.c_blue), Section.Style.SQUARE);

        LineIndicator indicator_speed = new LineIndicator(contextOfApplication, 0.15f);
        indicator_speed.setColor(getResources().getColor(R.color.c_white));
        indicator_speed.setWidth(14.0f);
        meterHashrate.setIndicator(indicator_speed);

        // Average Meter
        meterHashrate_avg = findViewById(R.id.meter_hashrate_avg);
        meterHashrate_avg.makeSections(1, getResources().getColor(android.R.color.transparent), Section.Style.SQUARE);

        SimpleTriangleIndicator indicator_avg = new SimpleTriangleIndicator(contextOfApplication);
        indicator_avg.setWidth(40.0f);
        indicator_avg.setColor(getResources().getColor(R.color.txt_main));
        meterHashrate_avg.setIndicator(indicator_avg);

        // Max Meter
        meterHashrate_max = findViewById(R.id.meter_hashrate_max);
        meterHashrate_max.makeSections(1, getResources().getColor(android.R.color.transparent), Section.Style.SQUARE);

        SimpleTriangleIndicator indicator_max = new SimpleTriangleIndicator(contextOfApplication);
        indicator_max.setWidth(40.0f);
        indicator_max.setColor(getResources().getColor(R.color.c_orange));
        meterHashrate_max.setIndicator(indicator_max);

        tvHashrate = findViewById(R.id.hashrate);
        tvStatus = findViewById(R.id.miner_status);

        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());

        tvCPUTemperature = findViewById(R.id.cputemp);
        tvCPUTemperatureUnit = findViewById(R.id.cputempunit);
        rlWarningCPUTemperature = findViewById(R.id.rlWarningCPUTemp);
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

        tvBatteryTemperature = findViewById(R.id.batterytemp);
        tvBatteryTemperatureUnit = findViewById(R.id.batterytempunit);
        rlWarningBatteryTemperature = findViewById(R.id.rlWarningBatteryTemp);
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

        tvAcceptedShares = findViewById(R.id.acceptedshare);
        tvDifficulty  = findViewById(R.id.difficulty);
        tvConnection  = findViewById(R.id.connection);

        btnStart = findViewById(R.id.start);
        //enableStartBtn(false);

        // Cores seekbar
        sbCores = findViewById(R.id.seekbarcores);
        sbCores.setMax(nNbMaxCores);
        sbCores.setProgress(nCores);

        sbCores.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (bIgnoreCPUCoresEvent)
                    return;

                if(isDeviceMining()) {
                    if(bIsRestartDialogShown)
                        return;

                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(contextOfApplication, R.style.MaterialAlertDialogCustom);
                    builder.setTitle(getString(R.string.stopmining))
                            .setMessage(getString(R.string.newparametersapplied))
                            .setCancelable(true)
                            .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    nCores = sbCores.getProgress();
                                    Config.write("cores", Integer.toString(nCores));

                                    bIsRestartEvent = true;

                                    MainActivity.this.stopMining(); // Stop mining

                                    // Start miner with small delay
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            MainActivity.this.startMining(); // Start mining
                                        }
                                    }, 1000);

                                    updateCores();

                                    bIsRestartDialogShown = false;
                                }
                            })
                            .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            bIgnoreCPUCoresEvent = true;
                                            sbCores.setProgress(nCores);
                                            bIgnoreCPUCoresEvent = false;
                                        }
                                    });

                                    bIsRestartDialogShown = false;
                                }
                            })
                            .show();

                    bIsRestartDialogShown = true;
                }
                else {
                    nCores = sbCores.getProgress();
                    Config.write("cores", Integer.toString(nCores));
                    updateCores();
                }
            }
        });

        if (!Arrays.asList(Config.SUPPORTED_ARCHITECTURES).contains(Tools.getABI())) {
            String sArchError = "Your architecture is not supported: " + Tools.getABI();
            appendLogOutputFormattedText(sArchError);
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

        startTimerTemperatures();

        createNotificationManager();

        updateStartButton();
        resetAvgMaxHashrate();

        chartHashrate = findViewById(R.id.chart_hashrate);
        chartTemperature = findViewById(R.id.chart_temprature);

        updateUI();

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

        super.onDestroy();
    }

    private void backHomeMenu() {
        isFromLogView = false;

        showMenuHome();
        navigationView.setVisibility(View.VISIBLE);
        toolbar.setButtonMain(Toolbar.BUTTON_MAIN_LOGO);
    }

    private void initChartHashrate() {
        chartHashrate.getDescription().setEnabled(false);
        chartHashrate.setTouchEnabled(true);
        chartHashrate.setDragEnabled(true);
        chartHashrate.setScaleEnabled(true);
        chartHashrate.setDrawGridBackground(false);
        chartHashrate.setHighlightPerDragEnabled(true);
        chartHashrate.setPinchZoom(true);
        chartHashrate.animateX(1500);
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

    private void setHashrateChartLimits() {
        float avgHr = meterHashrate_avg.getSpeed();

        LimitLine ll1 = new LimitLine(avgHr);
        ll1.setLineWidth(1f);
        ll1.enableDashedLine(10f, 10f, 0f);
        ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll1.setLineColor(getResources().getColor(R.color.txt_main));

        float maxHr = meterHashrate_max.getSpeed();
        LimitLine ll2 = new LimitLine(maxHr);
        ll2.setLineWidth(1f);
        ll2.enableDashedLine(10f, 10f, 0f);
        ll2.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll2.setTextSize(0f);
        ll2.setLineColor(getResources().getColor(R.color.c_orange));

        YAxis leftAxis = chartHashrate.getAxisLeft();
        leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
        leftAxis.addLimitLine(ll1);
        leftAxis.addLimitLine(ll2);
    }

    private void initChartTemperature() {
        chartTemperature.getDescription().setEnabled(false);
        chartTemperature.setTouchEnabled(true);
        chartTemperature.setDragEnabled(true);
        chartTemperature.setScaleEnabled(true);
        chartTemperature.setDrawGridBackground(false);
        chartTemperature.setHighlightPerDragEnabled(true);
        chartTemperature.setPinchZoom(false);
        chartTemperature.animateX(1500);
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

    private void resetCharts() {
        LineData dataH = chartHashrate.getData();
        if(dataH != null && dataH.getDataSetCount() > 0) {
            lValuesHr.clear();

            dataH.clearValues();
            dataH.notifyDataChanged();
            chartHashrate.notifyDataSetChanged();

            chartHashrate.clear();
        }

        BarData dataT = chartTemperature.getData();
        if(dataT != null && dataT.getDataSetCount() > 0) {
            lValuesTempBattery.clear();
            lValuesTempCPU.clear();

            dataT.clearValues();
            dataT.notifyDataChanged();
            chartTemperature.notifyDataSetChanged();

            chartTemperature.clear();
        }
    }

    private void addHashrateValue(float hr) {
        lValuesHr.add(new Entry(xHr, hr));
        xHr++;

        // Only keep 100 last values to avoid overflow
        if(lValuesHr.size() > MAX_CHART_VALUES)
            lValuesHr.remove(0);

        LineDataSet set1;
        LineData data = chartHashrate.getData();
        YAxis leftAxis = chartHashrate.getAxisLeft();

        if (data != null && data.getDataSetCount() > 0) {
            set1 = (LineDataSet) data.getDataSetByIndex(0);
            set1.setValues(lValuesHr);
            data.notifyDataChanged();
            chartHashrate.notifyDataSetChanged();
        } else {
            // Set Min/Max YAxis

            leftAxis.setAxisMaximum(hr * 1.25f);
            leftAxis.setAxisMinimum(hr * 0.75f);

            // create a dataset and give it a type
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

            // create a data object with the data sets
            data = new LineData(set1);
            data.setDrawValues(false);

            // set data
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

        chartHashrate.setMaxVisibleValueCount(10);
        chartHashrate.setVisibleXRangeMaximum(10);
        chartHashrate.moveViewToX(data.getEntryCount());
        chartHashrate.invalidate();
    }

    private void setTemperaturesChartLimits() {
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

    private void addTemperaturesValue(float cpu, float battery) {
        if(!isDeviceMining())
            return;

        float cpu_temp = bIsCelsius ? cpu : Utils.convertCelciusToFahrenheit(Math.round(cpu));
        float battery_temp = bIsCelsius ? battery : Utils.convertCelciusToFahrenheit(Math.round(battery));

        lValuesTempCPU.add(new BarEntry(xTemp, cpu_temp));
        lValuesTempBattery.add(new BarEntry(xTemp, battery_temp));
        xTemp++;

        // Only keep 100 last values to avoid overflow
        if(lValuesTempCPU.size() > MAX_CHART_VALUES)
            lValuesTempCPU.remove(0);

        if(lValuesTempBattery.size() > MAX_CHART_VALUES)
            lValuesTempBattery.remove(0);

        BarDataSet set1, set2;
        BarData data = chartTemperature.getData();
        if (data != null && data.getDataSetCount() > 0) {
            set1 = (BarDataSet) data.getDataSetByIndex(0);
            set2 = (BarDataSet) data.getDataSetByIndex(1);
            set1.setValues(lValuesTempCPU);
            set2.setValues(lValuesTempBattery);
            data.notifyDataChanged();
            chartTemperature.notifyDataSetChanged();
        } else {
            // create 4 DataSets
            set1 = new BarDataSet(lValuesTempCPU, "CPU");
            set1.setColor(getResources().getColor(R.color.c_blue));
            set1.setAxisDependency(YAxis.AxisDependency.LEFT);

            set2 = new BarDataSet(lValuesTempBattery, "Battery");
            set2.setColor(getResources().getColor(R.color.c_grey));
            set2.setAxisDependency(YAxis.AxisDependency.LEFT);

            data = new BarData(set1, set2);

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

        // restrict the x-axis range
        //chartTemperature.getXAxis().setAxisMinimum(0);

        // barData.getGroupWith(...) is a helper that calculates the width each group needs based on the provided parameters
        chartTemperature.getXAxis().setAxisMaximum(0 + chartTemperature.getBarData().getGroupWidth(groupSpace, barSpace) * set1.getEntryCount());
        chartTemperature.groupBars(0, groupSpace, barSpace);

        chartTemperature.fitScreen();

        chartTemperature.setMaxVisibleValueCount(20);
        chartTemperature.setVisibleXRangeMaximum(20);
        chartTemperature.moveViewToX(data.getEntryCount());

        chartTemperature.invalidate();
    }

    public void showCores() {
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
        timerTemperatures.scheduleAtFixedRate(timerTaskTemperatures, 0, 10000);
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

        if (!Config.read("mininggoal").isEmpty()) {
            edMiningGoal.setText(Config.read("mininggoal"));
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
                            Config.write("mininggoal", mininggoal);
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
            if(Config.read("mininggoal").equals("")) {
                fMinPayout = Utils.convertStringToFloat(d.pool.minPayout);
            }
            else
                fMinPayout = Utils.convertStringToFloat(Config.read("mininggoal").trim());

            TextView tvPayoutGoal = findViewById(R.id.tvPayoutGoal);
            tvPayoutGoal.setText(String.valueOf(Math.round(fMinPayout)));

            TextView tvPercentagePayout = findViewById(R.id.percentage);
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
        //TextView tvPayoutWidgetTitle = findViewById(R.id.payoutgoal);
        TextView tvMessage = findViewById(R.id.payoutmessage);

        if (enable) {
            /*if(tvPayoutWidgetTitle.getVisibility() == View.VISIBLE)
                return;

            tvPayoutWidgetTitle.setVisibility(View.VISIBLE);*/

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
            //tvPayoutWidgetTitle.setVisibility(View.INVISIBLE);

            TextView tvBalance = findViewById(R.id.balance_payout);
            tvBalance.setVisibility(View.INVISIBLE);

            TextView tvXLAUnit = findViewById(R.id.xlaunit);
            tvXLAUnit.setVisibility(View.INVISIBLE);

            TextView tvPercentage = findViewById(R.id.percentage);
            tvPercentage.setVisibility(View.INVISIBLE);

            TextView tvPercentageUnit = findViewById(R.id.percentageunit);
            tvPercentageUnit.setVisibility(View.INVISIBLE);

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

        if(doesPoolSupportAPI()) {
            tvPayout.setVisibility(View.VISIBLE);
            llPayoutWidget.setVisibility(View.VISIBLE);
        }
        else {
            tvPayout.setVisibility(View.GONE);
            llPayoutWidget.setVisibility(View.GONE);

            return;
        }

        if (Config.read("address").equals("")) {
            enablePayoutWidget(false, "");
            payoutEnabled = false;
            return;
        }

        PoolItem pi = ProviderManager.getSelectedPool();

        if (!Config.read("init").equals("1") || pi == null) {
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

        if(!Config.read("init").equals("1")) {
            Utils.showToast(contextOfApplication, getString(R.string.save_settings_first), Toast.LENGTH_SHORT);
            return false;
        }

        String walletaddress = Config.read("address");
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
        updateCores();
        setTemperaturesChartLimits();
    }

    private String getWorkerName() {
        String workerName = Config.read("workername");

        if(workerName.isEmpty())
            return "Your device";

        return workerName;
    }

    public void updateStartButton() {
        /*if (isValidConfig()) {
            enableStartBtn(true);
        }
        else {
            enableStartBtn(false);
        }*/
    }

    private void updateCores() {
        String sCores = nCores + "/" + nNbMaxCores;
        tvNbCores.setText(sCores);

        meterCores.speedTo(nCores, 0);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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

        llMain.setVisibility(View.VISIBLE);
        llLog.setVisibility(View.GONE);

        toolbar.setButtonOptions(Toolbar.BUTTON_OPTIONS_SHARE);
        toolbar.setTitle(getWorkerName(), true);

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

        llMain.setVisibility(View.GONE);
        llLog.setVisibility(View.VISIBLE);

        toolbar.setButtonMain(Toolbar.BUTTON_MAIN_CLOSE);
        toolbar.setButtonOptions(Toolbar.BUTTON_OPTIONS_COPY);
        toolbar.setTitle(getResources().getString(R.string.mininglog), true);

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
        bDisableTemperatureControl = Config.read("disableamayc", "0").equals("1");
        nMaxCPUTemp = Integer.parseInt(Config.read("maxcputemp", Integer.toString(Config.DefaultMaxCPUTemp)).trim());
        nMaxBatteryTemp = Integer.parseInt(Config.read("maxbatterytemp", Integer.toString(Config.DefaultMaxBatteryTemp)).trim());
        int nCooldownThreshold = Integer.parseInt(Config.read("cooldownthreshold", Integer.toString(Config.DefaultCooldownTheshold)).trim());

        nSafeCPUTemp = nMaxCPUTemp - Math.round((float)nMaxCPUTemp * (float)nCooldownThreshold / 100.0f);
        nSafeBatteryTemp = nMaxBatteryTemp - Math.round((float)nMaxBatteryTemp * (float)nCooldownThreshold / 100.0f);

        nCores = Integer.parseInt(Config.read("cores", "0"));

        bIsCelsius = Config.read(Config.CONFIG_TEMPERATURE_UNIT, "C").equals("C");
        tvCPUTemperatureUnit.setText(bIsCelsius ? getString(R.string.celsius) : getString(R.string.fahrenheit));
        tvBatteryTemperatureUnit.setText(bIsCelsius ? getString(R.string.celsius) : getString(R.string.fahrenheit));

        initChartHashrate();
        initChartTemperature();
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
                        return;
                    }
                }).show();
    }

    private void startMining() {
        if (binder == null) return;

        if (!Config.read("init").equals("1")) {
            setStatusText(getString(R.string.save_settings_first));
            return;
        }

        String password = Config.read("workername");
        String address = Config.read("address");

        if (!Utils.verifyAddress(address)) {
            setStatusText(getString(R.string.invalid_address));
            return;
        }

        if((!bValidCPUTemperatureSensor || !bValidBatteryTemperatureSensor) && !bForceMiningNoTempSensor && !Config.read(Config.CONFIG_TEMPERATURE_SENSOR_SHOW_WARNING).equals("0")) {
            showDisclaimerTemperatureSensors();
            return;
        }

        if (Config.read("pauseonbattery").equals("1") && !isCharging && !bForceMiningOnPause) {
            askToForceMining();
            return;
        }

        bForceMiningOnPause = false;
        bForceMiningNoTempSensor = false;

        String username = address + Config.read("usernameparameters");

        resetOptions();

        resetCharts();

        loadSettings();

        MiningService s = binder.getService();
        MiningService.MiningConfig cfg = s.newConfig(
                username,
                password,
                nCores,
                1,
                1
        );

        s.startMining(cfg);

        showNotification();

        setMinerStatus(Config.STATE_MINING);

        updateUI();
    }

    private void askToForceMining() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(contextOfApplication, R.style.MaterialAlertDialogCustom);
        builder.setTitle(getString(R.string.confirmstartmining))
                .setMessage(getString(R.string.deviceonbattery))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        bForceMiningOnPause = true;

                        if(isDevicePaused()) {
                            clearMinerLog = false;
                            resumeMiner();
                        }
                        else
                            startMining();
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        bForceMiningOnPause = false;
                    }
                })
                .show();
    }

    private void resetOptions() {
        bDisableAmayc = false;
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

        setMinerStatus(Config.STATE_STOPPED);

        binder.getService().stopMining();

        resetOptions();

        resetAvgMaxHashrate();

        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();

        ProviderManager.request.setListener(payoutListener).start();

        if(!isBatteryReceiverRegistered) {
            registerReceiver(batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            isBatteryReceiverRegistered = true;
        }

        if(!isServerConnectionBound) {
            Intent intent = new Intent(this, MiningService.class);
            bindService(intent, serverConnection, BIND_AUTO_CREATE);
            startService(intent);
            isServerConnectionBound = true;
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
            clearMinerLog = true;
            MainActivity.this.stopMining();
        } else {
            MainActivity.this.startMining();
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

    private void enableSliderCores(boolean enable) {
        if(bIsRestartEvent)
            return;

        Rect bounds = sbCores.getProgressDrawable().getBounds();

        if(enable) {
            sbCores.setProgressDrawable(getResources().getDrawable(R.drawable.seekbar_ruler_yellow));
            sbCores.getThumb().setColorFilter(getResources().getColor(R.color.c_white), PorterDuff.Mode.SRC_IN);
        }
        else {
            sbCores.setProgressDrawable(getResources().getDrawable(R.drawable.seekbar_ruler_inactive));
            sbCores.getThumb().setColorFilter(getResources().getColor(R.color.c_light_grey), PorterDuff.Mode.SRC_IN);
        }

        sbCores.getProgressDrawable().setBounds(bounds);
    }

    private void setMinerStatus(Integer status) {
        pbStatus.setScaleY(1f);

        if(status == Config.STATE_STOPPED) {
            llStatus.setVisibility(View.GONE);
            llHashrate.setVisibility(View.VISIBLE);

            tvHashrate.setText("0");
            tvHashrate.setTextSize(55);
            tvHashrate.setTextColor(getResources().getColor(R.color.txt_inactive));

            View v = findViewById(R.id.main_navigation);
            v.setKeepScreenOn(false);

            meterHashrate.speedTo(0);
            meterHashrate_avg.setVisibility(View.GONE);
            meterHashrate_max.setVisibility(View.GONE);

            stopTimerStatusHashrate();
            resetHashrateTicks();
            enableSliderCores(true);
        }
        else if(status ==Config.STATE_MINING) {
            if(tvHashrate.getText().equals("0")) {
                setMinerStatus(Config.STATE_CALCULATING);
            } else {
                llStatus.setVisibility(View.GONE);
                llHashrate.setVisibility(View.VISIBLE);

                tvHashrate.setTextColor(getResources().getColor(R.color.c_white));

                stopTimerStatusHashrate();
            }

            if (Config.read("keepscreenonwhenmining").equals("1")) {
                View v = findViewById(R.id.main_navigation);
                v.setKeepScreenOn(true);
            }

            enableSliderCores(false);
        }
        else {
            llStatus.setVisibility(View.VISIBLE);
            llHashrate.setVisibility(View.GONE);

            meterHashrate.speedTo(0);

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

    public void startTimerStatusHashrate() {
        if(timerHashrate != null) {
            return;
        }

        timerTaskHashrate = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        incrementProgressHashrate();
                    }
                });
            }
        };

        timerHashrate = new Timer();
        timerHashrate.scheduleAtFixedRate(timerTaskHashrate, 0, 500);
    }

    public void stopTimerStatusHashrate() {
        if(timerHashrate != null) {
            timerHashrate.cancel();
            timerHashrate = null;
            timerTaskHashrate = null;

            pbStatus.setProgress(0);

            tvStatusProgess.setVisibility(View.VISIBLE);
            tvStatusProgess.setText("0%");
        }
    }

    private void resetHashrateTicks() {
        SpeedView meterTicks = findViewById(R.id.meter_hashrate_ticks);
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
        SpeedView meterTicks = findViewById(R.id.meter_hashrate_ticks);
        float fCurrentMax = meterTicks.getMaxSpeed();

        if((meterTicks.getTickNumber() == 0 || (fCurrentMax > 0 && fHr >= fCurrentMax * 0.9)) && fMaxHr > 0) {
            float hrMax = getMaxHr(fMaxHr);

            // This is not normal, we need to recompute it
            if(fHr > hrMax) {
                hrMax = getMaxHr(fHr);
            }

            meterTicks.setMaxSpeed(hrMax);
            meterTicks.setTickNumber(10);
            meterTicks.setTextColor(getResources().getColor(R.color.txt_main));

            meterHashrate.setMaxSpeed(hrMax);
            meterHashrate.setWithTremble(!(hrMax < 15));
            //meterHashrate.setTrembleDegree(0.1f);

            meterHashrate_avg.setMaxSpeed(hrMax);
            meterHashrate_max.setMaxSpeed(hrMax);
        }
    }

    private void incrementProgressHashrate() {
        pbStatus.setProgress(pbStatus.getProgress() + 1);

        String sProgessPercent = String.valueOf(Math.round((float)pbStatus.getProgress() / (float)pbStatus.getMax() *100.0f));
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
        meterHashrate.speedTo(Math.round(fSpeed));

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

            if (meterHashrate_avg.getVisibility() == View.GONE)
                meterHashrate_avg.setVisibility(View.VISIBLE);
            meterHashrate_avg.setSpeedAt(fAvgHr);
        }
        else {
            tvAvgHr.setText(String.format(Locale.getDefault(), "%.1f", 0.0f));
            meterHashrate_avg.setVisibility(View.GONE);
        }

        // Max Hashrate
        if(fMax > 0.0f) {
            if(fMax > fMaxHr)
                fMaxHr = fMax;

            tvMaxHr.setText(String.format(Locale.getDefault(), "%.1f", fMaxHr));

            if(meterHashrate_max.getVisibility() == View.GONE)
                meterHashrate_max.setVisibility(View.VISIBLE);
            meterHashrate_max.setSpeedAt(fMaxHr);
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

        if(isDeviceCooling() && text.contains("paused, press")) {
            text = text.replace("paused, press", getResources().getString(R.string.miningpaused));
            text = text.replace("to resume", "");
            text = text.replace("r ", "");
        }

        if(m_nLastCurrentState == Config.STATE_COOLING && text.contains("resumed")) {
            text = text.replace("resumed", getResources().getString(R.string.resumedmining));
        }

        if (text.contains("threads:")) {
            text = text.replace("threads:", "* THREADS ");
        }

        if (text.contains("COMMANDS")) {
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

        formatText = "AMYAC error";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_orange)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "AMYAC response";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_orange)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.amaycerror);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_orange)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        llNoActivity.setVisibility(tvLog.getText().toString().isEmpty() ? View.VISIBLE : View.GONE);

        if(svLog != null) {
            svLog.post(new Runnable() {
                @Override
                public void run() {
                    svLog.fullScroll(View.FOCUS_DOWN);
                }
            });
        }

        if(tvLog2 != null) {
            TextView tvNoActivity = findViewById(R.id.tvNoActivity);
            tvNoActivity.setVisibility(tvLog2.getText().toString().isEmpty() ? View.VISIBLE : View.GONE);

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
            if (tvLog.getText().length() > Config.logMaxLength ){
                String outputLog = binder.getService().getOutput();
                tvLog.setText(formatLogOutputText(outputLog));
                tvLog2.setText(formatLogOutputText(outputLog));
            }
        }

        if(!line.equals("")) {
            String outputLog = line + System.getProperty("line.separator");
            tvLog.append(formatLogOutputText(outputLog));
            tvLog2.append(formatLogOutputText(outputLog));
        }

        refreshLogOutputView();
    }

    private ServiceConnection serverConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            binder = (MiningService.MiningServiceBinder) iBinder;
            if (validArchitecture) {
                btnStart.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if(!validateSettings())
                            return;

                        if (isDevicePaused()) {
                            if (Config.read("pauseonbattery").equals("1") && !isCharging && !bForceMiningOnPause) {
                                askToForceMining();
                                return;
                            }

                            clearMinerLog = false;
                            resumeMiner();
                        }
                        else {
                            toggleMiningState();
                        }

                        updateMiningButtonState();
                    }
                });

                updateMiningButtonState();
                //setMiningButtonState(binder.getService().getMiningServiceState());

                binder.getService().setMiningServiceStateListener(new MiningService.MiningServiceStateListener() {
                    @Override
                    public void onStateChange(Boolean state) {
                        Log.i(LOG_TAG, "onMiningStateChange: " + state);
                        runOnUiThread(() -> {
                            updateMiningButtonState();
                            if (state) {
                                if (clearMinerLog) {
                                    tvLog.setText("");
                                    tvLog2.setText("");
                                    tvAcceptedShares.setText("0");
                                    tvAcceptedShares.setTextColor(getResources().getColor(R.color.txt_inactive));

                                    tvDifficulty.setText("0");
                                    tvDifficulty.setTextColor(getResources().getColor(R.color.txt_inactive));

                                    tvConnection.setText("0");
                                    tvConnection.setTextColor(getResources().getColor(R.color.txt_inactive));

                                    updateHashrate(-1.0f, -1.0f);
                                }
                                clearMinerLog = true;
                                setStatusText("Miner Started.");
                            } else {

                                setStatusText("Miner Stopped.");
                            }

                            bIsRestartEvent = false;
                        });
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onStatusChange(String status, float speed, float max, Integer accepted, Integer difficuly, Integer connection) {
                        runOnUiThread(() -> {
                            appendLogOutputText(status);

                            String sAccepted = Integer.toString(accepted);
                            if(!tvAcceptedShares.getText().equals(sAccepted)) {
                                tvAcceptedShares.setText(sAccepted);
                                tvAcceptedShares.startAnimation(getBlinkAnimation());
                            }

                            tvDifficulty.setText(NumberFormat.getNumberInstance(Locale.getDefault()).format(difficuly));
                            tvConnection.setText(Integer.toString(connection));

                            if(!nLastShareCount.equals(accepted)) {
                                nLastShareCount = accepted;
                            }

                            if(accepted == 1) {
                                tvAcceptedShares.setTextColor(getResources().getColor(R.color.c_white));
                                tvDifficulty.setTextColor(getResources().getColor(R.color.c_white));
                                tvConnection.setTextColor(getResources().getColor(R.color.c_white));
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
            //enableStartBtn(false);
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
                tvCPUTemperature.setTextColor(getResources().getColor(R.color.txt_main));
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
                tvBatteryTemperature.setTextColor(getResources().getColor(R.color.txt_main));
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

        if(bDisableAmayc)
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
                                        enableCooling(true);
                                    }
                                }
                            }
                        } else if (uri.contains("check1")) {
                            if(obj.has("predicted_next")) {
                                double predictedNext = obj.getDouble("predicted_next");

                                if (!listCPUTemp.isEmpty()) {
                                    int cpupred = (int)Math.round(predictedNext);
                                    if (cpupred >= nMaxCPUTemp) {
                                        enableCooling(true);
                                    }
                                } else if (!listBatteryTemp.isEmpty()) {
                                    int batterypred = (int)Math.round(predictedNext);
                                    if (batterypred >= nMaxBatteryTemp) {
                                        enableCooling(true);
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
            String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
            JSONObject data = new JSONObject(responseBody);
            JSONArray errors = data.getJSONArray("errors");
            JSONObject jsonMessage = errors.getJSONObject(0);

            message = "AMYAC error: " + jsonMessage.getString("message");
        } catch (JSONException e) {
            message = "AMYAC error JSONException: " + e.getMessage();
        } finally {
            disableAmaycOnError(message);
        }
    }

    private void disableAmaycOnError(String error) {
        bDisableAmayc = true;
        appendLogOutputFormattedText(error);
        appendLogOutputFormattedText(getResources().getString(R.string.statictempcontrol));
    }

    private void appendLogOutputFormattedText(String text) {
        appendLogOutputText("[" + Utils.getDateTime() + "] " + text);
    }

    private void enableCooling(boolean enable) {
        if(enable) {
            setMinerStatus(Config.STATE_COOLING);

            pauseMiner();

            appendLogOutputFormattedText(getResources().getString(R.string.maxtemperaturereached));
        }
        else {
            if (Config.read("pauseonbattery").equals("1") && !isCharging) {
                setStatusText(getResources().getString(R.string.pauseonmining));
                return;
            }

            resumeMiner();

            listCPUTemp.clear();
            listBatteryTemp.clear();
        }
    }

    private void enableStartBtn(boolean enabled) {
        Drawable buttonDrawable = btnStart.getBackground();
        buttonDrawable = DrawableCompat.wrap(buttonDrawable);

        if(enabled) {
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_green));
            btnStart.setBackground(buttonDrawable);
            btnStart.setTextColor(getResources().getColor(R.color.c_white));
        } else {
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.c_inactive));
            btnStart.setBackground(buttonDrawable);
            btnStart.setTextColor(getResources().getColor(R.color.c_inactive));
        }

        btnStart.setEnabled(enabled);
    }

    private void pauseMiner() {
        if (!isDevicePaused()) {
            if(!isDeviceCooling()) {
                setMinerStatus(Config.STATE_PAUSED);

                //enableStartBtn(true);
                updateMiningButtonState();
            }

            if (binder != null) {
                binder.getService().sendInput("p");
            }
        }
    }

    private void resumeMiner() {
        if (isDevicePaused() || isDeviceCooling()) {
            setMinerStatus(Config.STATE_MINING);

            if (binder != null) {
                binder.getService().sendInput("r");
            }

            updateMiningButtonState();
            bForceMiningOnPause = false;
        }
    }

    private void sendInput(String s) {
        if (s.equals("p")) {
            pauseMiner();
        }
        else if (s.equals("r")) {
            if(isDeviceCooling()) {
                setStatusText(getResources().getString(R.string.amaycpaused));
                return;
            }

            resumeMiner();
        }
        else {
            if (binder != null) {
                binder.getService().sendInput(s);
            }
        }
    }

    public static final String OPEN_ACTION = "OPEN_ACTION";
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

    private void showNotification() {
        createNotificationManager();

        NotificationsReceiver.activity = this;

        // Open intent
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setAction(OPEN_ACTION);
        PendingIntent pendingIntentOpen = PendingIntent.getActivity(contextOfApplication, 1, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Stop intent
        Intent stopIntent = new Intent(this, NotificationsReceiver.class);
        stopIntent.setAction(STOP_ACTION);
        PendingIntent pendingIntentStop = PendingIntent.getBroadcast(contextOfApplication, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentTitle(getResources().getString(R.string.devicemining));
        notificationBuilder.setContentIntent(pendingIntentOpen);
        notificationBuilder.addAction(R.mipmap.ic_open_app,"Open", pendingIntentOpen);
        notificationBuilder.addAction(R.mipmap.ic_stop_miner,"Stop", pendingIntentStop);
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
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
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
    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
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

            setStatusText((isCharging ? "Device charging." : "Device on battery power."));

            if (Config.read("pauseonbattery").equals("0")) {
                clearMinerLog = true;
            } else {
                boolean state = false;
                if (binder != null) {
                    state = binder.getService().getMiningServiceState();
                }

                if (isCharging) {
                    resumeMiner();
                } else if (state) {
                    pauseMiner();
                }
            }
        }
    };
}
