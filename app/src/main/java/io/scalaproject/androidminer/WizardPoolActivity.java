// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.text.DecimalFormat;

public class WizardPoolActivity extends BaseActivity {
    private static final String LOG_TAG = "WizardPoolActivity";

    private int selectedPoolIndex = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        setContentView(R.layout.fragment_wizard_pool);

        View view = findViewById(android.R.id.content).getRootView();

        RequestQueue queue = Volley.newRequestQueue(this);

        // Scala
        StringRequest stringRequest = new StringRequest(Request.Method.GET, "https://pool.scalaproject.io/api/pool/stats",
                response -> {
                    try {
                        Log.i(LOG_TAG, "response: " + response);

                        JSONObject obj = new JSONObject(response);
                        JSONObject objStats = obj.getJSONObject("pool_statistics");

                        TextView tvMinersGNTL = view.findViewById(R.id.minersScala);
                        tvMinersGNTL.setText(String.format("%s %s", objStats.getString("miners"), getResources().getString(R.string.miners)));

                        TextView tvHrGNTL = view.findViewById(R.id.hrScala);
                        float fHrGNTL = Utils.convertStringToFloat(objStats.getString("hashRate")) / 1000.0f;
                        tvHrGNTL.setText(String.format("%s kH/s", new DecimalFormat("##.#").format(fHrGNTL)));

                    } catch (Exception e) {
                        //Do nothing
                    }
                }
        , this::parseVolleyError);

        queue.add(stringRequest);

        // Miner.rocks
        stringRequest = new StringRequest(Request.Method.GET, "https://stellite.miner.rocks/api/stats",
                response -> {
                    try {
                        Log.i(LOG_TAG, "response: " + response);

                        JSONObject obj = new JSONObject(response);
                        JSONObject objConfig = obj.getJSONObject("config");
                        JSONObject objConfigPool = obj.getJSONObject("pool");

                        TextView tvMinersMR = view.findViewById(R.id.minersMR);
                        tvMinersMR.setText(String.format("%s %s", objConfigPool.getString("miners"), getResources().getString(R.string.miners)));

                        TextView tvHrMR = view.findViewById(R.id.hrMR);
                        float fHrMR = Utils.convertStringToFloat(objConfigPool.getString("hashrate")) / 1000.0f;
                        tvHrMR.setText(String.format("%s kH/s", new DecimalFormat("##.#").format(fHrMR)));

                    } catch (Exception e) {
                        //Do nothing
                    }
                }
                , this::parseVolleyError);

        queue.add(stringRequest);

        // Hero Miners
        stringRequest = new StringRequest(Request.Method.GET, "https://scala.herominers.com/api/stats",
                response -> {
                    try {
                        Log.i(LOG_TAG, "response: " + response);

                        JSONObject obj = new JSONObject(response);
                        JSONObject objConfig = obj.getJSONObject("config");
                        JSONObject objConfigPool = obj.getJSONObject("pool");
                        TextView tvMinersHM = view.findViewById(R.id.minersHM);
                        tvMinersHM.setText(String.format("%s %s", objConfigPool.getString("miners"), getResources().getString(R.string.miners)));

                        TextView tvHrHM = view.findViewById(R.id.hrHM);
                        float fHrHM = Utils.convertStringToFloat(objConfigPool.getString("hashrate")) / 1000.0f;
                        tvHrHM.setText(String.format("%s kH/s", new DecimalFormat("##.#").format(fHrHM)));

                    } catch (Exception e) {
                        //Do nothing
                    }
                }
                , this::parseVolleyError);

        queue.add(stringRequest);

