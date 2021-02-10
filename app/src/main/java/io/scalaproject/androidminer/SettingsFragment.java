// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderManager;
import io.scalaproject.androidminer.widgets.Notice;
import io.scalaproject.androidminer.widgets.PoolView;

public class SettingsFragment extends Fragment {

    private static final String LOG_TAG = "MiningSvc";

    TextInputLayout tilAddress;
    private EditText edAddress, edWorkerName, edUsernameparameters, edPort, edMiningGoal;

    PoolView pvSelectedPool;

    public static PoolItem selectedPoolTmp = null;

    private Integer nMaxCPUTemp = Config.DefaultMaxCPUTemp; // 60,65,70,75,80
    private Integer nMaxBatteryTemp = Config.DefaultMaxBatteryTemp; // 30,35,40,45,50
    private Integer nCooldownTheshold = Config.DefaultCooldownTheshold; // 5,10,15,20,25

    private SeekBar sbCPUTemp, sbBatteryTemp, sbCooldown, sbCores;
    private TextView tvCPUMaxTemp, tvBatteryMaxTemp, tvCooldown, tvCPUTempUnit, tvBatteryTempUnit;
    private Switch swDisableTempControl, swPauseOnBattery, swKeepScreenOnWhenMining;
    private MaterialButtonToggleGroup tgTemperatureUnit;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ProviderManager.getPools(getContext());

        Button bSave;

        TextView tvCoresNb, tvCoresMax;

        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        Context appContext = MainActivity.getContextOfApplication();
        bSave = view.findViewById(R.id.saveSettings);

