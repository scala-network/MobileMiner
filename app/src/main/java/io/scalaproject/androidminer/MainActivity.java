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

package io.scalaproject.androidminer;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.Layout;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
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
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.anastr.speedviewlib.TubeSpeedometer;
import com.github.anastr.speedviewlib.components.Section;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.scalaproject.androidminer.api.IProviderListener;
import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderData;
import io.scalaproject.androidminer.api.ProviderManager;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

public class MainActivity extends BaseActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener
{
    private static final String LOG_TAG = "MainActivity";

    private TextView tvHashrate, tvStatus, tvNbCores, tvCPUTemperature, tvBatteryTemperature, tvAcceptedShares, tvDifficulty, tvConnection, tvLog;
    private TubeSpeedometer meterCores;
    private TubeSpeedometer meterHashrate;
    private Timer timerCPUUsage = null;
    private SeekBar sbCores = null;
    private TimerTask timerTaskCPUUsage = null;

    private LinearLayout llMain, llLog, llStatus;

    private ProgressBar pbPayout;
    private boolean payoutEnabled;
    protected IProviderListener payoutListener;

    private boolean validArchitecture = true;

    private MiningService.MiningServiceBinder binder;
    private boolean bPayoutDataReceived = false;

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

    private Integer nLastShareCount = 0;

    private Integer nNbMaxCores = 0;

    // Temperature Control
    private Timer timerTemperatures = null;
    private TimerTask timerTaskTemperatures = null;
    private List<String> listCPUTemp = new ArrayList<String>();
    private List<String> listBatteryTemp = new ArrayList<String>();
    private boolean isCharging = false;

    public static Context contextOfApplication;

    private boolean isServerConnectionBound = false;
    private boolean isBatteryReceiverRegistered = false;

    private PowerManager.WakeLock wl;

    public static Context getContextOfApplication() {
        return contextOfApplication;
    }

    private Button btnStart;
    private ImageView imgShowCores;

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

    public static boolean isDeviceMiningBackground() {
        return (m_nCurrentState == STATE_CALCULATING || m_nCurrentState == STATE_MINING || m_nCurrentState == STATE_COOLING);
    }

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

        //tvTitle = findViewById(R.id.title);
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
        llStatus = findViewById(R.id.layout_status);

        // Controls
        payoutEnabled = true;
        pbPayout = findViewById(R.id.progresspayout);

        // Log
        tvLog = findViewById(R.id.output);
        tvLog.setMovementMethod(new ScrollingMovementMethod());
        imgShowCores = findViewById(R.id.showCores);

        // CPU Cores

        nNbMaxCores = Runtime.getRuntime().availableProcessors();
        int nSettingsCores = Integer.parseInt(Config.read("cores"));

        TubeSpeedometer meterDummy = findViewById(R.id.meter_dummy);
        meterDummy.setMaxSpeed(nNbMaxCores);
        meterDummy.setTickNumber(nNbMaxCores + 1); // Keep this line to patch a bug in the meter implementation
        meterDummy.setOnPrintTickLabel((integer, aFloat) -> {
            String tick = "▮";
            Spannable textSpan = new SpannableString(tick);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            return textSpan;
        });
        meterDummy.invalidate();

        meterCores = findViewById(R.id.meter_cores);
        meterCores.makeSections(1, getResources().getColor(R.color.c_yellow), Section.Style.SQUARE);
        meterCores.speedTo(nSettingsCores);

        tvNbCores = findViewById(R.id.nbcores);

        // Hashrate

        meterHashrate = findViewById(R.id.meter_hashrate);
        meterHashrate.makeSections(1, getResources().getColor(R.color.c_blue), Section.Style.SQUARE);

        tvHashrate = findViewById(R.id.hashrate);
        tvStatus = findViewById(R.id.miner_status);

        tvCPUTemperature = findViewById(R.id.cputemp);
        tvBatteryTemperature = findViewById(R.id.batterytemp);

        tvAcceptedShares = findViewById(R.id.acceptedshare);
        tvDifficulty  = findViewById(R.id.difficulty);
        tvConnection  = findViewById(R.id.connection);

