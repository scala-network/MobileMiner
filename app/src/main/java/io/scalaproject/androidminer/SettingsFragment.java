// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.NumberPicker;
import java.util.Arrays;

import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.PoolManager;


public class SettingsFragment extends Fragment {

    private static final String LOG_TAG = "MiningSvc";

    private EditText edPass;
    private TextView edUser;
    private Button  qrButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Button click;
        EditText edPool, edPort;
        Spinner spPool;


        NumberPicker npCores;
        NumberPicker npThreads;
        NumberPicker npIntensity;

        PoolSpinAdapter poolAdapter;

        CheckBox chkPauseOnBattery;

        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        Context appContext = MainActivity.getContextOfApplication();
        click = view.findViewById(R.id.saveSettings);

        edUser = view.findViewById(R.id.username);
        edPool = view.findViewById(R.id.pool);
        edPort = view.findViewById(R.id.port);
        edPass = view.findViewById(R.id.pass);

        qrButton = view.findViewById(R.id.buttonQrReader);

        spPool = view.findViewById(R.id.poolSpinner);

        npCores = view.findViewById(R.id.cores);
        npThreads = view.findViewById(R.id.threads);
        npIntensity = view.findViewById(R.id.intensity);

        chkPauseOnBattery = view.findViewById(R.id.chkPauseOnBattery);

        PoolItem[] pools = PoolManager.getPools();
        String[] description = new String[pools.length];
        for(int i =0; i< pools.length;i++) {
            description[i] = pools[i].getKey();
        }

        poolAdapter = new PoolSpinAdapter(appContext, R.layout.spinner_text_color, description);
        spPool.setAdapter(poolAdapter);

        int cores = Runtime.getRuntime().availableProcessors();
        // write suggested cores usage into editText
        int suggested = cores / 2;
        if (suggested == 0) suggested = 1;
        ((TextView) view.findViewById(R.id.cpus)).setText(String.format("(%d)", cores));

        npCores.setMinValue(1);
        npCores.setMaxValue(cores);
        npCores.setWrapSelectorWheel(true);

        npThreads.setMinValue(1);
        npThreads.setMaxValue(3);
        npThreads.setWrapSelectorWheel(true);

        npIntensity.setMinValue(1);
        npIntensity.setMaxValue(5);
        npIntensity.setWrapSelectorWheel(true);

        if (Config.read("cores").equals("") == true) {
            npCores.setValue(suggested);
        } else {
            npCores.setValue(Integer.parseInt(Config.read("cores")));
        }

        if (Config.read("threads").equals("") == true) {
            npThreads.setValue(1);
        } else {
            npThreads.setValue(Integer.parseInt(Config.read("threads")));
        }

        if (Config.read("intensity").equals("") == true) {
            npIntensity.setValue(1);
        } else {
            npIntensity.setValue(Integer.parseInt(Config.read("intensity")));
        }

        boolean checkStatus = (Config.read("pauseonbattery").equals("1") == true);

        if(checkStatus){
            chkPauseOnBattery.setChecked(checkStatus);
        }

        if (Config.read("address").equals("") == false) {
            edUser.setText(Config.read("address"));
        }

        if (Config.read("pass").equals("") == false) {
            edPass.setText(Config.read("pass"));
        }

        spPool.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {

                if (Config.read("init").equals("1") == true) {
                    edUser.setText(Config.read("address"));
                    edPass.setText(Config.read("pass"));
                }

                if (position == 0){
                    edPool.setText(Config.read("custom_pool"));
                    edPort.setText(Config.read("custom_port"));
                    return;
                }

                PoolItem poolItem = PoolManager.getPoolById(position);

                if(poolItem != null){
                    edPool.setText(poolItem.getPool());
                    edPort.setText(poolItem.getPort());
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {
            }

        });

        PoolItem poolItem = null;
        String poolSelected = Config.read("selected_pool");
        int sp = Config.DefaultPoolIndex;
        if (poolSelected.equals("")) {
            poolSelected = Integer.toString(sp);
        }
        poolItem = PoolManager.getPoolById(poolSelected);

        if(poolItem == null) {
            poolSelected = Integer.toString(sp);
        }

        poolItem = PoolManager.getPoolById(poolSelected);