        ViewGroup llNotice = view.findViewById(R.id.llNotice);
        llNotice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), VaultActivity.class));
            }
        });
        Notice.showAll(llNotice, Notice.NOTICE_SHOW_VAULT, false);

        tilAddress = view.findViewById(R.id.addressIL);
        edAddress = view.findViewById(R.id.address);

        pvSelectedPool = view.findViewById(R.id.viewPool);
        pvSelectedPool.setOnButtonListener(new PoolView.OnButtonListener() {
            @Override
            public void onButton() {
                onOpenPools();
            }
        });

        pvSelectedPool.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                onOpenPools();
            }
        });

        edPort = view.findViewById(R.id.port);
        edUsernameparameters = view.findViewById(R.id.usernameparameters);
        edWorkerName = view.findViewById(R.id.workername);

        edMiningGoal = view.findViewById(R.id.mininggoal);

        Button bQrCode = view.findViewById(R.id.btnQrCode);

        sbCores = view.findViewById(R.id.seekbarcores);
        tvCoresNb = view.findViewById(R.id.coresnb);
        tvCoresMax = view.findViewById(R.id.coresmax);

        sbCPUTemp = view.findViewById(R.id.seekbarcputemperature);
        tvCPUMaxTemp = view.findViewById(R.id.cpumaxtemp);
        tvCPUTempUnit = view.findViewById(R.id.cputempunit);

        sbBatteryTemp = view.findViewById(R.id.seekbarbatterytemperature);
        tvBatteryMaxTemp = view.findViewById(R.id.batterymaxtemp);
        tvBatteryTempUnit = view.findViewById(R.id.batterytempunit);

        sbCooldown = view.findViewById(R.id.seekbarcooldownthreshold);
        tvCooldown = view.findViewById(R.id.cooldownthreshold);

        swPauseOnBattery = view.findViewById(R.id.chkPauseOnBattery);
        swKeepScreenOnWhenMining = view.findViewById(R.id.chkKeepScreenOnWhenMining);
        swDisableTempControl = view.findViewById(R.id.chkAmaycOff);

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

        // CPU Cores
        int cores = Runtime.getRuntime().availableProcessors();

        int suggested = cores / 2;
        if (suggested == 0) suggested = 1;

        sbCores.setMax(cores);
        tvCoresMax.setText(Integer.toString(cores));

        if (Config.read("cores").isEmpty()) {
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

        if (!Config.read("maxcputemp").isEmpty()) {
            nMaxCPUTemp = Integer.parseInt(Config.read("maxcputemp"));
        }
        int nProgress = ((nMaxCPUTemp-Utils.MIN_CPU_TEMP)/Utils.INCREMENT)+1;
        sbCPUTemp.setProgress(nProgress);
        updateCPUTemp();

        if (!Config.read("maxbatterytemp").isEmpty()) {
            nMaxBatteryTemp = Integer.parseInt(Config.read("maxbatterytemp"));
        }
        nProgress = ((nMaxBatteryTemp-Utils.MIN_BATTERY_TEMP)/Utils.INCREMENT)+1;
        sbBatteryTemp.setProgress(nProgress);
        updateBatteryTemp();

        if (!Config.read("cooldownthreshold").isEmpty()) {
            nCooldownTheshold = Integer.parseInt(Config.read("cooldownthreshold"));
        }
        nProgress = ((nCooldownTheshold-Utils.MIN_COOLDOWN)/Utils.INCREMENT)+1;
        sbCooldown.setProgress(nProgress);
        updateCooldownThreshold();

        boolean disableAmayc = (Config.read("disableamayc").equals("1"));
        if(disableAmayc){
            swDisableTempControl.setChecked(true);
        }
        enableAmaycControl(!disableAmayc);

        if (!Config.read("mininggoal").isEmpty()) {
            edMiningGoal.setText(Config.read("mininggoal"));
        }

        boolean checkStatus = Config.read("pauseonbattery").equals("1");
        if(checkStatus) {
            swPauseOnBattery.setChecked(true);
        }

        boolean checkStatusScreenOn = Config.read("keepscreenonwhenmining").equals("1");
        if(checkStatusScreenOn) {
            swKeepScreenOnWhenMining.setChecked(true);
        }

        if (!Config.read("address").isEmpty()) {
            edAddress.setText(Config.read("address"));
        }

        if (!Config.read("usernameparameters").isEmpty()) {
            edUsernameparameters.setText(Config.read("usernameparameters"));
        }

        if (!Config.read("workername").isEmpty()) {
            edWorkerName.setText(Config.read("workername"));
        }

        sbCores.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
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

        sbCPUTemp.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
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

        sbBatteryTemp.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
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

        sbCooldown.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
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

        swDisableTempControl.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                boolean checked = ((Switch)v).isChecked();
                if (checked) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity(), R.style.MaterialAlertDialogCustom);
                    builder.setTitle("Warning")
                            .setMessage(Html.fromHtml(getString(R.string.warning_temperature_control_prompt)))
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.yes), null)
                            .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    swDisableTempControl.setChecked(false);
                                }
                            })
                            .show();
                }

                enableAmaycControl(!checked);
            }
        });

        selectedPoolTmp = null;
        updatePort();

        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if mining, ask to restart
                if(MainActivity.isDeviceMiningBackground()) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialogCustom);
                    builder.setTitle(getString(R.string.stopmining))
                            .setMessage(getString(R.string.newparametersapplied))
                            .setCancelable(true)
                            .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    saveSettings();
                                }
                            })
                            .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // Do nothing
                                }
                            })
                            .show();
                } else {
                    saveSettings();
                }
            }
        });

        Button btnPasteAddress = view.findViewById(R.id.btnPasteAddress);
        btnPasteAddress.setOnClickListener(v -> edAddress.setText(Utils.pasteFromClipboard(MainActivity.getContextOfApplication())));

        bQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context appContext = MainActivity.getContextOfApplication();
                if (Build.VERSION.SDK_INT >= 23) {
                    if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                    }
                    else {
                        startQrCodeActivity();
                    }
                }
                else {
                    Utils.showToast(appContext, "This version of Android does not support QR Code.", Toast.LENGTH_LONG);
                }
            }
        });

        Button btnMineScala = view.findViewById(R.id.btnMineScala);
        btnMineScala.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialogCustom);
                builder.setTitle(getString(R.string.supporttheproject))
                        .setMessage(getString(R.string.minetoscala))
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                edAddress.setText(Utils.SCALA_XLA_ADDRESS);
                            }
                        })
                        .setNegativeButton(getString(R.string.no), null)
                        .show();
            }
        });

        Button btnTemperatureControlHelp = view.findViewById(R.id.btnTemperatureControlHelp);
        btnTemperatureControlHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // inflate the layout of the popup window
                View popupView = inflater.inflate(R.layout.helper_max_temperature, null);
                Utils.showPopup(v, inflater, popupView);
            }
        });

        Button btnAmaycWarning = view.findViewById(R.id.btnAmaycWarning);
        btnAmaycWarning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // inflate the layout of the popup window
                View popupView = inflater.inflate(R.layout.helper_temperature_control, null);
                TextView tvHelper = popupView.findViewById(R.id.tvHelperMessage);
                tvHelper.setText(Html.fromHtml(getString(R.string.warning_temperature_control)));

                Utils.showPopup(v, inflater, popupView);
            }
        });

        return view;
    }

    private void saveSettings() {
        // Validate address
        String address = edAddress.getText().toString().trim();

        if(address.isEmpty() || !Utils.verifyAddress(address)) {
            tilAddress.setErrorEnabled(true);
            tilAddress.setError(getResources().getString(R.string.invalidaddress));
            requestFocus(edAddress);
            return;
        }

        tilAddress.setErrorEnabled(false);
        tilAddress.setError(null);

        PoolItem selectedPoolItem = getSelectedPoolItem();

        Config.write(Config.CONFIG_SELECTED_POOL, selectedPoolItem.getKey().trim());
        Config.write(Config.CONFIG_POOL_PORT, selectedPoolItem.getDefaultPort().trim());

        Config.write("address", address);

        Config.write("usernameparameters", edUsernameparameters.getText().toString().trim());

        String workername = edWorkerName.getText().toString().trim();
        if(workername.isEmpty()) {
            workername = Tools.getDeviceName();
        }

        Log.i(LOG_TAG,"Worker Name : " + workername);
        Config.write("workername", workername);
        edWorkerName.setText(workername);

        Config.write("custom_port", edPort.getText().toString().trim());
        Config.write("cores", Integer.toString(sbCores.getProgress()));

        Config.write("maxcputemp", Integer.toString(getCPUTemp()));
        Config.write("maxbatterytemp", Integer.toString(getBatteryTemp()));
        Config.write("cooldownthreshold", Integer.toString(getCooldownTheshold()));
        Config.write("disableamayc", (swDisableTempControl.isChecked() ? "1" : "0"));

        String mininggoal = edMiningGoal.getText().toString().trim();
        if(!mininggoal.isEmpty()) {
            Config.write("mininggoal", mininggoal);
        }

        Config.write("pauseonbattery", swPauseOnBattery.isChecked() ? "1" : "0");
        Config.write("keepscreenonwhenmining", swKeepScreenOnWhenMining.isChecked() ? "1" : "0");

        Config.write(Config.CONFIG_TEMPERATURE_UNIT, tgTemperatureUnit.getCheckedButtonId() == R.id.btnFarehnheit ? "F" : "C");

        Config.write("init", "1");

        Utils.showToast(getContext(), "Settings Saved.", Toast.LENGTH_SHORT);

        MainActivity main = (MainActivity) getActivity();
        assert main != null;
        main.stopMining();
        main.loadSettings();

        main.updateStartButton();
        main.updateStatsListener();
        main.updateUI();

        selectedPoolTmp = null;
    }

    private void onOpenPools() {
        Intent intent = new Intent(getActivity(), PoolActivity.class);
        intent.putExtra(PoolActivity.RequesterType, PoolActivity.REQUESTER_SETTINGS);
        startActivity(intent);
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

    private void enableAmaycControl(boolean enable) {
        sbCPUTemp.setEnabled(enable);
        sbBatteryTemp.setEnabled(enable);
        sbCooldown.setEnabled(enable);
    }

    private void startQrCodeActivity() {
        Context appContext = MainActivity.getContextOfApplication();
        try {
            Intent intent = new Intent(appContext, QrCodeScannerActivity.class);
            startActivity(intent);
        }catch (Exception e) {
            Utils.showToast(appContext, e.getMessage(), Toast.LENGTH_SHORT);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Context appContext = MainActivity.getContextOfApplication();
        if (requestCode == 100) {
            if (permissions[0].equals(Manifest.permission.CAMERA) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQrCodeActivity();
            }
            else {
                Utils.showToast(appContext,"Camera permission denied.", Toast.LENGTH_LONG);
            }
        }
    }

    public void updateAddress() {
        String address =  Config.read("address");
        if (edAddress == null || address.isEmpty()) {
            return;
        }

        edAddress.setText(address);
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private PoolItem getSelectedPoolItem() {
        return selectedPoolTmp == null ? ProviderManager.getSelectedPool() : selectedPoolTmp;
    }

    private void updatePort() {
        if(selectedPoolTmp != null) {
            edPort.setText(selectedPoolTmp.getPortRaw());
        } else {
            PoolItem selectedPoolItem = getSelectedPoolItem();

            if (selectedPoolItem != null)
                edPort.setText(selectedPoolItem.getPort());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        pvSelectedPool.onFinishInflate();
        updatePort();
    }
}