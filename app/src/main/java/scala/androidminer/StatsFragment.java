// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.

package scala.androidminer;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class StatsFragment extends Fragment {

    private static final String LOG_TAG = "MiningSvc";

    public static String wallet;
    public static String apiUrl;
    public static String apiUrlMerged;
    public static String statsUrl;

    private TextView tvStatCheckOnline;

    private TextView data;
    private TextView dataNetwork;

    private fetchData.statsChangeListener statsListener;

    Timer timer;
    long delay = 30000L;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        statsListener = new fetchData.statsChangeListener() {
            public void onStatsChange(String addressStats, String networkStats) {
                data.setText(addressStats);
                dataNetwork.setText(networkStats);
            }
        };

        data = (TextView) view.findViewById(R.id.fetchdata);
        dataNetwork = (TextView) view.findViewById(R.id.fetchdataNetwork);
        tvStatCheckOnline = view.findViewById(R.id.statCheckOnline);

        if (!checkValidState()) {
            return view;
        }

        fetchData process = new fetchData();
        process.setStatsChangeListener(statsListener);
        process.execute();
        repeatTask();

        return view;
    }

    private boolean checkValidState() {

        String ps = PreferenceHelper.getName("PoolSelection");

        if (PreferenceHelper.getName("init").equals("1") == false || ps.isEmpty()) {
            data.setText("(start mining to view stats)");
            tvStatCheckOnline.setText("");
            return false;
        }

        PoolItem pi = Config.getPoolById(Integer.valueOf(ps));

        if (pi.getPoolType() == 0) {
            data.setText("(stats are not available for custom pools)");
            tvStatCheckOnline.setText("");
            return false;
        }

        wallet = PreferenceHelper.getName("address");
        apiUrl = pi.getPoolUrl();
        statsUrl = pi.getStatsURL();

        tvStatCheckOnline.setText(Html.fromHtml("<a href=\"" + statsUrl + "?wallet=" + wallet + "\">Check Stats Online</a>"));
        tvStatCheckOnline.setMovementMethod(LinkMovementMethod.getInstance());

        return true;
    }

    private void repeatTask() {

        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }

        timer = new Timer("Timer");

        if (!checkValidState()) {
            return;
        }

        TimerTask task = new TimerTask() {
            public void run() {
                fetchData process = new fetchData();
                process.setStatsChangeListener(statsListener);
                process.execute();
                repeatTask();
            }
        };

        timer.schedule(task, delay);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "onResume of StatsFragment");
        repeatTask();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(LOG_TAG, "OnPause of StatsFragment");
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

}


