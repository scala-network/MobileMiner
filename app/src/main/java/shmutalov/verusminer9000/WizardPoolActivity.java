// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package shmutalov.verusminer9000;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.android.volley.VolleyError;

public class WizardPoolActivity extends BaseActivity {
    private static final String LOG_TAG = "WizardPoolActivity";

    private int selectedPoolIndex = 1;
    private LinearLayout[] layouts;

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
//
//        RequestQueue queue = Volley.newRequestQueue(this);

        // Hero Miners
//        stringRequest = new StringRequest(Request.Method.GET, "https://scala.herominers.com/api/stats",
//                response -> {
//                    try {
//                        Log.i(LOG_TAG, "response: " + response);
//
//                        JSONObject obj = new JSONObject(response);
//                        JSONObject objConfig = obj.getJSONObject("config");
//                        JSONObject objConfigPool = obj.getJSONObject("pool");
//                        TextView tvMinersHM = view.findViewById(R.id.minersLuckpoolNA);
//                        tvMinersHM.setText(String.format("%s %s", objConfigPool.getString("miners"), getResources().getString(R.string.miners)));
//
//                        TextView tvHrHM = view.findViewById(R.id.hrLuckpoolNA);
//                        float fHrHM = Utils.convertStringToFloat(objConfigPool.getString("hashrate")) / 1000.0f;
//                        tvHrHM.setText(String.format("%s kH/s", new DecimalFormat("##.#").format(fHrHM)));
//
//                    } catch (Exception e) {
//                        //Do nothing
//                    }
//                }
//                , this::parseVolleyError);
//
//        queue.add(stringRequest);

        layouts = new LinearLayout[]{
            view.findViewById(R.id.llVeruspool),
            view.findViewById(R.id.llAlphatechIT),
            view.findViewById(R.id.llLuckpoolNA),
            view.findViewById(R.id.llLuckpoolEU),
            view.findViewById(R.id.llLuckpoolAP),
        };
    }

    private void parseVolleyError(VolleyError error) {
        // Do nothing
    }

    public void onClickVerus(View view) {
        selectedPoolIndex = 1;
        setHover(selectedPoolIndex);
    }

    public void onClickAlphatechIT(View view) {
        selectedPoolIndex = 2;
        setHover(selectedPoolIndex);
    }

    public void onClickLPNA(View view) {
        selectedPoolIndex = 3;
        setHover(selectedPoolIndex);
    }

    public void onClickLPEU(View view) {
        selectedPoolIndex = 4;
        setHover(selectedPoolIndex);
    }

    public void onClickLPAP(View view) {
        selectedPoolIndex = 5;
        setHover(selectedPoolIndex);
    }

    private void setHover(int layoutId) {
        if (layoutId < 1 && layoutId > layouts.length)
        {
            Log.e(LOG_TAG, "Unknown layout selected");
            return;
        }

        for (int i = 0; i < layouts.length; i++) {
            if (i == layoutId - 1) {
                layouts[i].setBackgroundResource(R.drawable.corner_radius_lighter_border_blue);
            } else {
                layouts[i].setBackgroundResource(R.drawable.corner_radius_lighter);
            }
            //llLPAP.setPadding(left, top, right, bottom);
        }
    }

    public void onNext(View view) {
        Config.write("selected_pool", Integer.toString(selectedPoolIndex));

        startActivity(new Intent(WizardPoolActivity.this, WizardSettingsActivity.class));
        finish();
    }

    public void onSkip(View view) {
        Config.write("selected_pool", "0");

        startActivity(new Intent(WizardPoolActivity.this, WizardSettingsActivity.class));
        finish();
    }
}