        btnStart = findViewById(R.id.start);
        enableStartBtn(false);

        // Cores seekbar
        /*sbCores = findViewById(R.id.seekbar_cores);
        sbCores.setMax(nb_max_cores);
        sbCores.setProgress(settings_cores);

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
                if(isDeviceMining()) {
                    stopMining();
                }

                int coresnb = sbCores.getProgress();
                Config.write("cores", Integer.toString(coresnb));
                meterCores.speedTo(coresnb);
            }
        });*/

        if (!Arrays.asList(Config.SUPPORTED_ARCHITECTURES).contains(Tools.getABI())) {
            String sArchError = "Your architecture is not supported: " + Tools.getABI();
            appendLogOutputFormattedText(sArchError);
            refreshLogOutputView();
            setStatusText(sArchError);

            validArchitecture = false;
        }

        imgShowCores.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendInput("h");
            }
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
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onShowCores(View view) {
        sendInput("h");
    }

    private void updateCPUUsage() {
        meterCores.speedTo(Math.round(Tools.getCPUUsage()));
    }

    private void showAcceptedShareNM() {
        tvAcceptedShares.setAlpha(1.0f);

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(1f, 0f);
        valueAnimator.setDuration(5000);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float alpha = (float) animation.getAnimatedValue();
                tvAcceptedShares.setAlpha(alpha);
            }
        });

        valueAnimator.start();
    }

    public void startTimerCPUUsage() {
        if(timerCPUUsage != null) {
            return;
        }

        timerTaskCPUUsage = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        updateCPUUsage();
                    }
                });
            }
        };

        timerCPUUsage = new Timer();
        timerCPUUsage.scheduleAtFixedRate(timerTaskCPUUsage, 0, 10000);
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

            float fMinPayout = 100;
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
        PoolItem pi = ProviderManager.getSelectedPool();

        return  Config.read("init").equals("1") &&
                !Config.read("address").equals("") &&
                pi != null &&
                !pi.getPool().equals("") &&
                !pi.getPort().equals("");
    }

    public void updateUI() {
        loadSettings();

        // Worker Name
        TextView tvWorkerName = findViewById(R.id.workername);
        String sWorkerName = Config.read("workername");
        if(!sWorkerName.equals(""))
            tvWorkerName.setText(sWorkerName);

        updatePayoutWidgetStatus();
        refreshLogOutputView();
        updateCores();
    }

    public void updateStartButton() {
        if (isValidConfig()) {
            enableStartBtn(true);
        }
        else {
            enableStartBtn(false);
        }
    }

    private void updateCores() {
        String sCores = nCores + "/" + nNbMaxCores;
        tvNbCores.setText(sCores);
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
            case R.id.menu_stats: {
                StatsFragment fragment_stats = (StatsFragment) getSupportFragmentManager().findFragmentByTag("fragment_stats");
                if (fragment_stats == null) {
                    fragment_stats = new StatsFragment();
                }

                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment_stats, "fragment_stats").commit();

                llMain.setVisibility(View.VISIBLE);
                llLog.setVisibility(View.GONE);

                break;
            }
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
        }

        updateUI();

        return true;
    }

    public void updateStatsListener() {
        ProviderManager.afterSave();
        ProviderManager.request.setListener(payoutListener).start();

        if(!ProviderManager.data.isNew) {
            updatePayoutWidget(ProviderManager.data);
            enablePayoutWidget(true, "XLA");
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void loadSettings() {
        if (!Config.read("init").equals("1"))
            return;

        nThreads = Integer.parseInt(Config.read("threads"));

        // Load AMAYC Settings
        bDisableTemperatureControl = Config.read("disableamayc").equals("1");
        nMaxCPUTemp = Integer.parseInt(Config.read("maxcputemp").trim());
        nMaxBatteryTemp = Integer.parseInt(Config.read("maxbatterytemp").trim());
        Integer nCooldownThreshold = Integer.parseInt(Config.read("cooldownthreshold").trim());

        nSafeCPUTemp = nMaxCPUTemp - Math.round(nMaxCPUTemp * nCooldownThreshold / 100);
        nSafeBatteryTemp = nMaxBatteryTemp - Math.round(nMaxBatteryTemp * nCooldownThreshold / 100);

        nCores = Integer.parseInt(Config.read("cores"));
        nIntensity = Integer.parseInt(Config.read("intensity"));
    }

    private void startMining() {
        if (binder == null) return;

        if (!Config.read("init").equals("1")) {
            setStatusText("Save settings before mining.");
            return;
        }

        String password = Config.read("workername");
        String address = Config.read("address");

        if (!Utils.verifyAddress(address)) {
            setStatusText("Invalid wallet address.");
            return;
        }

        if (Config.read("pauseonbattery").equals("1") && !isCharging) {
            setStatusText(getResources().getString(R.string.pauseonmining));
            return;
        }

        String username = address + Config.read("usernameparameters");

        resetOptions();

        loadSettings();

        MiningService s = binder.getService();
        MiningService.MiningConfig cfg = s.newConfig(
                username,
                password,
                nCores,
                nThreads,
                nIntensity
        );

        s.startMining(cfg);

        showNotification();

        setMinerStatus(STATE_MINING);

        updateUI();
    }

    private void resetOptions() {
        bDisableAmayc = false;
        listCPUTemp.clear();
        listBatteryTemp.clear();

        nLastShareCount = 0;
    }

    public void stopMining() {
        if(binder == null) {
            return;
        }

        setMinerStatus(STATE_STOPPED);

        binder.getService().stopMining();

        resetOptions();

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

    private void setMiningState() {
        if (binder == null)
            return;

        if (binder.getService().getMiningServiceState()) {
            clearMinerLog = true;
            MainActivity.this.stopMining();
        } else {
            MainActivity.this.startMining();
        }
    }

    private void setMiningButtonState(Boolean state) {
        Drawable buttonDrawableStart = btnStart.getBackground();
        buttonDrawableStart = DrawableCompat.wrap(buttonDrawableStart);

        Drawable buttonDrawableShowCores = btnStart.getBackground();
        buttonDrawableShowCores = DrawableCompat.wrap(buttonDrawableShowCores);

        if(isValidConfig()) {
            enableStartBtn(true);

            if (state) {
                updateHashrate("n/a");
                DrawableCompat.setTint(buttonDrawableStart, getResources().getColor(R.color.bg_lighter));
                btnStart.setBackground(buttonDrawableStart);
                btnStart.setText("Stop");

                // Hashrate button
                imgShowCores.setEnabled(true);

                // Pause button
                //activatePauseBtn(true);
            } else {
                updateHashrate("0");
                DrawableCompat.setTint(buttonDrawableStart, getResources().getColor(R.color.c_green));
                btnStart.setBackground(buttonDrawableStart);
                btnStart.setText("Start");

                // Hashrate button
                imgShowCores.setEnabled(false);

                // Pause button
                //activatePauseBtn(false);
            }
        }
        else {
            enableStartBtn(false);
        }
    }

    private void setMinerStatus(Integer status) {
        if(status == STATE_STOPPED) {
            tvHashrate.setText("0");
            tvHashrate.setTextColor(getResources().getColor(R.color.txt_inactive));

            meterHashrate.speedTo(0);

            View v = findViewById(R.id.main_navigation);
            v.setKeepScreenOn(false);
        }
        else if(status == STATE_MINING) {
            if(tvHashrate.getText().equals("0") || tvHashrate.getText().equals("n/a")) {
                setMinerStatus(STATE_CALCULATING);
            } else {
                llStatus.setVisibility(View.GONE);
                tvHashrate.setVisibility(View.VISIBLE);

                //tvHashrate.setTextSize(55.0f);
                tvHashrate.setTextColor(getResources().getColor(R.color.c_white));
            }

            if (Config.read("keepscreenonwhenmining").equals("1")) {
                View v = findViewById(R.id.main_navigation);
                v.setKeepScreenOn(true);
            }
        }
        else {
            llStatus.setVisibility(View.VISIBLE);
            tvHashrate.setVisibility(View.GONE);

            if (status == STATE_PAUSED && isDeviceMining()) {
                tvStatus.setText(getResources().getString(R.string.paused));
            } else if (status == STATE_COOLING && isDeviceMining()) {
                tvStatus.setText(getResources().getString(R.string.cooling));
            } else if (status == STATE_CALCULATING) {
                tvStatus.setText(getResources().getString(R.string.processing));
            }
        }

        m_nLastCurrentState = m_nCurrentState;
        m_nCurrentState = status;

        updateNotification();
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

    private void updateHashrate(String speed) {
        if(!isDeviceMining())
            return;

        Integer nspeed = Math.round(Utils.convertStringToFloat(speed));
        meterHashrate.speedTo(nspeed);

        tvHashrate.setText(speed);
        setMinerStatus(STATE_MINING);

        if(speed.equals("0")) {
            tvHashrate.setTextColor(getResources().getColor(R.color.txt_inactive));
        }
        else {
            tvHashrate.setTextColor(getResources().getColor(R.color.c_white));
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
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_red)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "AMYAC response";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_red)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = getResources().getString(R.string.amaycerror);
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = text.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_red)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        if(tvLog != null){
            final Layout layout = tvLog.getLayout();
            if(layout != null) {
                final int scrollAmount = layout.getHeight() - tvLog.getHeight() + tvLog.getPaddingBottom();
                if (scrollAmount > 0)
                    tvLog.scrollTo(0, scrollAmount);
                else
                    tvLog.scrollTo(0, 0);
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

    private ServiceConnection serverConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            binder = (MiningService.MiningServiceBinder) iBinder;
            if (validArchitecture) {
                btnStart.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (isDevicePaused()) {
                            clearMinerLog = false;
                        }

                        setMiningState();
                    }
                });

                setMiningButtonState(binder.getService().getMiningServiceState());

                binder.getService().setMiningServiceStateListener(new MiningService.MiningServiceStateListener() {
                    @Override
                    public void onStateChange(Boolean state) {
                        Log.i(LOG_TAG, "onMiningStateChange: " + state);
                        runOnUiThread(() -> {
                            setMiningButtonState(state);
                            if (state) {
                                if (clearMinerLog) {
                                    tvLog.setText("");
                                    tvAcceptedShares.setText("0");
                                    tvAcceptedShares.setTextColor(getResources().getColor(R.color.txt_inactive));

                                    tvDifficulty.setText("0");
                                    tvDifficulty.setTextColor(getResources().getColor(R.color.txt_inactive));

                                    tvConnection.setText("0");
                                    tvConnection.setTextColor(getResources().getColor(R.color.txt_inactive));

                                    updateHashrate("n/a");
                                }
                                clearMinerLog = true;
                                setStatusText("Miner Started");
                            } else {

                                setStatusText("Miner Stopped");
                            }
                        });
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onStatusChange(String status, String speed, Integer accepted, Integer difficuly, Integer connection) {
                        runOnUiThread(() -> {
                            appendLogOutputText(status);
                            tvAcceptedShares.setText(Integer.toString(accepted));
                            tvDifficulty.setText(Integer.toString(difficuly));
                            tvConnection.setText(Integer.toString(connection));

                            if(nLastShareCount != accepted) {
                                //showAcceptedShareNM();
                                nLastShareCount = accepted;
                            }

                            if(accepted == 1) {
                                tvAcceptedShares.setTextColor(getResources().getColor(R.color.txt_secondary));
                                tvDifficulty.setTextColor(getResources().getColor(R.color.txt_secondary));
                                tvConnection.setTextColor(getResources().getColor(R.color.txt_secondary));
                            }


                            updateHashrate(speed);
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
            tvCPUTemperature.setText(String.format("%.1f", cpuTemp));
        }
        else {
            tvCPUTemperature.setText("n/a");
        }

        if (batteryTemp > 0.0) {
            tvBatteryTemperature.setText(String.format("%.1f", batteryTemp));
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

                                if (predictedNext != null) {
                                    if (predictedNext.length() == 2) {
                                        Integer cpupred = (int)Math.round(predictedNext.getDouble(0));
                                        Integer batterypred = (int)Math.round(predictedNext.getDouble(1));

                                        if (cpupred >= nMaxCPUTemp || batterypred >= nMaxBatteryTemp) {
                                            enableCooling(true);
                                        }
                                    }
                                }
                            }
                        } else if (uri.contains("check1")) {
                            if(obj.has("predicted_next")) {
                                double predictedNext = obj.getDouble("predicted_next");

                                if (!listCPUTemp.isEmpty()) {
                                    Integer cpupred = (int)Math.round(predictedNext);
                                    if (cpupred >= nMaxCPUTemp) {
                                        enableCooling(true);
                                    }
                                } else if (!listBatteryTemp.isEmpty()) {
                                    Integer batterypred = (int)Math.round(predictedNext);
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
            String responseBody = new String(error.networkResponse.data, "UTF-8");
            JSONObject data = new JSONObject(responseBody);
            JSONArray errors = data.getJSONArray("errors");
            JSONObject jsonMessage = errors.getJSONObject(0);

            message = "AMYAC error: " + jsonMessage.getString("message");
        } catch (JSONException | UnsupportedEncodingException e) {
            message = "AMYAC error JSONException: " + e.getMessage();
        } finally {
            disableAmaycOnError(message);
        }
    }

    private void disableAmaycOnError(String error) {
        bDisableAmayc = true;
        appendLogOutputFormattedText(getResources().getString(R.string.amaycerror));
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
                //activatePauseBtn(true);
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
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.c_green));
            btnStart.setBackground(buttonDrawable);
            btnStart.setTextColor(getResources().getColor(R.color.c_white));
        } else {
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.c_black));
            btnStart.setBackground(buttonDrawable);
            btnStart.setTextColor(getResources().getColor(R.color.c_black));
        }

        btnStart.setEnabled(enabled);
    }

    /*private void enablePauseResumeBtn(boolean enabled)
    {
        btnMinerPR.setImageResource(R.drawable.ic_pause);
        btnMinerPR.setEnabled(enabled);
    }

    private void activatePauseBtn(boolean activatePause) {
        if(activatePause) {
            btnMinerPR.setImageResource(R.drawable.ic_pause);
        } else {
            btnMinerPR.setImageResource(R.drawable.ic_play);
        }
    }*/

    private void pauseMiner() {
        if (!isDevicePaused()) {
            //activatePauseBtn(false);

            if(!isDeviceCooling())
                setMinerStatus(STATE_PAUSED);

            if (binder != null) {
                binder.getService().sendInput("p");
            }
        }
    }

    private void resumeMiner() {
        if (isDevicePaused() || isDeviceCooling()) {
            //activatePauseBtn(true);

            setMinerStatus(STATE_MINING);

            if (binder != null) {
                binder.getService().sendInput("r");
            }
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
    private final String CHANNEL_ID = "MINING_STATUS";

    private void createNotificationManager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, getResources().getString(R.string.miningstatus), NotificationManager.IMPORTANCE_LOW);
            notificationManager = getSystemService(NotificationManager.class);
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
        //notificationBuilder.setContentText(getResources().getString(R.string.hashrate) + ": " + tvSpeed.getText().toString() + " H/s");
        notificationBuilder.setContentIntent(pendingIntentOpen);
        notificationBuilder.addAction(android.R.drawable.ic_menu_view,"Open", pendingIntentOpen);
        notificationBuilder.addAction(android.R.drawable.ic_lock_power_off,"Stop", pendingIntentStop);
        notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round));
        notificationBuilder.setSmallIcon(R.drawable.ic_notification);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setOnlyAlertOnce(true);
        notificationBuilder.build();

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationBuilder.setChannelId(CHANNEL_ID);*/

        //NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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

        String status = m_nCurrentState == STATE_MINING ? "Hashrate: " + tvHashrate.getText().toString() + " H/s" : tvHashrate.getText().toString();

        notificationBuilder.setContentText(status);
        notificationManager.notify(1, notificationBuilder.build());
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
