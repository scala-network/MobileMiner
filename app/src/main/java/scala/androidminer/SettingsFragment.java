// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.

package scala.androidminer;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

public class SettingsFragment extends Fragment {

    private static final String LOG_TAG = "MiningSvc";

    private EditText edPass;
    private EditText edUser;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Button click;
        EditText edPool;

        Spinner spPool;
        Spinner spAlgo;
        Spinner spMiner;

        NumberPicker npCores;
        NumberPicker npThreads;
        NumberPicker npIntensity;

        PoolSpinAdapter poolAdapter;
        AlgoSpinAdapter algoAdapter;

        CheckBox chkPauseOnBattery;

        final MinerSpinAdapter minerAdapter = new MinerSpinAdapter(MainActivity.contextOfApplication, R.layout.spinner_text_color, new ArrayList<MinerItem>());

        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        Context appContext = MainActivity.getContextOfApplication();
        click = view.findViewById(R.id.saveSettings);

        edUser = view.findViewById(R.id.username);
        edPool = view.findViewById(R.id.pool);
        edPass = view.findViewById(R.id.pass);

        spPool = view.findViewById(R.id.poolSpinner);
        spAlgo = view.findViewById(R.id.algoSpinner);
        spMiner = view.findViewById(R.id.minerSpinner);

        npCores = view.findViewById(R.id.cores);
        npThreads = view.findViewById(R.id.threads);
        npIntensity = view.findViewById(R.id.intensity);

        chkPauseOnBattery = view.findViewById(R.id.chkPauseOnBattery);

        poolAdapter = new PoolSpinAdapter(MainActivity.contextOfApplication, R.layout.spinner_text_color, Config.settings.getPools());
        spPool.setAdapter(poolAdapter);

        algoAdapter = new AlgoSpinAdapter(MainActivity.contextOfApplication, R.layout.spinner_text_color, Config.settings.getAlgos());
        spAlgo.setAdapter(algoAdapter);

        spMiner.setAdapter(minerAdapter);

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

        if (PreferenceHelper.getName("pauseonbattery").equals("1") == true) {
            chkPauseOnBattery.setChecked(true);
        }


        if (PreferenceHelper.getName("address").equals("") == false) {
            edUser.setText(PreferenceHelper.getName("address"));
        }

        if (PreferenceHelper.getName("pass").equals("") == false) {
            edPass.setText(PreferenceHelper.getName("pass"));
        }

        if (PreferenceHelper.getName("pool").equals("") == false) {
            edPool.setText(PreferenceHelper.getName("pool"));
            int n = poolAdapter.getCount();
            String poolAddress = PreferenceHelper.getName("pool");
            for (int i = 0; i < n; i++) {
                PoolItem itemPool = (PoolItem) poolAdapter.getItem(i);
                if (itemPool.getPool().equals(poolAddress)) {
                    if (itemPool.getAlgo().equals(PreferenceHelper.getName("algo"))) {
                        spPool.setSelection(i);

                    }
                    break;
                }
            }
        }

        if (PreferenceHelper.getName("algo").equals("") == false) {
            int n = algoAdapter.getCount();
            String selectedAlgo = PreferenceHelper.getName("algo");
            for (int i = 0; i < n; i++) {
                String itemAlgo = (String) algoAdapter.getItem(i).getAlgo();
                if (itemAlgo.equals(selectedAlgo)) {
                    spAlgo.setSelection(i);
                    break;
                }
            }
        }

        if (PreferenceHelper.getName("init").equals("1") == false) {
            spPool.setSelection(Config.settings.defaultPoolIndex);
            edUser.setText(Config.settings.defaultWallet);
            edPass.setText(Config.settings.defaultPassword);
        }

        spPool.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {

                PoolItem item = poolAdapter.getItem(position);

                if (PreferenceHelper.getName("init").equals("1") == true) {
                    edUser.setText(PreferenceHelper.getName("keyAddress-" + item.getKey()));
                    edPass.setText(PreferenceHelper.getName("keyPassword-" + item.getKey()));
                }

                if (position == 0) return;

                edPool.setText(item.getPool());

                int n = algoAdapter.getCount();
                String selectedCoinAlgo = item.getAlgo();

                for (int i = 0; i < n; i++) {
                    String s = (String) algoAdapter.getItem(i).getAlgo();
                    if (selectedCoinAlgo.equals(s)) {
                        spAlgo.setSelection(i);
                        break;
                    }
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {
            }
        });