        if (Config.read("init").equals("1") == false) {
            poolSelected = Integer.toString(sp);
            edUser.setText(Config.DefaultWallet);
            edPass.setText(Config.DefaultPassword);
        }

        if(poolSelected.equals("0")) {
            edPool.setText(Config.read("custom_pool"));
            edPort.setText(Config.read("custom_port"));
        } else if(!Config.read("custom_port").equals("")) {
            edPool.setText(poolItem.getKey());
            edPort.setText(Config.read("custom_port"));
        }else{
            Config.write("custom_pool","");
            Config.write("custom_port","");
            edPool.setText(poolItem.getKey());
            edPort.setText(poolItem.getPort());
        }

        spPool.setSelection(Integer.valueOf(poolSelected));

        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String address = edUser.getText().toString().trim();
                if (!Utils.verifyAddress(address)) {
                    Toast.makeText(appContext, "Invalid wallet address.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Config.write("address", address);

                String password = edPass.getText().toString().trim();
                if(password.equals("")) {
                    password = Tools.getDeviceName();
                }

                Log.i(LOG_TAG,"Pssword : " + password);
                Config.write("pass", password);
                edPass.setText(password);
                String key = (String)spPool.getSelectedItem();

                int selectedPosition = Config.DefaultPoolIndex;
                PoolItem[] pools = PoolManager.getPools();
                for(int i = 0;i< pools.length;i++){
                    PoolItem pi = pools[i];
                    if(pi.getKey().equals(key)) {
                        selectedPosition = i;
                        break;
                    }
                }
                
                PoolItem pi = PoolManager.getPoolById(selectedPosition);
                String port = edPort.getText().toString().trim();
                String pool = edPool.getText().toString().trim();
                if(pi.getPoolType() == 4) {
                    Config.write("custom_pool", pool);
                    Config.write("custom_port", port);
                } else if(!port.equals("") && !pi.getPort().equals(port)) {
                    Config.write("custom_pool", "");
                    Config.write("custom_port", port);
                } else {
                    Config.write("custom_port", "");
                    Config.write("custom_pool", "");
                }

                Config.write("selected_pool", Integer.toString(selectedPosition));
                Config.write("cores", Integer.toString(npCores.getValue()));
                Config.write("threads", Integer.toString(npThreads.getValue()));
                Config.write("intensity", Integer.toString(npIntensity.getValue()));
                Config.write("pauseonbattery", (chkPauseOnBattery.isChecked() ? "1" : "0"));

                Config.write("init", "1");

                Toast.makeText(appContext, "Settings Saved", Toast.LENGTH_SHORT).show();

                MainActivity main = (MainActivity) getActivity();
                for (Fragment fragment : getFragmentManager().getFragments()) {
                    if (fragment != null) {
                        getFragmentManager().beginTransaction().remove(fragment).commit();
                    }
                }

                NavigationView nav = main.findViewById(R.id.nav_view);
                nav.getMenu().getItem(0).setChecked(true);
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
                PoolItem[] pools = PoolManager.getPools();
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


        qrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23) {
                    if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(appContext,"Camera Permission Denied",Toast.LENGTH_LONG);
                        return;
                    }
                }

                try {
                    Intent intent = new Intent(appContext, QrCodeScannerActivity.class);
                    startActivity(intent);
                }catch (Exception e) {
                    Toast.makeText(appContext,e.getMessage(),Toast.LENGTH_LONG);
                }

            }
        });

        return view;
    }

    public void updateAddress() {
        String address =  Config.read("address");
        if (edUser == null || address.equals("")) {
            return;
        }

        edUser.setText(address);
    }

    public class PoolSpinAdapter extends ArrayAdapter<String> {

        private Context context;
        private String[] values;

        public PoolSpinAdapter(Context c, int textViewResourceId, String[] values) {
            super(c, textViewResourceId, values);
            this.context = c;
            this.values = values;
        }

        @Override
        public int getCount() {
            return values.length;
        }

        @Override
        public String getItem(int position) {
            return values[position];
        }

        public int getPosition(String item){
            return Arrays.asList(values).indexOf(item);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            TextView label = (TextView) super.getView(position, convertView, parent);
            label.setText(values[position]);
            return label;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView label = (TextView) super.getDropDownView(position, convertView, parent);
            label.setText(values[position]);
            label.setPadding(5, 10, 5, 10);
            return label;
        }

    }

}