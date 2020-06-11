// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputLayout;

import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderManager;

public class SettingsFragment extends Fragment {

    private static final String LOG_TAG = "MiningSvc";

    TextInputLayout tilAddress;
    private EditText edAddress, edWorkerName, edUsernameparameters;

    private Integer nMaxCPUTemp = 65; // 55,60,65,70,75
    private Integer nMaxBatteryTemp = 40; // 30,35,40,45,50
    private Integer nCooldownTheshold = 15; // 10,15,20,25,30

    private SeekBar sbCPUTemp, sbBatteryTemp, sbCooldown;
    private TextView tvCPUMaxTemp, tvBatteryMaxTemp, tvCooldown;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ProviderManager.generate();

        Button bSave;
        EditText edPool, edPort, edMiningGoal;
        Spinner spPool;

        SeekBar sbCores;
        TextView tvCoresNb, tvCoresMax;

        Switch swDisableTempControl, swPauseOnBattery, swKeepScreenOnWhenMining;

        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        Context appContext = MainActivity.getContextOfApplication();
        bSave = view.findViewById(R.id.saveSettings);

        tilAddress = view.findViewById(R.id.addressIL);
        edAddress = view.findViewById(R.id.address);
        edPool = view.findViewById(R.id.pool);
        edPort = view.findViewById(R.id.port);
        edUsernameparameters = view.findViewById(R.id.usernameparameters);
        edWorkerName = view.findViewById(R.id.workername);

        edMiningGoal = view.findViewById(R.id.mininggoal);

        Button bQrCode = view.findViewById(R.id.btnQrCode);

        spPool = view.findViewById(R.id.spinnerPool);

        sbCores = view.findViewById(R.id.seekbarcores);
        tvCoresNb = view.findViewById(R.id.coresnb);
        tvCoresMax = view.findViewById(R.id.coresmax);

        sbCPUTemp = view.findViewById(R.id.seekbarcputemperature);
        tvCPUMaxTemp = view.findViewById(R.id.cpumaxtemp);

        sbBatteryTemp = view.findViewById(R.id.seekbarbatterytemperature);
        tvBatteryMaxTemp = view.findViewById(R.id.batterymaxtemp);

        sbCooldown = view.findViewById(R.id.seekbarcooldownthreshold);
        tvCooldown = view.findViewById(R.id.cooldownthreshold);

        swPauseOnBattery = view.findViewById(R.id.chkPauseOnBattery);
        swKeepScreenOnWhenMining = view.findViewById(R.id.chkKeepScreenOnWhenMining);
        swDisableTempControl = view.findViewById(R.id.chkAmaycOff);

        // Pool spinner
        PoolItem[] pools = ProviderManager.getPools();
        String[] description = new String[pools.length];
        for(int i = 0; i< pools.length;i++) {
            description[i] = pools[i].getKey();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(appContext, R.layout.spinner_text_color, description);
        spPool.setAdapter(adapter);

        ImageView imgSpinnerDown = view.findViewById(R.id.imgSpinnerDown);
        imgSpinnerDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spPool.performClick();
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
                    // inflate the layout of the popup window
                    View popupView = inflater.inflate(R.layout.warning_amayc, null);
                    Utils.showPopup(v, inflater, popupView);
                }

