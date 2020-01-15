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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import java.io.BufferedReader;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

/* FOR AMAYC Support */
import android.hardware.Sensor;
import android.hardware.SensorManager;

import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.PoolManager;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    private static final String LOG_TAG = "MiningSvc";
    private DrawerLayout drawer;
    boolean accepted = false;

    private TextView tvLog;
    private TextView tvSpeed, tvAccepted, tvCPUTemperature, tvBatteryTemperature;

    private boolean validArchitecture = true;

    private MiningService.MiningServiceBinder binder;

    private ScrollView svOutput;

    public static Context contextOfApplication;

    private PowerManager.WakeLock wl;

    public static Context getContextOfApplication() {
        return contextOfApplication;
    }

    private Button minerBtn1, minerBtn2, minerBtn3;

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
        for(int i = 0;i< frags.size();i++)
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
            } else {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment(),"settings_fragment").commit();
            }
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(0).setChecked(true);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        enableButtons(false);

        // wire views
        tvLog = findViewById(R.id.output);
        tvSpeed = findViewById(R.id.speed);

        tvAccepted = findViewById(R.id.accepted);
        tvCPUTemperature = findViewById(R.id.cputemp);
        tvBatteryTemperature = findViewById(R.id.batterytemp);
        svOutput = findViewById(R.id.outputScrollView);

        minerBtn1 = (Button) findViewById(R.id.minerBtn1);
        minerBtn2 = (Button) findViewById(R.id.minerBtn2);
        minerBtn3 = (Button) findViewById(R.id.minerBtn3);
        updateUI();

        if (!Arrays.asList(Config.SUPPORTED_ARCHITECTURES).contains(Tools.getABI())) {
            Toast.makeText(this, "Unsupported architecture, yours is " + Tools.getABI(), Toast.LENGTH_LONG).show();
            validArchitecture = false;
        }

        Intent intent = new Intent(this, MiningService.class);
        bindService(intent, serverConnection, BIND_AUTO_CREATE);
        startService(intent);


        minerBtn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendInput("h");
            }
        });

        minerBtn2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendInput("p");
            }
        });

        minerBtn3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendInput("r");
            }
        });

    }

    private void setStatusText(String status) {

        if (status != null && !status.isEmpty() && !status.equals("")) {
            Toast.makeText(getApplicationContext(),status,Toast.LENGTH_SHORT);
        }
    }

    public void updateUI() {
        PoolItem pi = PoolManager.getSelectedPool();

        String status = "";

        if (pi == null || pi.getPool().equals("") || pi.getPort().equals("") || Config.read("address").equals("")) {
            status = "Update your Wallet Address in 'Settings'";
        }

        setStatusText(status);

        minerBtn1.setVisibility(View.VISIBLE);
        minerBtn2.setVisibility(View.VISIBLE);
        minerBtn3.setVisibility(View.VISIBLE);

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
                break;
            case R.id.about:
                AboutFragment fragment_about = (AboutFragment) getSupportFragmentManager().findFragmentByTag("fragment_about");

                if(fragment_about == null) {
                    fragment_about = new AboutFragment();
                }

                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment_about,"fragment_about").commit();
                break;
            case R.id.miner: //Main view
                for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                    if (fragment != null) {
                        getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                    }
                }
                updateUI();
                break;
            case R.id.settings:
                SettingsFragment settings_fragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("settings_fragment");
                if(settings_fragment == null) {
                    settings_fragment = new SettingsFragment();
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, settings_fragment,"settings_fragment").commit();
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
        dialog.setTitle("Disclaimer...");
        dialog.setCancelable(false);
        Button dialogButton = (Button) dialog.findViewById(R.id.button1);
        Button exitButton = (Button) dialog.findViewById(R.id.button2);
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
    }

    @Override
    protected void onPause() {

        super.onPause();
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
        Button btn = findViewById(R.id.start);

        Drawable buttonDrawable = btn.getBackground();
        buttonDrawable = DrawableCompat.wrap(buttonDrawable);

        if (minerPaused) {
            btn.setText("Resume");
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.c_green));
            btn.setBackground(buttonDrawable);
        } else {
            if (state) {
                btn.setText("Stop");
                DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.c_red));
                btn.setBackground(buttonDrawable);
            } else {
                btn.setText("Start");
                DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.c_blue));
                btn.setBackground(buttonDrawable);
            }
        }

    }

    private void appendLogOutputText(String line) {
        boolean refresh = false;
        if(binder != null){
            if ((tvLog.getText().equals("") && !binder.getService().getOutput().equals("")) || tvLog.getText().length() > Config.logMaxLength ){
                tvLog.setText(binder.getService().getOutput());
                refresh = true;
            }
        }

        if(!line.equals("")) {
            tvLog.append(line + System.lineSeparator());
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
                                    tvSpeed.setText("0");
                                    tvCPUTemperature.setText("0");
                                    tvBatteryTemperature.setText("0");
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
                            StringBuilder temp = new StringBuilder();
                            temp.append(Tools.getCurrentCPUTemperature());

                            if(batteryTemp > 0.0f) {
                                temp.append(" (");
                                temp.append(batteryTemp);
                                temp.append((char) 0x00B0);
                                temp.append("C)");
                            }
                            appendLogOutputText(status);
                            tvAccepted.setText(Integer.toString(accepted));
                            tvSpeed.setText(speed);
                            tvCPUTemperature.setText(Tools.getCurrentCPUTemperature());
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
