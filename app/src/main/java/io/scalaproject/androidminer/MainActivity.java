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

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.text.Spannable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/* FOR AMAYC Support */
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import io.scalaproject.androidminer.api.IProviderListener;
import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderData;
import io.scalaproject.androidminer.api.ProviderManager;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    private static final String LOG_TAG = "MiningSvc";
    private DrawerLayout drawer;
    boolean accepted = false;

    private TextView tvSpeed, tvHs, tvAccepted, tvCPUTemperature, tvBatteryTemperature, tvLog, tvTitle;

    private ProgressBar pbPayout;
    private boolean payoutEnabled;
    protected IProviderListener payoutListener;

    private boolean validArchitecture = true;

    private MiningService.MiningServiceBinder binder;
    private boolean bPayoutDataReceived = false;

    // Settings
    private boolean bDisableAmayc = false;
    private Integer nMaxCPUTemp = 0;
    private Integer nMaxBatteryTemp = 0;
    private Integer nCooldownThreshold = 0;
    private Integer nSafeCPUTemp = 0;
    private Integer nSafeBatteryTemp = 0;

    private Timer timerAMAYC = null;
    private TimerTask timerTaskAMAYC = null;
    private Integer DELAY_AMAYC = 10000;
    private Integer MAX_NUM_ARRAY = 6;
    private boolean minerPaused = false;
    private boolean minerPausedAMAYC = false;
    private List<String> listCPUTemp = new ArrayList<String>();
    private List<String> listBatteryTemp = new ArrayList<String>();
    private boolean isCharging = false;

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
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        PoolItem pi = ProviderManager.getSelectedPool();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);

        tvTitle = findViewById(R.id.title);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(0).setChecked(true);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        enableStartBtn(false);

        // Open Settings the first time the app is launched
        if (Config.read("address").equals("") || pi == null || pi.getPool().equals("") || pi.getPort().equals("")) {
            navigationView.getMenu().getItem(2).setChecked(true);
            setTitle(getResources().getString(R.string.settings));
            SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("settings_fragment");
            if(fragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment,"settings_fragment").commit();
            }
            else {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment(),"settings_fragment").commit();
            }
        }

        // Controls

        payoutEnabled = true;
        pbPayout = (ProgressBar) findViewById(R.id.progresspayout);

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
    }

    public void startTimerAMAYC() {
        // Load AMAYC Settings
        bDisableAmayc = Config.read("disableamayc").equals("1") == true;
        nMaxCPUTemp = Integer.parseInt(Config.read("maxcputemp").trim());
        nMaxBatteryTemp = Integer.parseInt(Config.read("maxbatterytemp").trim());
        nCooldownThreshold = Integer.parseInt(Config.read("cooldownthreshold").trim());

        nSafeCPUTemp = nMaxCPUTemp - Math.round(nMaxCPUTemp * nCooldownThreshold / 100);
        nSafeBatteryTemp = nMaxBatteryTemp - Math.round(nMaxBatteryTemp * nCooldownThreshold / 100);

        if(bDisableAmayc)
            return;

        if(timerAMAYC != null) {
            return;
        }

        timerTaskAMAYC = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        updateTemperatures();
                    }
                });
            }
        };

        timerAMAYC = new Timer();
        timerAMAYC.scheduleAtFixedRate(timerTaskAMAYC, 0, DELAY_AMAYC);
    }

    public void stoptTimerAMAYC() {
        if(timerAMAYC != null) {
            timerAMAYC.cancel();
            timerAMAYC = null;
            timerTaskAMAYC = null;
        }

        listCPUTemp.clear();
        listBatteryTemp.clear();
    }

    private void setStatusText(String status) {
        if (status != null && !status.isEmpty() && !status.equals("")) {
            Toast.makeText(getApplicationContext(), status, Toast.LENGTH_SHORT);
        }
    }

    private void updatePayoutWidget(ProviderData d) {
        if(d.isNew == true) {
            enablePayoutWidget(false, "");
        }
        else if(d.miner.paid == null) {
            enablePayoutWidget(false, "Loading...");
        }
        else {
            enablePayoutWidget(true, "XLA");

            // Payout
            String sHashrate = d.miner.hashrate;
            sHashrate = sHashrate.replace("H", "");
            TextView tvTotalHashrate = findViewById(R.id.totalhashrate);
            tvTotalHashrate.setText(sHashrate.trim());

            String sBalance = d.miner.balance;
            sBalance = sBalance.replace("XLA", "").trim();
            TextView tvBalance = findViewById(R.id.balance);
            tvBalance.setText(sBalance);

            TextView tvMinPayout = findViewById(R.id.minpayout);

            float fMinPayout = 100;
            if(Config.read("mininggoal").equals(""))
                fMinPayout = Utils.convertStringToFloat(d.pool.minPayout);
            else
                fMinPayout = Utils.convertStringToFloat(Config.read("mininggoal").trim());

            String sMinPayout = String.valueOf(Math.round(fMinPayout));
            tvMinPayout.setText(sMinPayout);

            float fBalance = Utils.convertStringToFloat(sBalance);
            if (fBalance > 0 && fMinPayout > 0) {
                pbPayout.setProgress(Math.round(fBalance));
                pbPayout.setMax(Math.round(fMinPayout));
            } else {
                pbPayout.setProgress(0);
                pbPayout.setMax(100);
            }
        }
    }

    public void enablePayoutWidget(boolean enable, String text) {
        TextView tvTotalHashrate = findViewById(R.id.totalhashrate);

        if (enable) {
            if(tvTotalHashrate.getVisibility() == View.VISIBLE)
                return;

            tvTotalHashrate.setVisibility(View.VISIBLE);

            TextView tvTotalHashrateUnit = findViewById(R.id.totalhashrateunit);
            tvTotalHashrateUnit.setVisibility(View.VISIBLE);

            TextView tvBalance = findViewById(R.id.balance);
            tvBalance.setVisibility(View.VISIBLE);

            TextView tvDivider = findViewById(R.id.divider);
            tvDivider.setVisibility(View.VISIBLE);

            TextView tvMinPayout = findViewById(R.id.minpayout);
            tvMinPayout.setVisibility(View.VISIBLE);

            TextView tvXLAUnit = findViewById(R.id.xlaunit);
            tvXLAUnit.setVisibility(View.VISIBLE);
            tvXLAUnit.setTextColor(getResources().getColor(R.color.c_white));
            tvXLAUnit.setTypeface(null, Typeface.BOLD);
            tvXLAUnit.setText(text);
        }
        else {
            if(tvTotalHashrate.getVisibility() != View.INVISIBLE) {

                tvTotalHashrate.setVisibility(View.INVISIBLE);

                TextView tvTotalHashrateUnit = findViewById(R.id.totalhashrateunit);
                tvTotalHashrateUnit.setVisibility(View.INVISIBLE);

                TextView tvBalance = findViewById(R.id.balance);
                tvBalance.setVisibility(View.INVISIBLE);

                TextView tvDivider = findViewById(R.id.divider);
                tvDivider.setVisibility(View.INVISIBLE);

                TextView tvMinPayout = findViewById(R.id.minpayout);
                tvMinPayout.setVisibility(View.INVISIBLE);
            }

            pbPayout.setProgress(0);
            pbPayout.setMax(100);

            TextView tvXLAUnit = findViewById(R.id.xlaunit);
            if(text.equals("")) {
                tvXLAUnit.setVisibility(View.INVISIBLE);
            }
            else {
                tvXLAUnit.setVisibility(View.VISIBLE);
                tvXLAUnit.setTextColor(getResources().getColor(R.color.c_grey));
                tvXLAUnit.setTypeface(null, Typeface.NORMAL);
                tvXLAUnit.setText(text);
            }
        }
    }

    private void updatePayoutWidgetStatus() {

        if (Config.read("address").equals("")) {
            enablePayoutWidget(false, "");
            payoutEnabled = false;
            return;
        }

        PoolItem pi = ProviderManager.getSelectedPool();

        if (Config.read("init").equals("1") == false || pi == null) {
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

    public void updateUI() {
        PoolItem pi = ProviderManager.getSelectedPool();

        String status = "";
        if (pi == null || pi.getPool().equals("") || pi.getPort().equals("") || Config.read("address").equals("")) {
            status = "Update your Wallet Address in 'Settings'";
        }

        setStatusText(status);

        updatePayoutWidgetStatus();
        refreshLogOutputView();
        enableAmaycStatus(false);
    }

    public void setTitle(String title) {
        tvTitle.setText(title);
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

                setTitle(getResources().getString(R.string.stats));

                break;
            case R.id.about:
                AboutFragment fragment_about = (AboutFragment) getSupportFragmentManager().findFragmentByTag("fragment_about");

                if(fragment_about == null) {
                    fragment_about = new AboutFragment();
                }

                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment_about,"fragment_about").commit();

                setTitle(getResources().getString(R.string.about));

                break;
            case R.id.miner: //Main view
                for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                    if (fragment != null) {
                        getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                    }
                }

                setTitle(getResources().getString(R.string.miner));

                ProviderManager.afterSave();
                ProviderManager.request.setListener(payoutListener).start();
                if(!ProviderManager.data.isNew) {
                    updatePayoutWidget(ProviderManager.data);
                    enablePayoutWidget(true, "XLA");
                }

                updateUI();
                break;
            case R.id.settings:
                SettingsFragment settings_fragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("settings_fragment");
                if(settings_fragment == null) {
                    settings_fragment = new SettingsFragment();
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, settings_fragment,"settings_fragment").commit();

                setTitle(getResources().getString(R.string.settings));

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

    private void startMining() {
        if (binder == null) return;

        if (Config.read("init").equals("1") == false || ProviderManager.getSelectedPool() == null) {
            Toast.makeText(contextOfApplication, "Save settings before mining.", Toast.LENGTH_SHORT).show();
            return;
        }

        String pass = Config.read("pass");
        String address = Config.read("address");

        if (!Utils.verifyAddress(address)) {
            Toast.makeText(contextOfApplication, "Invalid wallet address.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Config.read("pauseonbattery").equals("1") == true && !isCharging) {
            Toast.makeText(contextOfApplication, getResources().getString(R.string.pauseonmining), Toast.LENGTH_SHORT).show();
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

        startTimerAMAYC();

        updateUI();
    }

    public void stopMining() {
        if(binder == null) {
            return;
        }

        binder.getService().stopMining();

        stoptTimerAMAYC();

        enableAmaycStatus(false);

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
            MainActivity.this.stopMining();
        } else {
            MainActivity.this.startMining();
        }
    }

    private void setMiningButtonState(Boolean state) {
        Button btnStart = findViewById(R.id.start);

        Drawable buttonDrawable = btnStart.getBackground();
        buttonDrawable = DrawableCompat.wrap(buttonDrawable);

        if (state) {
            btnStart.setText("Stop");
            updateHashrate("n/a");
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.c_red));
            btnStart.setBackground(buttonDrawable);

            // Hashrate button
            minerBtnH.setEnabled(true);
            buttonDrawable = minerBtnH.getBackground();
            buttonDrawable = DrawableCompat.wrap(buttonDrawable);
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_lighter));
            minerBtnH.setBackground(buttonDrawable);
            minerBtnH.setTextColor(getResources().getColor(R.color.c_white));

            // Pause button
            enablePauseBtn(true);

            // Resume button
            enableResumeBtn(false);
        } else {
            btnStart.setText("Start");
            updateHashrate("0");
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.c_green));
            btnStart.setBackground(buttonDrawable);

            // Hashrate button
            minerBtnH.setEnabled(false);
            buttonDrawable = minerBtnH.getBackground();
            buttonDrawable = DrawableCompat.wrap(buttonDrawable);
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_black));
            minerBtnH.setBackground(buttonDrawable);
            minerBtnH.setTextColor(getResources().getColor(R.color.c_black));

            // Pause button
            enablePauseBtn(false);

            // Resume button
            enableResumeBtn(false);
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
        // Remove date and milliseconds from log
        String formatText = "]";
        if(text.contains(formatText)) {
            StringBuilder sb = new StringBuilder(text);
            text = sb.delete(1, 12).toString();

            int i = text.indexOf(formatText);
            text = sb.delete(i-4, i).toString();
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
                    textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                    textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_lighter)), imax, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), imax, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                    return textSpan;
                }
            }
        }

        Spannable textSpan = new SpannableString(text);

        // Format time
        formatText = "]";
        if(text.contains(formatText)) {
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_black)), 0, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), 0, 10, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        if (speed) {
            int i = text.indexOf("]");
            int max = text.lastIndexOf("s");
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_green)), i+1, max+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), i+1, max+1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "cpu accepted";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_blue)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "cpu READY";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_white)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "net use pool";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_lighter)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "net new job from";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_lighter)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "rx init dataset";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_lighter)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "rx allocated";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_lighter)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "rx dataset ready";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_lighter)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        formatText = "cpu use profile";
        if(text.contains(formatText)) {
            int i = text.indexOf(formatText);
            int imax = i + formatText.length();
            textSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.c_lighter)), i, imax, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpan.setSpan(new StyleSpan(android.graphics.Typeface.NORMAL), i, imax, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return textSpan;
        }

        return textSpan;
    }

    private void refreshLogOutputView() {
        svOutput.postDelayed(new Runnable() {
            @Override
            public void run() {
                svOutput.fullScroll(View.FOCUS_DOWN);
            }
        }, 50);
    }

    private void appendLogOutputText(String line) {
        boolean refresh = false;
        if(binder != null){
            if (/*(tvLog.getText().equals("") && !binder.getService().getOutput().equals("")) || */tvLog.getText().length() > Config.logMaxLength ){
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
                enableStartBtn(true);

                findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (minerPaused) {
                            clearMinerLog = false;
                        }
                        minerPaused = false;
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
                                if (clearMinerLog == true) {
                                    tvLog.setText("");
                                    tvAccepted.setText("0");
                                    tvAccepted.setTextColor(getResources().getColor(R.color.c_grey));

                                    updateHashrate("n/a");
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

                            if(accepted == 1)
                                tvAccepted.setTextColor(getResources().getColor(R.color.c_blue));

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
        if (cpuTemp > 0.0)
            tvCPUTemperature.setText(String.format("%.1f", cpuTemp));
        else
            tvCPUTemperature.setText("n/a");

        if (batteryTemp > 0.0)
            tvBatteryTemperature.setText(String.format("%.1f", batteryTemp));
        else
            tvBatteryTemperature.setText("n/a");
    }

    private void updateTemperatures() {
        float cpuTemp = Tools.getCurrentCPUTemperature();

        updateTemperaturesText(cpuTemp);

        if(minerPausedAMAYC) {
            if (cpuTemp <= nSafeCPUTemp && batteryTemp <= nSafeBatteryTemp) {
                enableCooling(false);
            }

            return;
        }

        if(minerPaused) return;

        Integer nCPU = Math.round(cpuTemp);
        if(nCPU != 0) {
            listCPUTemp.add(Integer.toString(nCPU));
        }

        Integer nBatt = Math.round(batteryTemp);
        if(nBatt != 0) {
            listBatteryTemp.add(Integer.toString(nBatt));
        }

        // Send temperatures to AMAYC engine (asynchronously)
        if(listCPUTemp.size() >= MAX_NUM_ARRAY || listBatteryTemp.size() >= MAX_NUM_ARRAY)
        {
            String uri = getResources().getString(R.string.amaycPostLink);
            if(!listCPUTemp.isEmpty() && !listBatteryTemp.isEmpty()) {
                //https://amaycapi.hayzam.in/check2?arrayc=[35,42,45,50]&arrayb=[32,43,45,38]
                uri = uri + "check2?arrayc=" + listCPUTemp.toString() + "&arrayb=" + listBatteryTemp.toString();
                getAMAYCStatus(uri);
            } else {
                if(!listCPUTemp.isEmpty()) {
                    uri = uri + "check1?array=" + listCPUTemp.toString();
                    getAMAYCStatus(uri);
                } else if(!listBatteryTemp.isEmpty()){
                    uri = uri + "check1?array=" + listBatteryTemp.toString();
                    getAMAYCStatus(uri);
                }
            }

            listCPUTemp.clear();
            listBatteryTemp.clear();
        }
    }

    public void getAMAYCStatus(String uri) {
        Log.i(LOG_TAG, "AMAYC uri: " + uri);

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, uri,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.i(LOG_TAG, "AMAYC response: " + response);

                            JSONObject obj = new JSONObject(response);
                            JSONArray predictedNext = obj.getJSONArray("predicted_next");

                            if(predictedNext.length() == 2) {
                                float cpupred = Float.parseFloat(predictedNext.getString(0));
                                float batterypred = Float.parseFloat(predictedNext.getString(1));

                                if (cpupred >= nMaxCPUTemp || batterypred >= nMaxBatteryTemp) {
                                    enableCooling(true);
                                    return;
                                }
                            } else if(predictedNext.length() == 1) {
                                if(!listCPUTemp.isEmpty()) {
                                    float cpupred = Float.parseFloat(predictedNext.getString(0));
                                    if (cpupred >= nMaxCPUTemp) {
                                        enableCooling(true);
                                        return;
                                    }
                                } else if(!listBatteryTemp.isEmpty()) {
                                    float batterypred = Float.parseFloat(predictedNext.getString(0));
                                    if (batterypred >= nMaxBatteryTemp) {
                                        enableCooling(true);
                                        return;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Toast.makeText(contextOfApplication, "AMAYC error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Stop sending data
                // bDisableAMAYC = true;
                Toast.makeText(contextOfApplication, "AMYAC: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        queue.add(stringRequest);
    }

    private void enableCooling(boolean enable) {
        enableAmaycStatus(enable);

        if(enable) {
            pauseMiner();

            minerPausedAMAYC = true;
            //appendLogOutputText(getResources().getString(R.string.amaycOn));

            enableResumeBtn(false);
        }
        else {
            if (Config.read("pauseonbattery").equals("1") == true && !isCharging) {
                Toast.makeText(contextOfApplication, getResources().getString(R.string.pauseonmining), Toast.LENGTH_SHORT).show();
                enableResumeBtn(true);
                return;
            }

            resumeMiner();

            minerPausedAMAYC = false;
            //appendLogOutputText(getResources().getString(R.string.amaycOff));
        }
    }

    private void enableAmaycStatus(boolean enabled) {
        int visible = enabled ? View.VISIBLE : View.INVISIBLE;
        findViewById(R.id.arrowdown).setVisibility(visible);
        findViewById(R.id.cooling).setVisibility(visible);
    }

    private void enableStartBtn(boolean enabled) {
        findViewById(R.id.start).setEnabled(enabled);
    }

    private void enablePauseBtn(boolean enable)
    {
        Drawable buttonDrawable = minerBtnP.getBackground();
        buttonDrawable = DrawableCompat.wrap(buttonDrawable);

        if(enable) {
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_lighter));
            minerBtnP.setBackground(buttonDrawable);
            minerBtnP.setTextColor(getResources().getColor(R.color.c_white));

            minerBtnP.setEnabled(true);
        } else {
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_black));
            minerBtnP.setBackground(buttonDrawable);
            minerBtnP.setTextColor(getResources().getColor(R.color.c_black));

            minerBtnP.setEnabled(false);
        }
    }

    private void enableResumeBtn(boolean enable)
    {
        Drawable buttonDrawable = minerBtnR.getBackground();
        buttonDrawable = DrawableCompat.wrap(buttonDrawable);

        if(enable) {
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_lighter));
            minerBtnR.setBackground(buttonDrawable);
            minerBtnR.setTextColor(getResources().getColor(R.color.c_white));

            minerBtnR.setEnabled(true);
        } else {
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_black));
            minerBtnR.setBackground(buttonDrawable);
            minerBtnR.setTextColor(getResources().getColor(R.color.c_black));

            minerBtnR.setEnabled(false);
        }
    }

    private void pauseMiner() {
        if (!minerPaused) {
            minerPaused = true;

            enablePauseBtn(false);
            enableResumeBtn(true);

            if (binder != null) {
                binder.getService().sendInput("p");
            }
        }
    }

    private void resumeMiner() {
        if (minerPaused ) {
            minerPaused = false;

            enablePauseBtn(true);
            enableResumeBtn(false);

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
            resumeMiner();
        }
        else {
            if (binder != null) {
                binder.getService().sendInput(s);
            }
        }
    }

    private boolean clearMinerLog = true;
    static boolean lastIsCharging = false;
    static float batteryTemp = 0.0f;
    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent batteryStatus) {
            batteryTemp = (float) (batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10;

            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

            if (lastIsCharging == isCharging) return;
            lastIsCharging = isCharging;

            Toast.makeText(contextOfApplication, (isCharging ? "Device Charging" : "Device on Battery"), Toast.LENGTH_SHORT).show();

            if (Config.read("pauseonbattery").equals("0") == true) {
                minerPaused = false;
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
                } else {
                    minerPaused = false;
                }
            }
        }
    };
}
