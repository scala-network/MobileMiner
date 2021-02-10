// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.button.MaterialButtonToggleGroup;

import io.scalaproject.androidminer.widgets.Toolbar;

public class WizardSettingsActivity extends BaseActivity {
    private SeekBar sbCores, sbCPUTemp, sbBatteryTemp, sbCooldown;
    private TextView tvCPUMaxTemp, tvBatteryMaxTemp, tvCooldown, tvCPUTempUnit, tvBatteryTempUnit;

    private Integer nMaxCPUTemp = Config.DefaultMaxCPUTemp; // 60,65,70,75,80
    private Integer nMaxBatteryTemp = Config.DefaultMaxBatteryTemp; // 30,35,40,45,50
    private Integer nCooldownTheshold = Config.DefaultCooldownTheshold; // 5,10,15,20,25

    private Toolbar toolbar;

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
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setOnButtonListener(new Toolbar.OnButtonListener() {
            @Override
            public void onButtonMain(int type) {
                switch (type) {
                    case Toolbar.BUTTON_MAIN_BACK:
                        //onBackPressed();
                        //startActivity(new Intent(WizardSettingsActivity.this, PoolActivity.class));
                        finish();
                        break;
                }
            }

            @Override
            public void onButtonOptions(int type) {
                switch (type) {
                    // Do nothing
                }
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

        Button btnTemperatureControlHelp = view.findViewById(R.id.btnTemperatureControlHelp);
        btnTemperatureControlHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // inflate the layout of the popup window
                LayoutInflater inflater = LayoutInflater.from(getApplicationContext());

                View popupView = inflater.inflate(R.layout.helper_max_temperature, null);
                Utils.showPopup(v, inflater, popupView);
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

        sbCores.setMax(cores);
        tvCoresMax.setText(Integer.toString(cores));

        if (Config.read("cores").equals("")) {
            sbCores.setProgress(suggested);
            tvCoresNb.setText(Integer.toString(suggested));
        } else {
            int corenb = Integer.parseInt(Config.read("cores"));
            sbCores.setProgress(corenb);
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
        if (!Config.read("maxcputemp").equals("")) {
            nMaxCPUTemp = Integer.parseInt(Config.read("maxcputemp"));
        }

        int nProgress = ((nMaxCPUTemp - Utils.MIN_CPU_TEMP)/Utils.INCREMENT) + 1;
        sbCPUTemp.setProgress(nProgress);
        updateCPUTemp();

        if (!Config.read("maxbatterytemp").equals("")) {
            nMaxBatteryTemp = Integer.parseInt(Config.read("maxbatterytemp"));
        }

        // Battery Temp
        nProgress = ((nMaxBatteryTemp-Utils.MIN_BATTERY_TEMP)/Utils.INCREMENT)+1;
        sbBatteryTemp.setProgress(nProgress);
        updateBatteryTemp();

        if (!Config.read("cooldownthreshold").equals("")) {
            nCooldownTheshold = Integer.parseInt(Config.read("cooldownthreshold"));
        }

        // Cooldown
        nProgress = ((nCooldownTheshold - Utils.MIN_COOLDOWN) / Utils.INCREMENT) + 1;
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
                tvCoresNb.setText(Integer.toString(progress));
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
        return ((sbCPUTemp.getProgress() - 1) * Utils.INCREMENT) + Utils.MIN_CPU_TEMP;
    }

    private Integer getBatteryTemp() {
        return ((sbBatteryTemp.getProgress() - 1) * Utils.INCREMENT) + Utils.MIN_BATTERY_TEMP;
    }

    private Integer getCooldownTheshold() {
        return ((sbCooldown.getProgress() - 1) * Utils.INCREMENT) + Utils.MIN_COOLDOWN;
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
        Config.write("workername", Tools.getDeviceName());

        Config.write("cores", Integer.toString(sbCores.getProgress()));
        Config.write("maxcputemp", Integer.toString(getCPUTemp()));
        Config.write("maxbatterytemp", Integer.toString(getBatteryTemp()));
        Config.write("cooldownthreshold", Integer.toString(getCooldownTheshold()));

        Config.write("disableamayc", "0");

        Config.write(Config.CONFIG_TEMPERATURE_UNIT, tgTemperatureUnit.getCheckedButtonId() == R.id.btnFarehnheit ? "F" : "C");

        Config.write("init", "1");

        Config.write("hide_setup_wizard", "1");

        Intent intent = new Intent(WizardSettingsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_CLEAR_TASK|
                Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }
}