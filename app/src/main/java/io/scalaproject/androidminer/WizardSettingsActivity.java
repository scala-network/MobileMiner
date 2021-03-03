// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.Objects;

import io.scalaproject.androidminer.widgets.Toolbar;

public class WizardSettingsActivity extends BaseActivity {
    private SeekBar sbCores, sbCPUTemp, sbBatteryTemp, sbCooldown;
    private TextView tvCPUMaxTemp, tvBatteryMaxTemp, tvCooldown, tvCPUTempUnit, tvBatteryTempUnit;

    private int nMaxCPUTemp = Config.DefaultMaxCPUTemp; // 60,65,70,75,80
    private int nMaxBatteryTemp = Config.DefaultMaxBatteryTemp; // 30,35,40,45,50
    private int nCooldownTheshold = Config.DefaultCooldownTheshold; // 5,10,15,20,25

    private MaterialButtonToggleGroup tgTemperatureUnit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        setContentView(R.layout.fragment_wizard_settings);

        View view = findViewById(android.R.id.content).getRootView();

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        toolbar.setOnButtonListener(new Toolbar.OnButtonListener() {
            @Override
            public void onButtonMain(int type) {
                if (type == Toolbar.BUTTON_MAIN_BACK) {//onBackPressed();
                    //startActivity(new Intent(WizardSettingsActivity.this, PoolActivity.class));
                    finish();
                }
            }

            @Override
            public void onButtonOptions(int type) {
                // Do nothing
            }
        });

        toolbar.setTitle("Settings");
        toolbar.setButtonMain(Toolbar.BUTTON_MAIN_BACK);
        toolbar.setButtonOptions(Toolbar.BUTTON_OPTIONS_NONE);

        sbCores = view.findViewById(R.id.seekbarcores);
        TextView tvCoresNb = view.findViewById(R.id.coresnb);
        TextView tvCoresMax = view.findViewById(R.id.coresmax);

        sbCPUTemp = view.findViewById(R.id.seekbarcputemperature);
        tvCPUMaxTemp = view.findViewById(R.id.cpumaxtemp);
        tvCPUTempUnit = view.findViewById(R.id.cputempunit);

        sbBatteryTemp = view.findViewById(R.id.seekbarbatterytemperature);
        tvBatteryMaxTemp = view.findViewById(R.id.batterymaxtemp);
        tvBatteryTempUnit = view.findViewById(R.id.batterytempunit);

        sbCooldown = view.findViewById(R.id.seekbarcooldownthreshold);
        tvCooldown = view.findViewById(R.id.cooldownthreshold);

