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
// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package shmutalov.verusminer9000;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
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
import android.text.Layout;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Spannable;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import shmutalov.verusminer9000.api.IProviderListener;
import shmutalov.verusminer9000.api.PoolItem;
import shmutalov.verusminer9000.api.ProviderData;
import shmutalov.verusminer9000.api.ProviderManager;
import shmutalov.verusminer9000.controls.SimpleTriangleIndicator;
import shmutalov.verusminer9000.miner.AbstractMiningService;
import shmutalov.verusminer9000.miner.AbstractMiningServiceBinder;
import shmutalov.verusminer9000.miner.IMiningServiceStateListener;
import shmutalov.verusminer9000.miner.MiningConfig;
import shmutalov.verusminer9000.miner.VerusBinMiningService;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

public class MainActivity extends BaseActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener
{
    private static final String LOG_TAG = "MainActivity";

    private TextView tvHashrate, tvStatus, tvNbCores, tvCPUTemperature, tvBatteryTemperature, tvAcceptedShares, tvDifficulty, tvLog, tvStatusProgess;
    private TubeSpeedometer meterCores, meterHashrate, meterHashrate_avg, meterHashrate_max;
    private SeekBar sbCores = null;

    private LinearLayout llMain, llLog, llHashrate, llStatus;

    private ProgressBar pbPayout;
    private boolean payoutEnabled;
    protected IProviderListener payoutListener;

    private Timer timerHashrate = null;
    private TimerTask timerTaskHashrate = null;
    private ProgressBar pbStatus;

    private boolean validArchitecture = true;

    private AbstractMiningServiceBinder binder;
    private boolean bPayoutDataReceived = false;

    private boolean bIgnoreCPUCoresEvent = false;
    private boolean bIsRestartEvent = false;
    private boolean bIsRestartDialogShown = false;
    private boolean bForceMiningOnPause = false;

    // Settings
    private boolean bDisableTemperatureControl = false;
    private boolean bDisableAmayc = false;
    private Integer nMaxCPUTemp = 0;
    private Integer nMaxBatteryTemp = 0;
    private Integer nSafeCPUTemp = 0;
    private Integer nSafeBatteryTemp = 0;
    private Integer nThreads = 1;
    private Integer nCores = 1;
    private Integer nIntensity = 1;

    private long nLastShareCount = 0;

    private Integer nNbMaxCores = 0;

    private double sumHr = 0.0;
    private Integer nHrCount = 0;
    private double maxHr = 0.0;

    // Temperature Control
    private Timer timerTemperatures = null;
    private TimerTask timerTaskTemperatures = null;
    private final List<String> listCPUTemp = new ArrayList<>();
    private final List<String> listBatteryTemp = new ArrayList<>();
    private boolean isCharging = false;

    public static Context contextOfApplication;

    private boolean isServerConnectionBound = false;
    private boolean isBatteryReceiverRegistered = false;

    private PowerManager.WakeLock wl;

    public static Context getContextOfApplication() {
        return contextOfApplication;
    }

    private Button btnStart;

    private final static int STATE_STOPPED = 0;
    private final static int STATE_MINING = 1;
    private final static int STATE_PAUSED = 2;
    private final static int STATE_COOLING = 3;
    private final static int STATE_CALCULATING = 4;

    private static int m_nLastCurrentState = STATE_STOPPED;
    private static int m_nCurrentState = STATE_STOPPED;
    public int getCurrentState() { return m_nCurrentState; }

    private NotificationManager notificationManager = null;
    private NotificationCompat.Builder notificationBuilder = null;

    private File imagePath = null;

    public static boolean isDeviceMiningBackground() {
        return (m_nCurrentState == STATE_CALCULATING || m_nCurrentState == STATE_MINING || m_nCurrentState == STATE_COOLING);
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
            // TODO: make abstract
            Intent intent = new Intent(this, VerusBinMiningService.class);
            bindService(intent, serverConnection, BIND_AUTO_CREATE);
            startService(intent);
            isServerConnectionBound = true;
        }

        setContentView(R.layout.activity_main);

        BottomNavigationView navigationView = findViewById(R.id.main_navigation);
        navigationView.getMenu().getItem(0).setChecked(true);
        navigationView.setOnNavigationItemSelectedListener(this);

