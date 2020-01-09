// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.

package scala.androidminer;

import android.content.Intent;
import android.support.design.widget.NavigationView;
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


public class SettingsFragment extends Fragment {

    private static final String LOG_TAG = "MiningSvc";

    private EditText edPass;
    private EditText edUser;
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

        PoolItem[] pools = Config.settings.getPools();
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

        if (PreferenceHelper.getName("cores").equals("") == true) {
            npCores.setValue(suggested);
        } else {
            npCores.setValue(Integer.parseInt(PreferenceHelper.getName("cores")));
        }

        if (PreferenceHelper.getName("threads").equals("") == true) {
            npThreads.setValue(1);
        } else {
            npThreads.setValue(Integer.parseInt(PreferenceHelper.getName("threads")));
        }

        if (PreferenceHelper.getName("intensity").equals("") == true) {
            npIntensity.setValue(1);
        } else {
            npIntensity.setValue(Integer.parseInt(PreferenceHelper.getName("intensity")));
        }

        boolean checkStatus = (PreferenceHelper.getName("pauseonbattery").equals("1") == true);
        chkPauseOnBattery.setChecked(checkStatus);

        if (PreferenceHelper.getName("address").equals("") == false) {
            edUser.setText(PreferenceHelper.getName("address"));
        }

        if (PreferenceHelper.getName("pass").equals("") == false) {
            edPass.setText(PreferenceHelper.getName("pass"));
        }

        PoolItem poolItem = null;
        String poolSelected = PreferenceHelper.getName("selected_pool");
        int sp = Config.settings.defaultPoolIndex;
        if (poolSelected.equals("") == false) {
            poolSelected = String.valueOf(sp);
        }

        poolItem = Config.getPoolById(poolSelected);

        if(poolItem == null) {
            poolSelected = String.valueOf(sp);
        }

        poolItem = Config.getPoolById(poolSelected);

        if (PreferenceHelper.getName("init").equals("1") == false) {
            poolSelected = String.valueOf(sp);
            edUser.setText(Config.settings.defaultWallet);
            edPass.setText(Config.settings.defaultPassword);
        }

        if(poolSelected.equals("0")) {
            edPool.setText(PreferenceHelper.getName("custom_pool"));
            edPort.setText(PreferenceHelper.getName("custom_port"));
        } else if(!PreferenceHelper.getName("custom_port").isEmpty()) {
            edPool.setText(poolItem.getKey());
            edPort.setText(PreferenceHelper.getName("custom_port"));
        }else{
            PreferenceHelper.setName("custom_pool","");
            PreferenceHelper.setName("custom_port","");
            edPool.setText(poolItem.getKey());
            edPort.setText(poolItem.getPort());
        }

        spPool.setSelection(Integer.valueOf(poolSelected));

        spPool.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {

                if (PreferenceHelper.getName("init").equals("1") == true) {
                    edUser.setText(PreferenceHelper.getName("address"));
                    edPass.setText(PreferenceHelper.getName("password"));
                }

                if (position == 0){
                    edPool.setText(PreferenceHelper.getName("custom_pool"));
                    edPort.setText(PreferenceHelper.getName("custom_port"));
                    return;
                }

                PoolItem poolItem = Config.getPoolById(position);
                if(poolItem != null){
                    edPool.setText(poolItem.getPool());
                    edPort.setText(poolItem.getPort());
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {
            }

        });

        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                PreferenceHelper.setName("address", edUser.getText().toString().trim());
                PreferenceHelper.setName("pass", edPass.getText().toString().trim());
                int seletedPosition = spPool.getSelectedItemPosition();
                String port = edPort.getText().toString().trim();
                String pool = edPool.getText().toString().trim();
                if(seletedPosition == 0) {
                    PreferenceHelper.setName("custom_pool", pool);
                    PreferenceHelper.setName("custom_port", port);
                } else if(!port.equals(Config.getPoolById(seletedPosition).getPort())) {
                    PreferenceHelper.setName("custom_pool", "");
                    PreferenceHelper.setName("custom_port", port);
                } else {
                    PreferenceHelper.setName("custom_port", "");
                    PreferenceHelper.setName("custom_pool", "");
                }

                PreferenceHelper.setName("selected_pool", String.valueOf(seletedPosition));
                PreferenceHelper.setName("cores", Integer.toString(npCores.getValue()));
                PreferenceHelper.setName("threads", Integer.toString(npThreads.getValue()));
                PreferenceHelper.setName("intensity", Integer.toString(npIntensity.getValue()));

                PreferenceHelper.setName("pauseonbattery", (chkPauseOnBattery.isChecked() ? "1" : "0"));

                PreferenceHelper.setName("init", "1");

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
                PoolItem[] pools = Config.getPools();
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
                Intent intent = new Intent(appContext, QrCodeScannerActivity.class);
                startActivity(intent);
            }
        });

            return view;
    }

    public void updateAddress() {
        if (PreferenceHelper.getName("address").equals("") == false && edUser != null) {
            edUser.setText(PreferenceHelper.getName("address"));
        }
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