        Context context = this;
        Button btnTemperatureControlHelp = view.findViewById(R.id.btnTemperatureControlHelp);
        btnTemperatureControlHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.showPopup(context, getString(R.string.temperature_control), getString(R.string.hardware_settings_help));
            }
        });

        tgTemperatureUnit = view.findViewById(R.id.tgTemperatureUnit);
        tgTemperatureUnit.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if(checkedId == R.id.btnFarehnheit) {
                    tvCPUTempUnit.setText(getString(R.string.celsius));
                    tvBatteryTempUnit.setText(getString(R.string.celsius));
                } else {
                    tvCPUTempUnit.setText(getString(R.string.fahrenheit));
                    tvBatteryTempUnit.setText(getString(R.string.fahrenheit));
                }

                updateCPUTemp();
                updateBatteryTemp();
            }
        });

        // Cores
        int cores = Runtime.getRuntime().availableProcessors();

        int suggested = cores / 2;
        if (suggested == 0) suggested = 1;

        sbCores.setMax(cores-1);
        tvCoresMax.setText(Integer.toString(cores));

        if (Config.read(Config.CONFIG_CORES).equals("")) {
            sbCores.setProgress(suggested-1);
            tvCoresNb.setText(Integer.toString(suggested));
        } else {
            int corenb = Integer.parseInt(Config.read(Config.CONFIG_CORES));
            sbCores.setProgress(corenb-1);
            tvCoresNb.setText(Integer.toString(corenb));
        }

        String temperature_unit = Config.read(Config.CONFIG_TEMPERATURE_UNIT, "C");
        tgTemperatureUnit.check(temperature_unit.equals("C") ? R.id.btnCelsius : R.id.btnFarehnheit);

        if(temperature_unit.equals("C")) {
            tvCPUTempUnit.setText(getString(R.string.celsius));
            tvBatteryTempUnit.setText(getString(R.string.celsius));
        } else {
            tvCPUTempUnit.setText(getString(R.string.fahrenheit));
            tvBatteryTempUnit.setText(getString(R.string.fahrenheit));
        }

        // CPU Temp
        if (!Config.read(Config.CONFIG_MAX_CPU_TEMP).equals("")) {
            nMaxCPUTemp = Integer.parseInt(Config.read(Config.CONFIG_MAX_CPU_TEMP));
        }

        int nProgress = ((nMaxCPUTemp - Utils.MIN_CPU_TEMP)/Utils.INCREMENT);
        sbCPUTemp.setProgress(nProgress);
        updateCPUTemp();

        if (!Config.read(Config.CONFIG_MAX_BATTERY_TEMP).equals("")) {
            nMaxBatteryTemp = Integer.parseInt(Config.read(Config.CONFIG_MAX_BATTERY_TEMP));
        }

        // Battery Temp
        nProgress = ((nMaxBatteryTemp-Utils.MIN_BATTERY_TEMP)/Utils.INCREMENT);
        sbBatteryTemp.setProgress(nProgress);
        updateBatteryTemp();

        if (!Config.read(Config.CONFIG_COOLDOWN_THRESHOLD).equals("")) {
            nCooldownTheshold = Integer.parseInt(Config.read(Config.CONFIG_COOLDOWN_THRESHOLD));
        }

        // Cooldown
        nProgress = ((nCooldownTheshold - Utils.MIN_COOLDOWN)/Utils.INCREMENT);
        sbCooldown.setProgress(nProgress);
        updateCooldownThreshold();

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
                tvCoresNb.setText(Integer.toString(progress+1));
            }
        });

        sbCPUTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                updateCPUTemp();
            }
        });

        sbBatteryTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                updateBatteryTemp();
            }
        });

        sbCooldown.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                updateCooldownThreshold();
            }
        });
    }

    private Integer getCPUTemp() {
        return ((sbCPUTemp.getProgress()) * Utils.INCREMENT) + Utils.MIN_CPU_TEMP;
    }

    private Integer getBatteryTemp() {
        return ((sbBatteryTemp.getProgress()) * Utils.INCREMENT) + Utils.MIN_BATTERY_TEMP;
    }

    private Integer getCooldownTheshold() {
        return ((sbCooldown.getProgress()) * Utils.INCREMENT) + Utils.MIN_COOLDOWN;
    }

    private void updateCPUTemp() {
        int cpu_temp = getCPUTemp();
        tvCPUMaxTemp.setText(tgTemperatureUnit.getCheckedButtonId() == R.id.btnFarehnheit ? Integer.toString(Utils.convertCelciusToFahrenheit(cpu_temp)) : Integer.toString(cpu_temp));
    }

    private void updateBatteryTemp() {
        int battery_temp = getBatteryTemp();
        tvBatteryMaxTemp.setText(tgTemperatureUnit.getCheckedButtonId() == R.id.btnFarehnheit ? Integer.toString(Utils.convertCelciusToFahrenheit(battery_temp)) : Integer.toString(battery_temp));
    }

    private void updateCooldownThreshold() {
        tvCooldown.setText(Integer.toString(getCooldownTheshold()));
    }

    public void onStart(View view) {
        Config.write(Config.CONFIG_WORKERNAME, Tools.getDeviceName());

        Config.write(Config.CONFIG_CORES, Integer.toString(sbCores.getProgress()+1));
        Config.write(Config.CONFIG_MAX_CPU_TEMP, Integer.toString(getCPUTemp()));
        Config.write(Config.CONFIG_MAX_BATTERY_TEMP, Integer.toString(getBatteryTemp()));
        Config.write(Config.CONFIG_COOLDOWN_THRESHOLD, Integer.toString(getCooldownTheshold()));

        Config.write(Config.CONFIG_DISABLE_TEMPERATURE_CONTROL, "0");

        Config.write(Config.CONFIG_TEMPERATURE_UNIT, tgTemperatureUnit.getCheckedButtonId() == R.id.btnFarehnheit ? "F" : "C");

        Config.write(Config.CONFIG_INIT, "1");

        Config.write(Config.CONFIG_HIDE_SETUP_WIZARD, "1");

        Intent intent = new Intent(WizardSettingsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_CLEAR_TASK|
                Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }
}