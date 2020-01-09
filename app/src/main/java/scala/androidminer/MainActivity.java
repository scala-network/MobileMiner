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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import java.util.Arrays;

/* FOR AMAYC Support */
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener
{
    private static final String LOG_TAG = "MiningSvc";
    private DrawerLayout drawer;
    boolean accepted = false;

    private final static String[] SUPPORTED_ARCHITECTURES = {"arm64-v8a", "armeabi-v7a"};

    private TextView tvLog;
    private TextView edStatus;
    private TextView tvMiningTo;

    private TextView tvSpeed, tvAccepted;
    private boolean validArchitecture = true;
    public static SharedPreferences preferences;

    private MiningService.MiningServiceBinder binder;

    private ScrollView svOutput;

    public static Context contextOfApplication;

    private PowerManager pm;
    private PowerManager.WakeLock wl;

    private SensorManager mSensorManager;
    private Sensor mTempSensor;

    public static Context getContextOfApplication() {
        return contextOfApplication;
    }

    private Button minerBtn1, minerBtn2, minerBtn3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mTempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        //PreferenceHelper.clear();

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

        if (PreferenceHelper.getName("address").equals("") || PreferenceHelper.getName("pool").equals("")) {
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
        tvAccepted = findViewById(R.id.accepted);
        edStatus = findViewById(R.id.status);
        tvMiningTo = findViewById(R.id.miningTo);
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
            edStatus.setVisibility(View.GONE);
            edStatus.setText("");
        } else {
            edStatus.setVisibility(View.VISIBLE);
            edStatus.setText(status);
        }
    }

    public void updateUI() {

        String status = "";
        if (PreferenceHelper.getName("address").equals("")) {
            status = "Update your Wallet Address in 'Settings'";
        }

        setStatusText(status);

        String miningTo = "Mining to:";
        String pool = PreferenceHelper.getName("pool");
        String algo = PreferenceHelper.getName("algo");
        String miner = PreferenceHelper.getName("miner");

        if (pool.equals("") == false) {
            miningTo += "\n" + pool;
        }

        if (miner.equals("") == false || algo.equals("") == false) {

            miningTo += "\n";

            if (miner.equals("") == false) {
                miningTo += miner + ": ";
            }

            if (algo.equals("") == false) {
                miningTo += algo;
            }
        }

        tvMiningTo.setText(miningTo);

        if (miner.equals(Config.miner_xlarig)) {
            minerBtn1.setVisibility(View.VISIBLE);
            minerBtn2.setVisibility(View.VISIBLE);
            minerBtn3.setVisibility(View.VISIBLE);
        } else {
            minerBtn1.setVisibility(View.INVISIBLE);
            minerBtn2.setVisibility(View.INVISIBLE);
            minerBtn3.setVisibility(View.INVISIBLE);
        }

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

        if (PreferenceHelper.getName("init").equals("1") == false) {
            Toast.makeText(contextOfApplication, "Save settings before mining.", Toast.LENGTH_SHORT).show();
            return;
        }

        String pool = PreferenceHelper.getName("pool");
        String pass = PreferenceHelper.getName("pass");
        String address = PreferenceHelper.getName("address");
        String algo = PreferenceHelper.getName("minerAlgo");
        String assetExtension = PreferenceHelper.getName("assetExtension");

        int cores = Integer.parseInt(PreferenceHelper.getName("cores"));
        int threads = Integer.parseInt(PreferenceHelper.getName("threads"));
        int intensity = Integer.parseInt(PreferenceHelper.getName("intensity"));

        /*
        int av = 1;

        if (Tools.getABI().contains("armeabi-v7a")) {
            av = 3;
        }
        */

        MiningService.MiningConfig cfg = binder.getService().newConfig(
                address,
                pool,
                pass,
                cores,
                threads,
                intensity,
                algo,
                assetExtension);

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
        mSensorManager.registerListener(this, mTempSensor, SensorManager.SENSOR_DELAY_NORMAL);

        updateUI();
        SettingsFragment frag = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("settings_fragment");
        if(frag != null) {
            frag.updateAddress();
        }

    }

    @Override
    protected void onPause() {

        super.onPause();
        mSensorManager.unregisterListener(this);

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
            DrawableCompat.setTint(buttonDrawable, Color.rgb(238, 201, 0));
            btn.setBackground(buttonDrawable);
        } else {
            if (state) {
                btn.setText("Stop");
                DrawableCompat.setTint(buttonDrawable, Color.rgb(153, 0, 0));
                btn.setBackground(buttonDrawable);
            } else {
                btn.setText("Start");
                DrawableCompat.setTint(buttonDrawable, Color.rgb(0, 153, 0));
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
                            tvSpeed.setText(speed);
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
    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent batteryStatus) {

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

    /* AMAYC */

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            PreferenceHelper.setName("temperature",String.valueOf(event.values[0]));
        }
    }


}
