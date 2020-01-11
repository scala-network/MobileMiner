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

package scala.androidminer;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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

/* FOR AMAYC Support */
import android.hardware.Sensor;
import android.hardware.SensorManager;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    private static final String LOG_TAG = "MiningSvc";
    private DrawerLayout drawer;
    boolean accepted = false;

    private final static String[] SUPPORTED_ARCHITECTURES = {"arm64-v8a", "armeabi-v7a"};

    private TextView tvLog;


    private TextView tvSpeed, tvAccepted, tvTemperature;
    private boolean validArchitecture = true;
    public static SharedPreferences preferences;

    private MiningService.MiningServiceBinder binder;

    private ScrollView svOutput;

    public static Context contextOfApplication;

    private PowerManager pm;
    private PowerManager.WakeLock wl;


    public static Context getContextOfApplication() {
        return contextOfApplication;
    }

    private Button minerBtn1, minerBtn2, minerBtn3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        String configversion = PreferenceHelper.getName("config_version");

        if(!configversion.equals(Config.version)) {
            PreferenceHelper.clear();
            PreferenceHelper.setName("config_version", Config.version);
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

        String isshowagain = PreferenceHelper.getName("show_again");

        if (isshowagain.equals("")) {
            showdialog();
        }

        super.onCreate(savedInstanceState);
        PoolItem pi = Config.getSelectedPool();
        if (PreferenceHelper.getName("address").equals("") || pi == null || pi.getPool().equals("") || pi.getPort().equals("")) {
            setContentView(R.layout.activity_main);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment(),"settings_fragment").commit();
        } else {
            setContentView(R.layout.activity_main);
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
        tvTemperature = findViewById(R.id.temperature);
        tvAccepted = findViewById(R.id.accepted);
        svOutput = findViewById(R.id.outputScrollView);

        minerBtn1 = (Button) findViewById(R.id.minerBtn1);
        minerBtn2 = (Button) findViewById(R.id.minerBtn2);
        minerBtn3 = (Button) findViewById(R.id.minerBtn3);
        updateUI();

        if (!Arrays.asList(SUPPORTED_ARCHITECTURES).contains(Tools.getABI())) {
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

        if (status == null || status.isEmpty()) {
            //edStatus.setVisibility(View.GONE);
            tvLog.setText("");
        } else {
            //edStatus.setVisibility(View.VISIBLE);
            tvLog.setText(status);
        }
    }

    public void updateUI() {
        PoolItem pi = Config.getSelectedPool();

        String status = "";
        String pool = "";
        if (PreferenceHelper.getName("address").equals("") || pi == null || pi.getPool().equals("") || pi.getPort().equals("")) {
            status = "Update your Wallet Address in 'Settings'";
        } else {
            pool = pi.getPool();
        }

        setStatusText(status);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.stats:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new StatsFragment()).commit();
                break;
            case R.id.about:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new AboutFragment()).commit();
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
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment(),"settings_fragment").commit();
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
                PreferenceHelper.setName("show_again", "1");
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

        if (PreferenceHelper.getName("init").equals("1") == false || Config.getSelectedPool() == null) {
            Toast.makeText(contextOfApplication, "Save settings before mining.", Toast.LENGTH_SHORT).show();
            return;
        }

        String pass = PreferenceHelper.getName("pass");
        String address = PreferenceHelper.getName("address");

        int cores = Integer.parseInt(PreferenceHelper.getName("cores"));
        int threads = Integer.parseInt(PreferenceHelper.getName("threads"));
        int intensity = Integer.parseInt(PreferenceHelper.getName("intensity"));

        MiningService.MiningConfig cfg = binder.getService().newConfig(
                address,
                pass,
                cores,
                threads,
                intensity
        );

        binder.getService().startMining(cfg);

        updateUI();
    }

    private void stopMining(View view) {
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

        if (tvLog.length() > Config.logMaxLength) {
            if (binder != null) {
                tvLog.setText(binder.getService().getOutput());
            }
        } else {
            tvLog.append(line + System.lineSeparator());
        }

        svOutput.postDelayed(new Runnable() {
            @Override
            public void run() {
                svOutput.fullScroll(View.FOCUS_DOWN);
            }
        }, 50);

    }
    private String getCurrentCPUTemperature() {
        String file = readFile("/sys/devices/virtual/thermal/thermal_zone0/temp", '\n');
        float output = 0.0f;
        if (file != null) {
            output = (float) Long.parseLong(file);
        }
        if(output > 0.0f && Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            output = output / 1000;
        }

        return String.format("%.02f "+ (char) 0x00B0 + "C", output);
    }
    private byte[] mBuffer = new byte[4096];

    private String readFile(String file, char endChar) {

        StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            int len = is.read(mBuffer);
            is.close();

            if (len > 0) {
                int i;
                for (i = 0; i < len; i++) {
                    if (mBuffer[i] == endChar) {
                        break;
                    }
                }
                return new String(mBuffer, 0, i);
            }
        } catch (java.io.FileNotFoundException e) {
        } catch (java.io.IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (java.io.IOException e) {
                }
            }
            StrictMode.setThreadPolicy(savedPolicy);
        }
        return null;
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
                                    tvTemperature.setText("0" + (char) 0x00B0 + "C (" + batteryTemp + (char) 0x00B0 + "C)");
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
                        StringBuilder temp = new StringBuilder();
                        temp.append(getCurrentCPUTemperature());
                        Intent intent = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                        batteryTemp   = ((float) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)) / 10;
                        if(batteryTemp > 0.0f) {
                            temp.append(" (");
                            temp.append(batteryTemp);
                            temp.append((char) 0x00B0);
                            temp.append("C)");
                        }
                        final String finalTemp = temp.toString();
                        
                        runOnUiThread(() -> {
                            appendLogOutputText(status);
                            tvAccepted.setText(Integer.toString(accepted));
                            tvSpeed.setText(speed);

                            tvTemperature.setText(finalTemp);
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

            if (PreferenceHelper.getName("pauseonbattery").equals("0") == true) {
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
            } else {
                if (state) {
                    minerPaused = true;
                    stopMining(null);
                } else {
                    minerPaused = false;
                }
            }
        }
    };


}