        // Open Settings the first time the app is launched
        if (Config.read("address").equals("")) {
            navigationView.getMenu().getItem(2).setChecked(true);

            SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("settings_fragment");
            if(fragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment,"settings_fragment").commit();
            }
            else {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment(),"settings_fragment").commit();
            }
        }

        // Layouts
        llMain = findViewById(R.id.layout_main);
        llLog = findViewById(R.id.layout_mining_log);
        llHashrate = findViewById(R.id.layout_hashrate);
        llStatus = findViewById(R.id.layout_status);

        // Controls

        payoutEnabled = true;
        pbPayout = findViewById(R.id.progresspayout);
        pbStatus = findViewById(R.id.progress_status);

        pbStatus.setMax(MAX_HASHRATE_TIMER * 2);
        pbStatus.setProgress(0);

        tvStatusProgess = findViewById(R.id.hr_progress);

        // Log
        tvLog = findViewById(R.id.output);
        tvLog.setMovementMethod(new ScrollingMovementMethod());

        // CPU Cores

        nNbMaxCores = Runtime.getRuntime().availableProcessors();
        nCores = Utils.parseIntOrDefault(Config.read("cores"), nNbMaxCores == 1 ? 1 : nNbMaxCores - 1);

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
        meterCores.makeSections(1, ResourcesCompat.getColor(getResources(), R.color.c_yellow, getTheme()), Section.Style.SQUARE);
        meterCores.setMaxSpeed(nNbMaxCores);
        meterCores.speedTo(nCores, 0);

        tvNbCores = findViewById(R.id.nbcores);

        // Hashrate
        meterHashrate = findViewById(R.id.meter_hashrate);
        meterHashrate.makeSections(1, ResourcesCompat.getColor(getResources(), R.color.c_blue, getTheme()), Section.Style.SQUARE);

        LineIndicator indicator_speed = new LineIndicator(contextOfApplication, 0.15f);
        indicator_speed.setColor(ResourcesCompat.getColor(getResources(), R.color.c_white, getTheme()));
        indicator_speed.setWidth(14.0f);
        meterHashrate.setIndicator(indicator_speed);

        // Average Meter
        meterHashrate_avg = findViewById(R.id.meter_hashrate_avg);
        meterHashrate_avg.makeSections(1, ResourcesCompat.getColor(getResources(), android.R.color.transparent, getTheme()), Section.Style.SQUARE);

        SimpleTriangleIndicator indicator_avg = new SimpleTriangleIndicator(contextOfApplication);
        indicator_avg.setWidth(40.0f);
        indicator_avg.setColor(ResourcesCompat.getColor(getResources(), R.color.txt_main, getTheme()));
        meterHashrate_avg.setIndicator(indicator_avg);

        // Max Meter
        meterHashrate_max = findViewById(R.id.meter_hashrate_max);
        meterHashrate_max.makeSections(1, ResourcesCompat.getColor(getResources(), android.R.color.transparent, getTheme()), Section.Style.SQUARE);

        SimpleTriangleIndicator indicator_max = new SimpleTriangleIndicator(contextOfApplication);
        indicator_max.setWidth(40.0f);
        indicator_max.setColor(ResourcesCompat.getColor(getResources(), R.color.c_orange, getTheme()));
        meterHashrate_max.setIndicator(indicator_max);

        tvHashrate = findViewById(R.id.hashrate);
        tvStatus = findViewById(R.id.miner_status);

        tvCPUTemperature = findViewById(R.id.cputemp);
        tvBatteryTemperature = findViewById(R.id.batterytemp);

        tvAcceptedShares = findViewById(R.id.acceptedshare);
        tvDifficulty  = findViewById(R.id.difficulty);

        btnStart = findViewById(R.id.start);
        enableStartBtn(false);

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

                    final Dialog dialog = new Dialog(MainActivity.this);
                    dialog.setContentView(R.layout.stop_mining);
                    dialog.setCancelable(false);

                    Button btnYes = dialog.findViewById(R.id.btnStopMiningYes);
                    btnYes.setOnClickListener(v -> {
                        nCores = sbCores.getProgress();
                        Config.write("cores", Integer.toString(nCores));

                        bIsRestartEvent = true;

                        MainActivity.this.stopMining(); // Stop mining

                        // Start miner with small delay
                        new Handler().postDelayed(() -> {
                            MainActivity.this.startMining(); // Start mining
                        }, 1000);

                        updateCores();

                        dialog.dismiss();
                        bIsRestartDialogShown = false;
                    });

                    Button btnNo = dialog.findViewById(R.id.btnStopMiningNo);
                    btnNo.setOnClickListener(v -> {
                        runOnUiThread(() -> {
                            bIgnoreCPUCoresEvent = true;
                            sbCores.setProgress(nCores);
                            bIgnoreCPUCoresEvent = false;
                        });

                        dialog.dismiss();
                        bIsRestartDialogShown = false;
                    });

                    dialog.show();
                    bIsRestartDialogShown = true;
                }
                else {
                    nCores = sbCores.getProgress();
                    Config.write("cores", Integer.toString(nCores));
                    updateCores();
                }
            }
        });

        // TODO: make abstract
        AbstractMiningService miner = new VerusBinMiningService();
        if (!Arrays.asList(miner.getSupportedArchitectures()).contains(Tools.getABI())) {
            String sArchError = "Your architecture is not supported: " + Tools.getABI();
            appendLogOutputFormattedText(sArchError);
            refreshLogOutputView();
            setStatusText(sArchError);

            validArchitecture = false;
        }

        Button btnShare = findViewById(R.id.btnShare);
        btnShare.setOnClickListener(v -> {
            Bitmap bitmap = takeScreenshot();
            saveBitmap(bitmap);
            shareIt();
        });

        ProviderManager.generate();

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

        updateUI();
    }

    @Override
    protected void onDestroy() {
        if (isServerConnectionBound) {
            unbindService(serverConnection);
        }

        if (isBatteryReceiverRegistered) {
            unregisterReceiver(batteryInfoReceiver);
        }

        super.onDestroy();
    }

    public void onShowCores(View view) {
        toggleHashrate();
    }

    public void startTimerTemperatures() {
        if(timerTemperatures != null) {
            return;
        }

        timerTaskTemperatures = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> updateTemperatures());
            }
        };

        timerTemperatures = new Timer();
        timerTemperatures.scheduleAtFixedRate(timerTaskTemperatures, 0, 10000);
    }

    public void stoptTimerTemperatures() {
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
            Toast.makeText(contextOfApplication, status, Toast.LENGTH_SHORT).show();
        }
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
            TextView tvBalance = findViewById(R.id.balance);
            tvBalance.setText(sBalance);

            float fMinPayout;
            if(Config.read("mininggoal").equals(""))
                fMinPayout = Utils.convertStringToFloat(d.pool.minPayout);
            else
                fMinPayout = Utils.convertStringToFloat(Config.read("mininggoal").trim());

            float fBalance = Utils.convertStringToFloat(sBalance);
            if (fBalance > 0 && fMinPayout > 0) {
                pbPayout.setProgress(Math.round(fBalance));
                pbPayout.setMax(Math.round(fMinPayout));
            } else {
                pbPayout.setProgress(0);
                pbPayout.setMax(100);
            }

            String sPercentagePayout = String.valueOf(Math.round(fBalance / fMinPayout *100));
            TextView tvPercentagePayout = findViewById(R.id.percentage);
            tvPercentagePayout.setText(sPercentagePayout);
        }
    }

    public void enablePayoutWidget(boolean enable, String text) {
        TextView tvPayoutWidgetTitle = findViewById(R.id.payoutgoal);
        TextView tvMessage = findViewById(R.id.payoutmessage);

        if (enable) {
            if(tvPayoutWidgetTitle.getVisibility() == View.VISIBLE)
                return;

            tvPayoutWidgetTitle.setVisibility(View.VISIBLE);

            TextView tvBalance = findViewById(R.id.balance);
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
            if(tvPayoutWidgetTitle.getVisibility() != View.INVISIBLE) {

                tvPayoutWidgetTitle.setVisibility(View.INVISIBLE);

                TextView tvBalance = findViewById(R.id.balance);
                tvBalance.setVisibility(View.INVISIBLE);

                TextView tvXLAUnit = findViewById(R.id.xlaunit);
                tvXLAUnit.setVisibility(View.INVISIBLE);

                TextView tvPercentage = findViewById(R.id.percentage);
                tvPercentage.setVisibility(View.INVISIBLE);

                TextView tvPercentageUnit = findViewById(R.id.percentageunit);
                tvPercentageUnit.setVisibility(View.INVISIBLE);
            }

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
        payoutEnabled = false;

        /*
        if(doesPoolSupportAPI()) {
            if(llPayoutWidget.getVisibility() != View.VISIBLE)
                llPayoutWidget.setVisibility(View.VISIBLE);
        }
        else {
            if(llPayoutWidget.getVisibility() != View.GONE)
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

        payoutEnabled = true;*/
    }

    private boolean isValidConfig() {
        Log.i(LOG_TAG, "isValidConfig");
        PoolItem pi = ProviderManager.getSelectedPool();

        String init = Config.read("init");
        String address = Config.read("address");
        String pool = pi == null ? "" : pi.getPool();
        String port = pi == null ? "" : pi.getPort();

        boolean result = init.equals("1") &&
                !address.equals("") &&
                pi != null &&
                !pi.getPool().equals("") &&
                !pi.getPort().equals("");

        Log.i(LOG_TAG, String.format("isValidConfig [init = %s, address = %s, pool = %s, port = %s] == %b", init, address, pool, port, result));

        return result;
    }

    public void updateUI() {
        loadSettings();

        // Worker Name
        TextView tvWorkerName = findViewById(R.id.workername);
        String sWorkerName = Config.read("workername");
        if(!sWorkerName.equals("")) {
            tvWorkerName.setText(sWorkerName);
        }

        updatePayoutWidgetStatus();
        refreshLogOutputView();
        updateCores();
        adjustMetricsLayout();
    }

    private void adjustMetricsLayout() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int height = size.y;

        ImageView imgAcceptedShare = findViewById(R.id.imgacceptedshare);
        ImageView imgDifficulty = findViewById(R.id.imgdifficulty);

        if(height < 2000) {
            imgAcceptedShare.setVisibility(View.GONE);
            imgDifficulty.setVisibility(View.GONE);
        } else {
            imgAcceptedShare.setVisibility(View.VISIBLE);
            imgDifficulty.setVisibility(View.VISIBLE);
        }
    }

    public void updateStartButton() {
        Log.i(LOG_TAG, "updateStartButton");

        enableStartBtn(isValidConfig());
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
                for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                    if (fragment != null) {
                        getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                    }
                }

                llMain.setVisibility(View.VISIBLE);
                llLog.setVisibility(View.GONE);

                updateStatsListener();
                updateUI();

                break;
            }
            case R.id.menu_log: {
                for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                    if (fragment != null) {
                        getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                    }
                }

                llMain.setVisibility(View.GONE);
                llLog.setVisibility(View.VISIBLE);

                updateStatsListener();
                updateUI();

                break;
            }
