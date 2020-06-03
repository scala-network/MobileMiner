// Copyright (c) 2020, Scala
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

import io.scalaproject.androidminer.api.ProviderManager;

public class WizardSettingsActivity extends BaseActivity {
    private SeekBar sbCores, sbCPUTemp, sbBatteryTemp, sbCooldown;
    private TextView tvCPUMaxTemp, tvBatteryMaxTemp, tvCooldown;

    private Integer nMaxCPUTemp = 65; // 55,60,65,70,75
    private Integer nMaxBatteryTemp = 40; // 30,35,40,45,50
    private Integer nCooldownTheshold = 15; // 10,15,20,25,30

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

        sbCores = view.findViewById(R.id.seekbarcores);
        TextView tvCoresNb = view.findViewById(R.id.coresnb);
        TextView tvCoresMax = view.findViewById(R.id.coresmax);

        sbCPUTemp = view.findViewById(R.id.seekbarcputemperature);
        tvCPUMaxTemp = view.findViewById(R.id.cpumaxtemp);

        sbBatteryTemp = view.findViewById(R.id.seekbarbatterytemperature);
        tvBatteryMaxTemp = view.findViewById(R.id.batterymaxtemp);

        sbCooldown = view.findViewById(R.id.seekbarcooldownthreshold);
        tvCooldown = view.findViewById(R.id.cooldownthreshold);

        Button btnHardwareHelp = view.findViewById(R.id.btnHardwareHelp);
        btnHardwareHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // inflate the layout of the popup window
                LayoutInflater inflater = LayoutInflater.from(getApplicationContext());

                View popupView = inflater.inflate(R.layout.helper_hardware_settings, null);
                Utils.showPopup(v, inflater, popupView);
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

    private void updateCPUTemp(){
        tvCPUMaxTemp.setText(Integer.toString(getCPUTemp()));
    }

    private void updateBatteryTemp() {
        tvBatteryMaxTemp.setText(Integer.toString(getBatteryTemp()));
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

        Config.write("threads", "1"); // Default value
        Config.write("intensity", "1"); // Default value

        Config.write("disableamayc", "0");

        Config.write("init", "1");

        ProviderManager.generate();

        startActivity(new Intent(WizardSettingsActivity.this, MainActivity.class));
        finish();
    }
}