        spAlgo.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {

                ArrayList<MinerItem> items = algoAdapter.getItem(spAlgo.getSelectedItemPosition()).getMiners();
                minerAdapter.addList(items);

                String selectedAlgo = algoAdapter.getItem(spAlgo.getSelectedItemPosition()).getAlgo();
                String selectedMiner = PreferenceHelper.getName("keyMiner-" + selectedAlgo);

                if (selectedMiner.equals("") == false) {

                    int n = minerAdapter.getCount();

                    for (int i = 0; i < n; i++) {
                        String itemMiner = (String) minerAdapter.getItem(i).getMiner();
                        if (itemMiner.equals(selectedMiner)) {
                            spMiner.setSelection(i);
                            break;
                        }
                    }

                } else {

                    spMiner.setSelection(0);

                    int n = minerAdapter.getCount();

                    String defaultMiner = algoAdapter.getItem(spAlgo.getSelectedItemPosition()).getDefaultMiner();

                    for (int i = 0; i < n; i++) {
                        String itemMiner = (String) minerAdapter.getItem(i).getMiner();
                        if (itemMiner.equals(defaultMiner)) {
                            spMiner.setSelection(i);
                            break;
                        }
                    }

                }

                String poolAddress = edPool.getText().toString();
                int n = poolAdapter.getCount();

                for (int i = 0; i < n; i++) {
                    PoolItem itemPool = (PoolItem) poolAdapter.getItem(i);
                    if (itemPool.getPool().equals(poolAddress)) {
                        if (itemPool.getAlgo().equals(selectedAlgo)) {
                            spPool.setSelection(i);
                            return;
                        }
                        break;
                    }
                }
                spPool.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {
            }
        });

        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                PreferenceHelper.setName("address", edUser.getText().toString().trim());
                PreferenceHelper.setName("pool", edPool.getText().toString().trim());
                PreferenceHelper.setName("pass", edPass.getText().toString().trim());

                AlgoItem selectedAlgoItem = (AlgoItem) spAlgo.getSelectedItem();
                MinerItem selectedMinerItem = (MinerItem) spMiner.getSelectedItem();
                PoolItem selectedPoolItem = (PoolItem) spPool.getSelectedItem();

                //save miner based on algo
                PreferenceHelper.setName("keyMiner-" + selectedAlgoItem.getAlgo(), selectedMinerItem.getMiner());
                PreferenceHelper.setName("minerAlgo", selectedMinerItem.getAlgo());
                PreferenceHelper.setName("miner", selectedMinerItem.getMiner());

                PreferenceHelper.setName("algo", selectedAlgoItem.getAlgo());
                PreferenceHelper.setName("assetExtension", selectedMinerItem.getAssetExtension());

                PreferenceHelper.setName("apiUrl", selectedPoolItem.getApiUrl());
                PreferenceHelper.setName("apiUrlMerged", selectedPoolItem.getApiUrlMerged());
                PreferenceHelper.setName("poolUrl", selectedPoolItem.getPoolUrl());
                PreferenceHelper.setName("statsUrl", selectedPoolItem.getStatsURL());
                PreferenceHelper.setName("startUrl", selectedPoolItem.getStartUrl());

                PreferenceHelper.setName("coin", selectedPoolItem.getCoin());

                PreferenceHelper.setName("keyAddress-" + selectedPoolItem.getKey(), edUser.getText().toString().trim());
                PreferenceHelper.setName("keyPassword-" + selectedPoolItem.getKey(), edPass.getText().toString().trim());

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
                //  getActivity().getFragmentManager().beginTransaction().remove(this).commit();
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
                String poolAddress = edPool.getText().toString();
                int n = poolAdapter.getCount();
                if (s.length() != 0) {
                    for (int i = 0; i < n; i++) {
                        PoolItem itemPool = (PoolItem) poolAdapter.getItem(i);
                        if (itemPool.getPool().equals(poolAddress)) {
                            if (itemPool.getAlgo().equals(algoAdapter.getItem(spAlgo.getSelectedItemPosition()).getAlgo())) {
                                spPool.setSelection(i);
                                return;
                            }
                            break;
                        }
                    }
                    spPool.setSelection(0);
                }
            }
        });

        return view;
    }

    public class PoolSpinAdapter extends ArrayAdapter<PoolItem> {

        private Context context;
        private PoolItem[] values;

        public PoolSpinAdapter(Context c, int textViewResourceId, PoolItem[] values) {
            super(c, textViewResourceId, values);
            this.context = c;
            this.values = values;
        }

        @Override
        public int getCount() {
            return values.length;
        }

        @Override
        public PoolItem getItem(int position) {
            return values[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            TextView label = (TextView) super.getView(position, convertView, parent);
            label.setText(values[position].getCoin());
            return label;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView label = (TextView) super.getDropDownView(position, convertView, parent);
            label.setText(values[position].getCoin());
            label.setPadding(5, 10, 5, 10);
            return label;
        }
    }

    public class AlgoSpinAdapter extends ArrayAdapter<AlgoItem> {

        private Context context;
        private AlgoItem[] values;

        public AlgoSpinAdapter(Context c, int textViewResourceId, AlgoItem[] values) {
            super(c, textViewResourceId, values);
            this.context = c;
            this.values = values;
        }

        @Override
        public int getCount() {
            return values.length;
        }

        @Override
        public AlgoItem getItem(int position) {
            return values[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            TextView label = (TextView) super.getView(position, convertView, parent);
            label.setText(values[position].getAlgo());
            return label;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView label = (TextView) super.getDropDownView(position, convertView, parent);
            label.setText(values[position].getAlgo());
            label.setPadding(5, 10, 5, 10);
            return label;
        }
    }

    public class MinerSpinAdapter extends ArrayAdapter<MinerItem> {

        private Context context;
        private ArrayList<MinerItem> values;

        public MinerSpinAdapter(Context c, int textViewResourceId, ArrayList<MinerItem> values) {
            super(c, textViewResourceId, values);
            this.context = c;
            this.values = values;
        }

        public void addList(ArrayList<MinerItem> list) {
            values.clear();
            for (MinerItem value : list) {
                values.add(value);
            }
            this.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return values.size();
        }

        @Override
        public MinerItem getItem(int position) {
            return values.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            TextView label = (TextView) super.getView(position, convertView, parent);
            label.setText(values.get(position).getMiner());
            return label;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView label = (TextView) super.getDropDownView(position, convertView, parent);
            label.setText(values.get(position).getMiner());
            label.setPadding(5, 10, 5, 10);
            return label;
        }
    }

    private void selectSpinnerValue(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
}