//            case R.id.menu_stats: {
//                StatsFragment fragment_stats = (StatsFragment) getSupportFragmentManager().findFragmentByTag("fragment_stats");
//                if (fragment_stats == null) {
//                    fragment_stats = new StatsFragment();
//                }
//
//                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment_stats, "fragment_stats").commit();
//
//                llMain.setVisibility(View.VISIBLE);
//                llLog.setVisibility(View.GONE);
//
//                break;
//            }
            case R.id.menu_settings: {
                SettingsFragment settings_fragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("settings_fragment");
                if (settings_fragment == null) {
                    settings_fragment = new SettingsFragment();
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, settings_fragment, "settings_fragment").commit();

                llMain.setVisibility(View.VISIBLE);
                llLog.setVisibility(View.GONE);

                break;
            }
            case R.id.menu_about: {
                AboutFragment fragment_about = (AboutFragment) getSupportFragmentManager().findFragmentByTag("fragment_about");
                if (fragment_about == null) {
                    fragment_about = new AboutFragment();
                }

                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment_about, "fragment_about").commit();

                llMain.setVisibility(View.VISIBLE);
                llLog.setVisibility(View.GONE);

                break;
            }
        }

        updateUI();

        return true;
    }

    public void updateStatsListener() {
        ProviderManager.afterSave();
        ProviderManager.request.setListener(payoutListener).start();

        if(!ProviderManager.data.isNew) {
            updatePayoutWidget(ProviderManager.data);
            enablePayoutWidget(true, "VRSC");
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void loadSettings() {
        Log.i(LOG_TAG, "loadSettings");

        if (!Config.read("init").equals("1"))
            return;

        nThreads = Integer.parseInt(Config.read("threads"));

        // Load AMAYC Settings
        bDisableTemperatureControl = Config.read("disableamayc").equals("1");
        nMaxCPUTemp = Integer.parseInt(Config.read("maxcputemp").trim());
        nMaxBatteryTemp = Integer.parseInt(Config.read("maxbatterytemp").trim());
        int nCooldownThreshold = Integer.parseInt(Config.read("cooldownthreshold").trim());

        nSafeCPUTemp = nMaxCPUTemp - Math.round((float)nMaxCPUTemp * (float)nCooldownThreshold / 100.0f);
        nSafeBatteryTemp = nMaxBatteryTemp - Math.round((float)nMaxBatteryTemp * (float)nCooldownThreshold / 100.0f);

        nCores = Integer.parseInt(Config.read("cores"));
        nIntensity = Integer.parseInt(Config.read("intensity"));
    }

    private void startMining() {
        Log.i(LOG_TAG, "stopMining");

        if (binder == null) return;

        if (!Config.read("init").equals("1")) {
            setStatusText("Save settings before mining.");
            return;
        }

        String address = Config.read("address");
        String password = Config.read("password");
        String workername = Config.read("workername");

        if (!Utils.verifyAddress(address)) {
            setStatusText("Invalid wallet address.");
            return;
        }

        if (Config.read("pauseonbattery").equals("1") && !isCharging && !bForceMiningOnPause) {
            askToForceMining();
            return;
        }

        bForceMiningOnPause = false;

        String username = address + Config.read("usernameparameters");

        resetOptions();

        loadSettings();

        AbstractMiningService s = binder.getService();
        MiningConfig cfg = s.newConfig(
                username,
                password,
                workername,
                nCores,
                nThreads,
                nIntensity
        );

        s.startMining(cfg);

        showNotification();

        setMinerStatus(STATE_MINING);

        updateUI();
    }

    private void askToForceMining() {
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.force_mining);
        dialog.setCancelable(false);

        Button btnYes = dialog.findViewById(R.id.btnStopMiningYes);
        btnYes.setOnClickListener(v -> {
            bForceMiningOnPause = true;

            if(isDevicePaused()) {
                clearMinerLog = false;
                resumeMiner();
            }
            else
                startMining();

            dialog.dismiss();
        });

        Button btnNo = dialog.findViewById(R.id.btnStopMiningNo);
        btnNo.setOnClickListener(v -> {
            bForceMiningOnPause = false;
            dialog.dismiss();
        });

        dialog.show();
    }

    private void resetOptions() {
        bDisableAmayc = false;
        listCPUTemp.clear();
        listBatteryTemp.clear();

        nLastShareCount = 0;

        sumHr = 0.0f;
        nHrCount = 0;
        maxHr = 0.0f;
    }

    public void stopMining() {
        Log.i(LOG_TAG, "stopMining");

        if(binder == null) {
            return;
        }

        setMinerStatus(STATE_STOPPED);

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
            // TODO: make abstract
            Intent intent = new Intent(this, VerusBinMiningService.class);
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

        if(isValidConfig()) {
            enableStartBtn(true);

            if (m_nCurrentState == STATE_STOPPED ) {
                updateHashrate(0.0f, 0.0f);
                DrawableCompat.setTint(buttonDrawableStart, ResourcesCompat.getColor(getResources(), R.color.bg_green, getTheme()));
                btnStart.setBackground(buttonDrawableStart);
                btnStart.setText(R.string.start);
            } else if(m_nCurrentState == STATE_PAUSED) {
                updateHashrate(0.0f, 0.0f);
                DrawableCompat.setTint(buttonDrawableStart, ResourcesCompat.getColor(getResources(), R.color.bg_green, getTheme()));
                btnStart.setBackground(buttonDrawableStart);
                btnStart.setText(R.string.resume);
            }
            else {
                updateHashrate(-1.0f, -1.0f);
                DrawableCompat.setTint(buttonDrawableStart, ResourcesCompat.getColor(getResources(), R.color.bg_lighter, getTheme()));
                btnStart.setBackground(buttonDrawableStart);
                btnStart.setText(R.string.stop);
            }
        }
        else {
            enableStartBtn(false);
        }
    }

    private void enableSliderCores(boolean enable) {
        if(bIsRestartEvent)
            return;

        Rect bounds = sbCores.getProgressDrawable().getBounds();

        if(enable) {
            sbCores.setProgressDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.seekbar_ruler_yellow, getTheme()));
            sbCores.getThumb().setColorFilter(ResourcesCompat.getColor(getResources(), R.color.c_white, getTheme()), PorterDuff.Mode.SRC_IN);
        }
        else {
            sbCores.setProgressDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.seekbar_ruler_inactive, getTheme()));
            sbCores.getThumb().setColorFilter(ResourcesCompat.getColor(getResources(), R.color.c_light_grey, getTheme()), PorterDuff.Mode.SRC_IN);
        }

        sbCores.getProgressDrawable().setBounds(bounds);
    }

    private void setMinerStatus(Integer status) {
        if(status == STATE_STOPPED) {
            llStatus.setVisibility(View.GONE);
            llHashrate.setVisibility(View.VISIBLE);

            tvHashrate.setText("0");
            tvHashrate.setTextColor(ResourcesCompat.getColor(getResources(), R.color.txt_inactive, getTheme()));

            View v = findViewById(R.id.main_navigation);
            v.setKeepScreenOn(false);

            meterHashrate.speedTo(0);
            meterHashrate_avg.setVisibility(View.GONE);
            meterHashrate_max.setVisibility(View.GONE);

            stopTimerStatusHashrate();
            resetHashrateTicks();
            enableSliderCores(true);
        }
        else if(status == STATE_MINING) {
            if(tvHashrate.getText().equals("0")) {
                setMinerStatus(STATE_CALCULATING);
            } else {
                llStatus.setVisibility(View.GONE);
                llHashrate.setVisibility(View.VISIBLE);

                tvHashrate.setTextColor(ResourcesCompat.getColor(getResources(), R.color.c_white, getTheme()));

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

            if (status == STATE_PAUSED && isDeviceMining()) {
                tvStatus.setText(getResources().getString(R.string.paused));
                stopTimerStatusHashrate();

                pbStatus.setIndeterminate(true);
                pbStatus.setProgress(0);
                tvStatusProgess.setVisibility(View.INVISIBLE);
            } else if (status == STATE_COOLING && isDeviceMining()) {
                tvStatus.setText(getResources().getString(R.string.cooling));
                stopTimerStatusHashrate();

                pbStatus.setIndeterminate(true);
            } else if (status == STATE_CALCULATING) {
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
                runOnUiThread(() -> incrementProgressHashrate());
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
        meterTicks.setTextColor(ResourcesCompat.getColor(getResources(), android.R.color.transparent, getTheme()));

        meterHashrate.setMaxSpeed(500);
        meterHashrate_avg.setMaxSpeed(500);
        meterHashrate_max.setMaxSpeed(500);
    }

    private void updateHashrateTicks(double max) {
        SpeedView meterTicks = findViewById(R.id.meter_hashrate_ticks);
        if (meterTicks.getTickNumber() == 0) {
            meterTicks.setTickNumber(10);
            meterTicks.setTextColor(ResourcesCompat.getColor(getResources(), R.color.txt_main, getTheme()));
        }

        if (max > 0) {
            float hrMax = (float)(nNbMaxCores * max / nCores);
            if(!nCores.equals(nNbMaxCores)) {
                hrMax = hrMax * 1.05f;
            }

            meterTicks.setMaxSpeed(hrMax);
            meterTicks.setTickNumber(10);
            //meterTicks.setTextColor(ResourcesCompat.getColor(getResources(), R.color.txt_main, getTheme()));

            meterHashrate.setMaxSpeed(hrMax);
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
        return (m_nCurrentState == STATE_CALCULATING || m_nCurrentState == STATE_MINING);
    }

    private boolean isDevicePaused() {
        return m_nCurrentState == STATE_PAUSED;
    }

    private boolean isDeviceCooling() {
        return m_nCurrentState == STATE_COOLING;
    }

    private void updateHashrate(double speed, double max) {
        if(!isDeviceMining() || speed < 0.0)
            return;

        SpeedView meterTicks = findViewById(R.id.meter_hashrate_ticks);
        updateHashrateTicks(max);

        if (meterTicks.getTickNumber() == 0) {
            // Start timer
            new Handler().postDelayed(() -> updateHashrateMeter(speed, max), 2000);
        } else {
            updateHashrateMeter(speed, max);
        }
    }

    private void updateHashrateMeter(double speed, double max) {
        meterHashrate.speedTo(Math.round(speed));

        tvHashrate.setText(String.format(Locale.getDefault(), "%.1f", speed));
        setMinerStatus(STATE_MINING);

        if(speed <= 0.0) {
            tvHashrate.setTextColor(ResourcesCompat.getColor(getResources(), R.color.txt_inactive, getTheme()));
        } else {
            tvHashrate.setTextColor(ResourcesCompat.getColor(getResources(), R.color.c_white, getTheme()));
        }

        updateAvgMaxHashrate(speed, max);
    }

    private void resetAvgMaxHashrate() { updateAvgMaxHashrate(0.0, 0.0); }

    private void updateAvgMaxHashrate(double speed, double max) {
        TextView tvAvgHr = findViewById(R.id.avghr);
        TextView tvMaxHr = findViewById(R.id.maxhr);

        // Average Hashrate
        if(speed > 0.0) {
            nHrCount++;
            sumHr += speed;

            float avgHr = (float)(sumHr / (double) nHrCount);
            tvAvgHr.setText(String.format(Locale.getDefault(), "%.1f", avgHr));

            if (meterHashrate_avg.getVisibility() == View.GONE)
                meterHashrate_avg.setVisibility(View.VISIBLE);
            meterHashrate_avg.setSpeedAt(avgHr);
        }
        else {
            tvAvgHr.setText(String.format(Locale.getDefault(), "%.1f", 0.0));
            meterHashrate_avg.setVisibility(View.GONE);
        }

        // Max Hashrate
        if(max > 0.0) {
            if(max > maxHr)
                maxHr = max;

            tvMaxHr.setText(String.format(Locale.getDefault(), "%.1f", maxHr));

            if(meterHashrate_max.getVisibility() == View.GONE)
                meterHashrate_max.setVisibility(View.VISIBLE);
            meterHashrate_max.setSpeedAt((float)maxHr);
        }
        else {
            tvMaxHr.setText(String.format(Locale.getDefault(), "%.1f", 0.0));
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

        if(m_nLastCurrentState == STATE_COOLING && text.contains("resumed")) {
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
            text = text.replace("kH/s ", "");
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
                    textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_white, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_grey, getTheme())), imax, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    return textSpan;
                }
            }
        }

        Spannable textSpan = new SpannableString(text);

        // Format time
        formatText = "]";
        if(text.contains(formatText)) {
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_dark_grey, getTheme())), 0, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), 0, 10, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        if (speed) {
            int i = text.indexOf("]");
            int max = text.lastIndexOf("s");
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_blue, getTheme())), i+1, max+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, max+1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "cpu accepted";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_green, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "cpu READY";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_white, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "net use pool";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_grey, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "net new job from";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_grey, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "rx init dataset";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_grey, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "rx allocated";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_grey, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "rx dataset ready";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_grey, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "cpu use profile";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_grey, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.maxtemperaturereached);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_white, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.miningpaused);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_grey, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.resumedmining);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_white, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "AMYAC error";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_red, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "AMYAC response";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_red, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.amaycerror);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_red, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.statictempcontrol);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.c_white, getTheme())), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        return textSpan;
    }

    private void refreshLogOutputView() {
        if(tvLog != null){
            final Layout layout = tvLog.getLayout();
            if(layout != null) {
                final int scrollAmount = layout.getHeight() - tvLog.getHeight() + tvLog.getPaddingBottom();
                tvLog.scrollTo(0, Math.max(scrollAmount, 0));
            }
        }
    }

    private void appendLogOutputText(String line) {
        boolean refresh = false;
        if(binder != null){
            if (tvLog.getText().length() > Config.logMaxLength ){
                String outputLog = binder.getService().getOutput();
                tvLog.setText(formatLogOutputText(outputLog));
                refresh = true;
            }
        }

        if(!line.equals("")) {
            String outputLog = line + System.getProperty("line.separator");
            tvLog.append(formatLogOutputText(outputLog));
            refresh = true;
        }

        if(refresh) {
            refreshLogOutputView();
        }
    }

    private final ServiceConnection serverConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            binder = (AbstractMiningServiceBinder) iBinder;
            if (validArchitecture) {
                btnStart.setOnClickListener(v -> {
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
                });

                updateMiningButtonState();
                //setMiningButtonState(binder.getService().getMiningServiceState());

                binder.getService().setMiningServiceStateListener(new IMiningServiceStateListener() {
                    @Override
                    public void onStateChange(Boolean state) {
                        Log.i(LOG_TAG, "onMiningStateChange: " + state);
                        runOnUiThread(() -> {
                            updateMiningButtonState();
                            if (state) {
                                if (clearMinerLog) {
                                    tvLog.setText("");
                                    tvAcceptedShares.setText("0 / 0");
                                    tvAcceptedShares.setTextColor(ResourcesCompat.getColor(getResources(), R.color.txt_inactive, getTheme()));

                                    tvDifficulty.setText("0");
                                    tvDifficulty.setTextColor(ResourcesCompat.getColor(getResources(), R.color.txt_inactive, getTheme()));

                                    updateHashrate(-1.0, -1.0);
                                }
                                clearMinerLog = true;
                                setStatusText("Miner Started");
                            } else {

                                setStatusText("Miner Stopped");
                            }

                            bIsRestartEvent = false;
                        });
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onStatusChange(String status, double speed, double max, long accepted, long total, double difficuly, int connection) {
                        runOnUiThread(() -> {
                            appendLogOutputText(status);
                            tvAcceptedShares.setText(accepted + " / " + total);
                            tvDifficulty.setText(NumberFormat.getNumberInstance(Locale.getDefault()).format(difficuly));

                            if(nLastShareCount != accepted) {
                                nLastShareCount = accepted;
                            }

                            if(accepted == 1) {
                                tvAcceptedShares.setTextColor(ResourcesCompat.getColor(getResources(), R.color.c_white, getTheme()));
                                tvDifficulty.setTextColor(ResourcesCompat.getColor(getResources(), R.color.c_white, getTheme()));
                            }

                            updateHashrate(speed, max);
                        });
                    }
                });
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            binder = null;
            enableStartBtn(false);
        }
    };

    private void updateTemperaturesText(float cpuTemp) {
        if (cpuTemp > 0.0) {
            tvCPUTemperature.setText(String.format(Locale.getDefault(), "%.0f", cpuTemp));
        }
        else {
            tvCPUTemperature.setText("n/a");
        }

        if (batteryTemp > 0.0) {
            tvBatteryTemperature.setText(String.format(Locale.getDefault(), "%.0f", batteryTemp));
        }
        else {
            tvBatteryTemperature.setText("n/a");
        }
    }

    private void updateTemperatures() {
        float cpuTemp = Tools.getCurrentCPUTemperature();

        updateTemperaturesText(cpuTemp);

        if(bDisableTemperatureControl)
            return;

        // Check if temperatures are now safe to resume mining
        if(isDeviceCooling()) {
            if (cpuTemp <= nSafeCPUTemp && batteryTemp <= nSafeBatteryTemp) {
                enableCooling(false);
            }

            return;
        }

        if(!isDeviceMining())
            return;

        // Check if current temperatures exceed maximum temperatures
        if (cpuTemp >= nMaxCPUTemp || batteryTemp >= nMaxBatteryTemp) {
            enableCooling(true);

            return;
        }

        if(bDisableAmayc)
            return;

        int nCPU = Math.round(cpuTemp);
        if(nCPU != 0) {
            listCPUTemp.add(Integer.toString(nCPU));
        }

        int nBatt = Math.round(batteryTemp);
        if(nBatt != 0) {
            listBatteryTemp.add(Integer.toString(nBatt));
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
            setMinerStatus(STATE_COOLING);

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
        Log.i(LOG_TAG, "enableStartBtn: " + enabled);

        Drawable buttonDrawable = btnStart.getBackground();
        buttonDrawable = DrawableCompat.wrap(buttonDrawable);

        if (enabled) {
            DrawableCompat.setTint(buttonDrawable, ResourcesCompat.getColor(getResources(), R.color.bg_green, getTheme()));
            btnStart.setBackground(buttonDrawable);
            btnStart.setTextColor(ResourcesCompat.getColor(getResources(), R.color.c_white, getTheme()));
        } else {
            DrawableCompat.setTint(buttonDrawable, ResourcesCompat.getColor(getResources(), R.color.c_inactive, getTheme()));
            btnStart.setBackground(buttonDrawable);
            btnStart.setTextColor(ResourcesCompat.getColor(getResources(), R.color.c_inactive, getTheme()));
        }

        btnStart.setEnabled(enabled);
    }

    private void pauseMiner() {
        if (!isDevicePaused()) {
            if(!isDeviceCooling()) {
                setMinerStatus(STATE_PAUSED);

                enableStartBtn(true);
                updateMiningButtonState();
            }

            if (binder != null) {
                binder.getService().pauseMiner();
            }
        }
    }

    private void resumeMiner() {
        if (isDevicePaused() || isDeviceCooling()) {
            setMinerStatus(STATE_MINING);

            if (binder != null) {
                binder.getService().resumeMiner();
            }

            updateMiningButtonState();
            bForceMiningOnPause = false;
        }
    }

    private void toggleHashrate() {
        if (binder != null) {
            binder.getService().toggleHashrate();
        }
    }

    public static final String OPEN_ACTION = "OPEN_ACTION";
    public static final String STOP_ACTION = "STOP_ACTION";

    private void createNotificationManager() {
        String CHANNEL_ID = "MINING_STATUS";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
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
        if(notificationManager == null)
            createNotificationManager();

        NotificationsReceiver.activity = this;

        // Open intent
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setAction(OPEN_ACTION);
        PendingIntent pendingIntentOpen = PendingIntent.getActivity(contextOfApplication, 1, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Stop intent
        Intent stopIntent = new Intent(this, NotificationsReceiver.class);
        stopIntent.setAction(STOP_ACTION);
        PendingIntent pendingIntentStop = PendingIntent.getBroadcast(contextOfApplication, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentTitle(getResources().getString(R.string.devicemining));
        notificationBuilder.setContentIntent(pendingIntentOpen);
        notificationBuilder.addAction(android.R.drawable.ic_menu_view,"Open", pendingIntentOpen);
        notificationBuilder.addAction(android.R.drawable.ic_lock_power_off,"Stop", pendingIntentStop);
        notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round));
        notificationBuilder.setSmallIcon(R.drawable.ic_notification);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setOnlyAlertOnce(true);
        notificationBuilder.build();

        notificationManager.notify(1, notificationBuilder.build());
    }

    private void hideNotifications() {
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

        String status = m_nCurrentState == STATE_MINING ? "Hashrate: " + tvHashrate.getText().toString() + " kH/s" : tvStatus.getText().toString();

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

        imagePath = new File(Environment.getExternalStorageDirectory() + "/scala_scrnshot.png"); ////File imagePath
        FileOutputStream fos;
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }

            fos = new FileOutputStream(imagePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    private void shareIt() {
        Uri uri = Uri.fromFile(imagePath);
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("image/*");
        String shareBody = "Take a look at my Scala Mobile Miner stats!";
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "My Scala Mobile Miner Stats");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);

        startActivity(Intent.createChooser(sharingIntent, "Share via"));
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

            setStatusText((isCharging ? "Device Charging" : "Device on Battery"));

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