        // GNTL
        stringRequest = new StringRequest(Request.Method.GET, "https://xla.pool.gntl.co.uk/api/pool/stats",
                response -> {
                    try {
                        Log.i(LOG_TAG, "response: " + response);

                        JSONObject obj = new JSONObject(response);
                        JSONObject objStats = obj.getJSONObject("pool_statistics");

                        TextView tvMinersGNTL = view.findViewById(R.id.minersGNTL);
                        tvMinersGNTL.setText(String.format("%s %s", objStats.getString("miners"), getResources().getString(R.string.miners)));

                        TextView tvHrGNTL = view.findViewById(R.id.hrGNTL);
                        float fHrGNTL = Utils.convertStringToFloat(objStats.getString("hashRate")) / 1000.0f;
                        tvHrGNTL.setText(String.format("%s kH/s", new DecimalFormat("##.#").format(fHrGNTL)));

                    } catch (Exception e) {
                        //Do nothing
                    }
                }
                , this::parseVolleyError);

        queue.add(stringRequest);
    }

    private void parseVolleyError(VolleyError error) {
        // Do nothing
    }

    public void onClickScala(View view) {
        View view2 = findViewById(android.R.id.content).getRootView();

        selectedPoolIndex = 1;

        LinearLayout llScala = view2.findViewById(R.id.llScala);
        int bottom = llScala.getPaddingBottom();
        int top = llScala.getPaddingTop();
        int right = llScala.getPaddingRight();
        int left = llScala.getPaddingLeft();
        llScala.setBackgroundResource(R.drawable.corner_radius_lighter_border_blue);
        llScala.setPadding(left, top, right, bottom);

        LinearLayout llMR = view2.findViewById(R.id.llMR);
        bottom = llMR.getPaddingBottom();
        top = llMR.getPaddingTop();
        right = llMR.getPaddingRight();
        left = llMR.getPaddingLeft();
        llMR.setBackgroundResource(R.drawable.corner_radius_lighter);
        llMR.setPadding(left, top, right, bottom);

        LinearLayout llHM = view2.findViewById(R.id.llHM);
        bottom = llHM.getPaddingBottom();
        top = llHM.getPaddingTop();
        right = llHM.getPaddingRight();
        left = llHM.getPaddingLeft();
        llHM.setBackgroundResource(R.drawable.corner_radius_lighter);
        llHM.setPadding(left, top, right, bottom);

        LinearLayout llGNTL = view2.findViewById(R.id.llGNTL);
        bottom = llGNTL.getPaddingBottom();
        top = llGNTL.getPaddingTop();
        right = llGNTL.getPaddingRight();
        left = llGNTL.getPaddingLeft();
        llGNTL.setBackgroundResource(R.drawable.corner_radius_lighter);
        llGNTL.setPadding(left, top, right, bottom);
    }

    public void onClickMR(View view) {
        View view2 = findViewById(android.R.id.content).getRootView();

        selectedPoolIndex = 2;

        LinearLayout llScala = view2.findViewById(R.id.llScala);
        int bottom = llScala.getPaddingBottom();
        int top = llScala.getPaddingTop();
        int right = llScala.getPaddingRight();
        int left = llScala.getPaddingLeft();
        llScala.setBackgroundResource(R.drawable.corner_radius_lighter);
        llScala.setPadding(left, top, right, bottom);

        LinearLayout llMR = view2.findViewById(R.id.llMR);
        bottom = llMR.getPaddingBottom();
        top = llMR.getPaddingTop();
        right = llMR.getPaddingRight();
        left = llMR.getPaddingLeft();
        llMR.setBackgroundResource(R.drawable.corner_radius_lighter_border_blue);
        llMR.setPadding(left, top, right, bottom);

        LinearLayout llHM = view2.findViewById(R.id.llHM);
        bottom = llHM.getPaddingBottom();
        top = llHM.getPaddingTop();
        right = llHM.getPaddingRight();
        left = llHM.getPaddingLeft();
        llHM.setBackgroundResource(R.drawable.corner_radius_lighter);
        llHM.setPadding(left, top, right, bottom);

        LinearLayout llGNTL = view2.findViewById(R.id.llGNTL);
        bottom = llGNTL.getPaddingBottom();
        top = llGNTL.getPaddingTop();
        right = llGNTL.getPaddingRight();
        left = llGNTL.getPaddingLeft();
        llGNTL.setBackgroundResource(R.drawable.corner_radius_lighter);
        llGNTL.setPadding(left, top, right, bottom);
    }

    public void onClickHM(View view) {
        View view2 = findViewById(android.R.id.content).getRootView();

        selectedPoolIndex = 3;

        LinearLayout llScala = view2.findViewById(R.id.llScala);
        int bottom = llScala.getPaddingBottom();
        int top = llScala.getPaddingTop();
        int right = llScala.getPaddingRight();
        int left = llScala.getPaddingLeft();
        llScala.setBackgroundResource(R.drawable.corner_radius_lighter);
        llScala.setPadding(left, top, right, bottom);

        LinearLayout llMR = view2.findViewById(R.id.llMR);
        bottom = llMR.getPaddingBottom();
        top = llMR.getPaddingTop();
        right = llMR.getPaddingRight();
        left = llMR.getPaddingLeft();
        llMR.setBackgroundResource(R.drawable.corner_radius_lighter);
        llMR.setPadding(left, top, right, bottom);

        LinearLayout llHM = view2.findViewById(R.id.llHM);
        bottom = llHM.getPaddingBottom();
        top = llHM.getPaddingTop();
        right = llHM.getPaddingRight();
        left = llHM.getPaddingLeft();
        llHM.setBackgroundResource(R.drawable.corner_radius_lighter_border_blue);
        llHM.setPadding(left, top, right, bottom);

        LinearLayout llGNTL = view2.findViewById(R.id.llGNTL);
        bottom = llGNTL.getPaddingBottom();
        top = llGNTL.getPaddingTop();
        right = llGNTL.getPaddingRight();
        left = llGNTL.getPaddingLeft();
        llGNTL.setBackgroundResource(R.drawable.corner_radius_lighter);
        llGNTL.setPadding(left, top, right, bottom);
    }

    public void onClickGNTL(View view) {
        View view2 = findViewById(android.R.id.content).getRootView();

        selectedPoolIndex = 4;

        LinearLayout llScala = view2.findViewById(R.id.llScala);
        int bottom = llScala.getPaddingBottom();
        int top = llScala.getPaddingTop();
        int right = llScala.getPaddingRight();
        int left = llScala.getPaddingLeft();
        llScala.setBackgroundResource(R.drawable.corner_radius_lighter);
        llScala.setPadding(left, top, right, bottom);

        LinearLayout llMR = view2.findViewById(R.id.llMR);
        bottom = llMR.getPaddingBottom();
        top = llMR.getPaddingTop();
        right = llMR.getPaddingRight();
        left = llMR.getPaddingLeft();
        llMR.setBackgroundResource(R.drawable.corner_radius_lighter);
        llMR.setPadding(left, top, right, bottom);

        LinearLayout llHM = view2.findViewById(R.id.llHM);
        bottom = llHM.getPaddingBottom();
        top = llHM.getPaddingTop();
        right = llHM.getPaddingRight();
        left = llHM.getPaddingLeft();
        llHM.setBackgroundResource(R.drawable.corner_radius_lighter);
        llHM.setPadding(left, top, right, bottom);

        LinearLayout llGNTL = view2.findViewById(R.id.llGNTL);
        bottom = llGNTL.getPaddingBottom();
        top = llGNTL.getPaddingTop();
        right = llGNTL.getPaddingRight();
        left = llGNTL.getPaddingLeft();
        llGNTL.setBackgroundResource(R.drawable.corner_radius_lighter_border_blue);
        llGNTL.setPadding(left, top, right, bottom);
    }

    public void onNext(View view) {
        Config.write("selected_pool", Integer.toString(selectedPoolIndex));

        startActivity(new Intent(WizardPoolActivity.this, WizardSettingsActivity.class));
        finish();
    }
}