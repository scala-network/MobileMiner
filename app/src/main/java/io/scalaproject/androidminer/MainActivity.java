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
// Copyright (c) 2020; Scala Project
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.text.Spannable;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/* FOR AMAYC Support */
import android.hardware.Sensor;
import android.hardware.SensorManager;

import io.scalaproject.androidminer.api.Data;
import io.scalaproject.androidminer.api.PoolManager;
import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderAbstract;
import io.scalaproject.androidminer.api.ProviderListenerInterface;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    private static final String LOG_TAG = "MiningSvc";
    private DrawerLayout drawer;
    boolean accepted = false;

    private TextView tvSpeed, tvHs, tvAccepted, tvCPUTemperature, tvBatteryTemperature, tvLog, tvTitle;

    private LinearLayout llPayout;
    private ProgressBar pbPayout;
    private boolean payoutEnabled;
    protected ProviderListenerInterface payoutListener;
    Timer timer;
    long delay = 30000L;

    private boolean validArchitecture = true;

    private MiningService.MiningServiceBinder binder;

    private ScrollView svOutput;

    public static Context contextOfApplication;

    private PowerManager.WakeLock wl;

    public static Context getContextOfApplication() {
        return contextOfApplication;
    }

    private Button minerBtnH, minerBtnP, minerBtnR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        Config.initialize(preferences);
        String configversion = Config.read("config_version");

        if(!configversion.equals(Config.version)) {
            Config.clear();
            Config.write("config_version", Config.version);
        }

        contextOfApplication = getApplicationContext();

        if (wl != null) {
            if (wl.isHeld()) {
                wl.release();
            }
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PARTIAL_WAKE_LOCK, "app:sleeplock");
        wl.acquire();

        registerReceiver(batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        String isshowagain = Config.read("show_again");

        if (isshowagain.equals("")) {
            showdialog();
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        List<Fragment> frags = getSupportFragmentManager().getFragments();
        for(int i = 0; i< frags.size(); i++)
        Log.d(LOG_TAG,"This is the initial FRAG: "+ frags.get(i).toString());

        PoolItem pi = PoolManager.getSelectedPool();

        if (Config.read("address").equals("") || pi == null || pi.getPool().equals("") || pi.getPort().equals("")) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }

            SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("settings_fragment");
            if(fragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment,"settings_fragment").commit();
            }
            else {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment(),"settings_fragment").commit();
            }
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(2).setChecked(true);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        enableButtons(false);

        // Controls

        payoutEnabled = true;
        llPayout = (LinearLayout) findViewById(R.id.layoutpayout);
        pbPayout = (ProgressBar) findViewById(R.id.progresspayout);

        tvTitle = findViewById(R.id.title);

        tvLog = findViewById(R.id.output);
        tvSpeed = findViewById(R.id.speed);
        tvHs = findViewById(R.id.hs);

        tvAccepted = findViewById(R.id.accepted);
        tvCPUTemperature = findViewById(R.id.cputemp);
        tvBatteryTemperature = findViewById(R.id.batterytemp);
        svOutput = findViewById(R.id.outputScrollView);

        minerBtnH = (Button) findViewById(R.id.minerBtnH);
        minerBtnP = (Button) findViewById(R.id.minerBtnP);
        minerBtnR = (Button) findViewById(R.id.minerBtnR);

        updateUI();

        if (!Arrays.asList(Config.SUPPORTED_ARCHITECTURES).contains(Tools.getABI())) {
            Toast.makeText(this, "Your architecture is not supported: " + Tools.getABI(), Toast.LENGTH_LONG).show();
            validArchitecture = false;
        }

        Intent intent = new Intent(this, MiningService.class);
        bindService(intent, serverConnection, BIND_AUTO_CREATE);
        startService(intent);

        minerBtnH.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendInput("h");
            }
        });

        minerBtnP.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendInput("p");
            }
        });

        minerBtnR.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendInput("r");
            }
        });

        payoutListener = new ProviderListenerInterface() {
            public void onStatsChange(Data d) {
                if (!payoutEnabled) {
                    return;
                }

                PoolItem pi = PoolManager.getSelectedPool();

                String wallet = Config.read("address");

                // Payout
                TextView tvTotalHashrate = findViewById(R.id.totalhashrate);
                tvTotalHashrate.setText(d.getMiner().hashrate);

                String balance = d.getMiner().balance;
                TextView tvBalance = findViewById(R.id.balance);
                tvBalance.setText(balance);

                TextView tvPaymentThreshold = findViewById(R.id.payoutthreshold);
                String threshold = d.getPool().minPayout;
                tvPaymentThreshold.setText(threshold);

                float fBalance = Utils.convertStringToFloat(balance);
                float fThreshold = Utils.convertStringToFloat(threshold);
                String percentage = getResources().getString(R.string.na);
                float fpercentage = Float.valueOf(0);

                if(fBalance > 0 && fThreshold > 0) {
                    pbPayout.setProgress(Math.round(fBalance));
                    pbPayout.setMax(Math.round(fThreshold));
                    fpercentage = fBalance / fThreshold * Float.valueOf(100);
                    percentage = Integer.toString(Math.round(fpercentage));
                }
                else{
                    pbPayout.setProgress(0);
                    pbPayout.setMax(100);
                }

                TextView tvPercentage = findViewById(R.id.percentage);
                tvPercentage.setText(percentage);
            }
        };

        //@@TODO: Retrieve pool stats at startup
        /*ProviderAbstract api = pi.getInterface();

        api.setPayoutChangeListener(payoutListener);
        api.execute();*/

        repeatTask();
    }

    private void repeatTask() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }

        if (!payoutEnabled) {
            return;
        }

        timer = new Timer("Timer");

        TimerTask task = new TimerTask() {
            public void run() {
                ProviderAbstract process = PoolManager.getSelectedPool().getInterface();
                process.setPayoutChangeListener(payoutListener);
                process.execute();
                repeatTask();
            }
        };

        timer.schedule(task, delay);
    }

    private void setStatusText(String status) {
        if (status != null && !status.isEmpty() && !status.equals("")) {
            Toast.makeText(getApplicationContext(),status,Toast.LENGTH_SHORT);
        }
    }

    private void enablePayoutWidget() {
        PoolItem pi = PoolManager.getSelectedPool();

        if(Config.read("address").equals("")) {
            llPayout.setVisibility(View.GONE);
            payoutEnabled = false;
            return;
        }

        if (Config.read("init").equals("1") == false || pi == null) {
            llPayout.setVisibility(View.GONE);
            payoutEnabled = false;
            return;
        }

        if (pi.getPoolType() == 0) {
            llPayout.setVisibility(View.GONE);
            payoutEnabled = false;
            return;
        }

        llPayout.setVisibility(View.VISIBLE);
        payoutEnabled = true;
    }

    public void updateUI() {
        PoolItem pi = PoolManager.getSelectedPool();

        String status = "";

        if (pi == null || pi.getPool().equals("") || pi.getPort().equals("") || Config.read("address").equals("")) {
            status = "Update your Wallet Address in 'Settings'";
        }

        setStatusText(status);

        enablePayoutWidget();

        //@@TODO Update AMYAC accordingly
        updateAmyac(false);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.stats:
                StatsFragment fragment_stats = (StatsFragment) getSupportFragmentManager().findFragmentByTag("fragment_stats");
                if(fragment_stats == null) {
                    fragment_stats = new StatsFragment();
                }

                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment_stats,"fragment_stats").commit();

                tvTitle.setText(R.string.stats);

                break;
            case R.id.about:
                AboutFragment fragment_about = (AboutFragment) getSupportFragmentManager().findFragmentByTag("fragment_about");

                if(fragment_about == null) {
                    fragment_about = new AboutFragment();
                }

                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment_about,"fragment_about").commit();

                tvTitle.setText(R.string.about);

                break;
            case R.id.miner: //Main view
                for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                    if (fragment != null) {
                        getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                    }
                }

                tvTitle.setText(R.string.miner);

                updateUI();
                break;
            case R.id.settings:
                SettingsFragment settings_fragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("settings_fragment");
                if(settings_fragment == null) {
                    settings_fragment = new SettingsFragment();
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, settings_fragment,"settings_fragment").commit();

                tvTitle.setText(R.string.settings);

                break;

        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void showdialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.disclaimer);
        dialog.setTitle("Disclaimer");
        dialog.setCancelable(false);
        Button dialogButton = (Button) dialog.findViewById(R.id.btnAgree);
        Button exitButton = (Button) dialog.findViewById(R.id.btnExit);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accepted = true;
                Config.write("show_again", "1");
                dialog.dismiss();
            }
        });
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.finish();
            }
        });
        dialog.show();
    }

    private void startMining(View view) {
        if (binder == null) return;

        if (Config.read("init").equals("1") == false || PoolManager.getSelectedPool() == null) {
            Toast.makeText(contextOfApplication, "Save settings before mining.", Toast.LENGTH_SHORT).show();
            return;
        }

        String pass = Config.read("pass");
        String address = Config.read("address");

        if (!Utils.verifyAddress(address)) {
            Toast.makeText(contextOfApplication, "Invalid wallet address.", Toast.LENGTH_SHORT).show();
            return;
        }

        int cores = Integer.parseInt(Config.read("cores"));
        int threads = Integer.parseInt(Config.read("threads"));
        int intensity = Integer.parseInt(Config.read("intensity"));
        MiningService s = binder.getService();
        MiningService.MiningConfig cfg = s.newConfig(
                address,
                pass,
                cores,
                threads,
                intensity
        );

        s.startMining(cfg);

        updateUI();
    }

    private void stopMining(View view) {
        if(binder == null) {
            return;
        }
        binder.getService().stopMining();
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        SettingsFragment frag = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("settings_fragment");
        if(frag != null) {
            frag.updateAddress();
        }
        appendLogOutputText("");

        repeatTask();
    }

    @Override
    protected void onPause() {

        super.onPause();

        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    private void setMiningState(View view) {
        if (binder == null) return;
        if (binder.getService().getMiningServiceState()) {
            MainActivity.this.stopMining(view);
        } else {
            MainActivity.this.startMining(view);
        }
    }

    private void setMiningButtonState(Boolean state) {
        Button btnStart = findViewById(R.id.start);

        Drawable buttonDrawable = btnStart.getBackground();
        buttonDrawable = DrawableCompat.wrap(buttonDrawable);

        if (minerPaused) {
            btnStart.setText("Resume");
            updateHashrate("0");
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.c_green));
            btnStart.setBackground(buttonDrawable);
        } else {
            if (state) {
                btnStart.setText("Stop");
                updateHashrate("n/a");
                DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.c_red));
                btnStart.setBackground(buttonDrawable);

                // Hashrate button
                minerBtnH.setEnabled(true);
                buttonDrawable = minerBtnH.getBackground();
                buttonDrawable = DrawableCompat.wrap(buttonDrawable);
                DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_green));
                minerBtnH.setBackground(buttonDrawable);
                minerBtnH.setTextColor(getResources().getColor(R.color.c_white));

                // Pause button
                minerBtnP.setEnabled(true);
                buttonDrawable = minerBtnP.getBackground();
                buttonDrawable = DrawableCompat.wrap(buttonDrawable);
                DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_lighter));
                minerBtnP.setBackground(buttonDrawable);
                minerBtnP.setTextColor(getResources().getColor(R.color.c_white));

                // Resume button
                minerBtnR.setEnabled(false);
                buttonDrawable = minerBtnR.getBackground();
                buttonDrawable = DrawableCompat.wrap(buttonDrawable);
                DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_black));
                minerBtnR.setBackground(buttonDrawable);
                minerBtnR.setTextColor(getResources().getColor(R.color.c_black));

            } else {
                btnStart.setText("Start");
                updateHashrate("0");
                DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.c_blue));
                btnStart.setBackground(buttonDrawable);

                // Hashrate button
                minerBtnH.setEnabled(false);
                buttonDrawable = minerBtnH.getBackground();
                buttonDrawable = DrawableCompat.wrap(buttonDrawable);
                DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_black));
                minerBtnH.setBackground(buttonDrawable);
                minerBtnH.setTextColor(getResources().getColor(R.color.c_black));

                // Pause button
                minerBtnP.setEnabled(false);
                buttonDrawable = minerBtnP.getBackground();
                buttonDrawable = DrawableCompat.wrap(buttonDrawable);
                DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_black));
                minerBtnP.setBackground(buttonDrawable);
                minerBtnP.setTextColor(getResources().getColor(R.color.c_black));

                // Resume button
                minerBtnR.setEnabled(false);
                buttonDrawable = minerBtnR.getBackground();
                buttonDrawable = DrawableCompat.wrap(buttonDrawable);
                DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_black));
                minerBtnR.setBackground(buttonDrawable);
                minerBtnR.setTextColor(getResources().getColor(R.color.c_black));
            }
        }
    }

    private void updateHashrate(String speed) {
        String speedstr = speed;
        if (speed.equals("n/a")) {
            speedstr = getResources().getString(R.string.processing);
            tvHs.setVisibility(View.INVISIBLE);
            tvSpeed.setTextColor(getResources().getColor(R.color.c_grey));
            tvSpeed.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        }
        else {
            tvHs.setVisibility(View.VISIBLE);
            tvSpeed.setTextColor(getResources().getColor(R.color.c_green));
            tvSpeed.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        }

        tvSpeed.setText(speedstr);
    }

    private Spannable formatLogOutputText(String text) {
        Spannable textSpan = new SpannableString(text);

        String formatText = "accepted";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i+ formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_blue)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        formatText = "speed";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i+ formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_green)), i, i + formatText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        return textSpan;
    }

    private void appendLogOutputText(String line) {
        boolean refresh = false;
        if(binder != null){
            if ((tvLog.getText().equals("") && !binder.getService().getOutput().equals("")) || tvLog.getText().length() > Config.logMaxLength ){
                String outputLog = binder.getService().getOutput();
                tvLog.setText(formatLogOutputText(outputLog));
                refresh = true;
            }
        }

        if(!line.equals("")) {
            String outputLog = line + System.lineSeparator();
            tvLog.append(formatLogOutputText(outputLog));
            refresh = true;
        }

        if(refresh) {
            svOutput.postDelayed(new Runnable() {
                @Override
                public void run() {
                    svOutput.fullScroll(View.FOCUS_DOWN);
                }
            }, 50);
        }
    }

    private ServiceConnection serverConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            binder = (MiningService.MiningServiceBinder) iBinder;
            if (validArchitecture) {
                enableButtons(true);

                findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (minerPaused) {
                            clearMinerLog = false;
                        }
                        minerPaused = false;
                        setMiningState(v);
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
                                if (clearMinerLog == true) {
                                    tvLog.setText("");
                                    tvAccepted.setText("0");
                                    updateHashrate("n/a");
                                    tvCPUTemperature.setText("n/a");
                                    tvBatteryTemperature.setText("n/a");
                                }
                                clearMinerLog = true;
                                Toast.makeText(contextOfApplication, "Miner Started", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(contextOfApplication, "Miner Stopped", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onStatusChange(String status, String speed, Integer accepted) {
                        runOnUiThread(() -> {
                            appendLogOutputText(status);
                            tvAccepted.setText(Integer.toString(accepted));

                            updateHashrate(speed);

                            float cpuTemp = Tools.getCurrentCPUTemperature();
                            if (cpuTemp != 0.0)
                                tvCPUTemperature.setText(String.format("%.1f", cpuTemp));

                            if (batteryTemp != 0.0)
                                tvBatteryTemperature.setText(String.format("%.1f", batteryTemp));
                        });
                    }
                });
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            binder = null;
            enableButtons(false);
        }
    };

    private void updateAmyac(boolean enabled) {
        int visible = enabled ? View.VISIBLE : View.INVISIBLE;
        findViewById(R.id.arrowdown).setVisibility(visible);
        findViewById(R.id.cooling).setVisibility(visible);
    }

    private void enableButtons(boolean enabled) {
        findViewById(R.id.start).setEnabled(enabled);
    }

    private void sendInput(String s) {

        if (s.equals("p")) {
            if (!minerPaused) {
                minerPaused = true;

                // Pause button
                Drawable buttonDrawable = minerBtnP.getBackground();
                buttonDrawable = DrawableCompat.wrap(buttonDrawable);
                DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_black));
                minerBtnP.setBackground(buttonDrawable);
                minerBtnP.setTextColor(getResources().getColor(R.color.c_black));

                minerBtnP.setEnabled(false);

                // Resume button
                buttonDrawable = minerBtnR.getBackground();
                buttonDrawable = DrawableCompat.wrap(buttonDrawable);
                DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_lighter));
                minerBtnR.setBackground(buttonDrawable);
                minerBtnR.setTextColor(getResources().getColor(R.color.c_white));

                minerBtnR.setEnabled(true);
            }
        }
        else if (s.equals("r")) {
            if (minerPaused) {
                minerPaused = false;

                // Pause button
                Drawable buttonDrawable = minerBtnP.getBackground();
                buttonDrawable = DrawableCompat.wrap(buttonDrawable);
                DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_lighter));
                minerBtnP.setBackground(buttonDrawable);
                minerBtnP.setTextColor(getResources().getColor(R.color.c_white));

                minerBtnP.setEnabled(true);

                // Resume button
                buttonDrawable = minerBtnR.getBackground();
                buttonDrawable = DrawableCompat.wrap(buttonDrawable);
                DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_black));
                minerBtnR.setBackground(buttonDrawable);
                minerBtnR.setTextColor(getResources().getColor(R.color.c_black));

                minerBtnR.setEnabled(false);
            }
        }

        if (binder != null) {
            binder.getService().sendInput(s);
        }
    }

    private boolean minerPaused = false;
    private boolean clearMinerLog = true;
    static boolean lastIsCharging = false;
    static float batteryTemp = 0.0f;
    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent batteryStatus) {

            batteryTemp = (float) (batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10;

            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

            if (lastIsCharging == isCharging) return;
            lastIsCharging = isCharging;

            Toast.makeText(contextOfApplication, (isCharging ? "Device Charging" : "Device on Battery"), Toast.LENGTH_SHORT).show();

            if (Config.read("pauseonbattery").equals("0") == true) {
                minerPaused = false;
                clearMinerLog = true;
                return;
            }

            boolean state = false;
            if (binder != null) {
                state = binder.getService().getMiningServiceState();
            }

            if (isCharging) {
                if (minerPaused) {
                    minerPaused = false;
                    clearMinerLog = false;
                    startMining(null);
                }
            } else if (state) {
                minerPaused = true;
                stopMining(null);
            } else {
                minerPaused = false;
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }
}