                enableAmaycControl(!checked);
            }
        });

        spPool.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                if (Config.read("init").equals("1")) {
                    edAddress.setText(Config.read("address"));
                    edUsernameparameters.setText(Config.read("usernameparameters"));
                    edWorkerName.setText(Config.read("workername"));
                }

                if (position == 0){
                    edPool.setText(Config.read("custom_pool"));
                    edPort.setText(Config.read("custom_port"));
                    return;
                }

                PoolItem poolItem = ProviderManager.getPoolById(position);

                if(poolItem != null){
                    edPool.setText(poolItem.getPool());
                    edPort.setText(poolItem.getPort());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        PoolItem poolItem = null;
        String poolSelected = Config.read("selected_pool");
        int sp = Config.DefaultPoolIndex;
        if (poolSelected.isEmpty()) {
            poolSelected = Integer.toString(sp);
        }
        poolItem = ProviderManager.getPoolById(poolSelected);

        if(poolItem == null) {
            poolSelected = Integer.toString(sp);
        }

        poolItem = ProviderManager.getPoolById(poolSelected);
        if (!Config.read("init").equals("1")) {
            poolSelected = Integer.toString(sp);
        }

        if(poolSelected.equals("0")) {
            edPool.setText(Config.read("custom_pool"));
            edPort.setText(Config.read("custom_port"));
        } else if(!Config.read("custom_port").isEmpty()) {
            assert poolItem != null;
            edPool.setText(poolItem.getKey());
            edPort.setText(Config.read("custom_port"));
        }else{
            Config.write("custom_pool","");
            Config.write("custom_port","");
            edPool.setText(poolItem.getKey());
            edPort.setText(poolItem.getPort());
        }

        spPool.setSelection(Integer.parseInt(poolSelected));

        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

                Config.write("address", address);

                Config.write("usernameparameters", edUsernameparameters.getText().toString().trim());

                String workername = edWorkerName.getText().toString().trim();
                if(workername.isEmpty()) {
                    workername = Tools.getDeviceName();
                }

                Log.i(LOG_TAG,"Worker Name : " + workername);
                Config.write("workername", workername);
                edWorkerName.setText(workername);

                String key = spPool.getSelectedItem().toString();
                int selectedPosition = Config.DefaultPoolIndex;

                PoolItem[] pools = ProviderManager.getPools();
                for(int i = 0; i < pools.length; i++) {
                    PoolItem pi = pools[i];
                    if(pi.getKey().equals(key)) {
                        selectedPosition = i;
                        break;
                    }
                }

                PoolItem pi = ProviderManager.getPoolById(selectedPosition);
                String port = edPort.getText().toString().trim();
                String pool = edPool.getText().toString().trim();

                Log.i(LOG_TAG,"PoolType : " + pi.getPoolType());
                if(pi.getPoolType() == 0) {
                    Config.write("custom_pool", pool);
                    Config.write("custom_port", port);
                } else if(!port.isEmpty() && !pi.getPort().equals(port)) {
                    Config.write("custom_pool", "");
                    Config.write("custom_port", port);
                } else {
                    Config.write("custom_port", "");
                    Config.write("custom_pool", "");
                }

                Log.i(LOG_TAG,"SelectedPool : " + selectedPosition);
                Config.write("selected_pool", Integer.toString(selectedPosition));
                Config.write("cores", Integer.toString(sbCores.getProgress()));
                Config.write("threads", "1"); // Default value
                Config.write("intensity", "1"); // Default value

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

                Config.write("init", "1");

                Toast.makeText(appContext, "Settings Saved", Toast.LENGTH_SHORT).show();

                MainActivity main = (MainActivity) getActivity();
                assert main != null;
                main.stopMining();
                main.loadSettings();
                main.setTitle(getResources().getString(R.string.home));

                if (getFragmentManager() != null) {
                    for (Fragment fragment : getFragmentManager().getFragments()) {
                        if (fragment != null) {
                            getFragmentManager().beginTransaction().remove(fragment).commit();
                            ProviderManager.afterSave();
                        }
                    }
                }

                BottomNavigationView nav = main.findViewById(R.id.main_navigation);
                nav.getMenu().getItem(0).setChecked(true);

                main.updateStartButton();
                main.updateStatsListener();
                main.updateUI();
            }
        });

        edPool.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String poolAddress = edPool.getText().toString().trim();
                PoolItem[] pools = ProviderManager.getPools();
                int position  = spPool.getSelectedItemPosition();

                if (s.length() > 0) {
                    int poolSelected = 0;
                    for (int i = 1; i < pools.length; i++) {
                        PoolItem itemPool = pools[i];
                        if (itemPool.getPool().equals(poolAddress)) {
                            poolSelected = i;
                            break;
                        }
                    }
                    if(position != poolSelected){
                        spPool.setSelection(poolSelected);
                    }
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
                    Toast.makeText(appContext, "This version of Android does not support Qr Code.", Toast.LENGTH_LONG).show();
                }
            }
        });

        Button btnMineScala = view.findViewById(R.id.btnMineScala);
        btnMineScala.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.mine_scala);
                dialog.setCancelable(false);

                Button btnYes = dialog.findViewById(R.id.btnYes);
                btnYes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        edAddress.setText(Utils.SCALA_XLA_ADDRESS);

                        dialog.dismiss();
                    }
                });

                Button btnNo = dialog.findViewById(R.id.btnNo);
                btnNo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });

        Button btnHardwareHelp = view.findViewById(R.id.btnHardwareHelp);
        btnHardwareHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // inflate the layout of the popup window
                View popupView = inflater.inflate(R.layout.helper_hardware_settings, null);
                Utils.showPopup(v, inflater, popupView);
            }
        });

        Button btnAmaycWarning = view.findViewById(R.id.btnAmaycWarning);
        btnAmaycWarning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // inflate the layout of the popup window
                View popupView = inflater.inflate(R.layout.warning_amayc, null);
                Utils.showPopup(v, inflater, popupView);
            }
        });

        return view;
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
            Toast.makeText(appContext, e.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(appContext,"Camera Permission Denied.", Toast.LENGTH_LONG).show();
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
}