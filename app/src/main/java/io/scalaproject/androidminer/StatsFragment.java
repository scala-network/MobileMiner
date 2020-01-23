// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import io.scalaproject.androidminer.api.Data;
import io.scalaproject.androidminer.api.PoolManager;
import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderAbstract;
import io.scalaproject.androidminer.api.ProviderListenerInterface;

public class StatsFragment extends Fragment {

    private static final String LOG_TAG = "MiningSvc";

    private Button bStatCheckOnline;

    private TextView data;
    private TextView dataNetwork;

    Timer timer;
    long delay = 30000L;

    protected ProviderListenerInterface statsListener;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);
        /*data = (TextView) view.findViewById(R.id.fetchdata);
        dataNetwork = (TextView) view.findViewById(R.id.fetchdataNetwork);*/
        bStatCheckOnline = view.findViewById(R.id.checkstatsonline);

        statsListener = new ProviderListenerInterface(){
            public void onStatsChange(Data d) {
                if (!checkValidState()) {
                    return;
                }
                PoolItem pm = PoolManager.getSelectedPool();

                //@@TODO UI FOR DATA TO BE INSERTED
                String dataParsedNetwork = "Block Height: " + d.getNetwork().lastBlockHeight + "\n"
                    + "Difficulty: " + d.getNetwork().difficulty + "\n"
                    + "Last Block: " + d.getNetwork().lastBlockTime + "\n"
                    + "Last Reward: " + d.getNetwork().lastRewardAmount;

                String dataParsedAddress = "Hashrate: " + d.getMiner().hashrate + "\n"
                        + "Balance: " + d.getMiner().balance + "\n"
                        + "Paid: " + d.getMiner().paid + "\n"
                        + "Last Share: " + d.getMiner().lastShare + "\n";

                if(pm.getPoolType() == 1) {
                    dataParsedAddress+= "Shares Accepted: " + d.getMiner().blocks;
                } else {
                    dataParsedAddress+= "Blocks Found: " + d.getMiner().blocks;

                }

                //data.setText(dataParsedAddress);
                //dataNetwork.setText(dataParsedNetwork);

                String wallet = Config.read("address");
                String statsUrl = pm.getStatsURL();
                String statsUrlWallet = statsUrl + "?wallet=" + wallet;

                //tvStatCheckOnline.setText(Html.fromHtml("<a href=\"" + statsUrl + "?wallet=" + wallet + "\">Check Stats Online</a>"));
                //tvStatCheckOnline.setMovementMethod(LinkMovementMethod.getInstance());

                bStatCheckOnline.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Uri uri = Uri.parse(statsUrlWallet);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    }
                });
            }
        };

        if (!checkValidState()) {
            return view;
        }

        PoolItem pi = PoolManager.getSelectedPool();

        ProviderAbstract api = pi.getInterface();

        api.setStatsChangeListener(statsListener);
        api.execute();
        repeatTask();

        return view;
    }

    private boolean checkValidState() {

        PoolItem pi = PoolManager.getSelectedPool();

        if(Config.read("address").equals("")) {
            //data.setText("Wallet address is empty");
            bStatCheckOnline.setEnabled(false);
            return false;
        }

        if (Config.read("init").equals("1") == false || pi == null) {
            //data.setText("Start mining to view stats");
            bStatCheckOnline.setEnabled(false);
            return false;
        }

        if (pi.getPoolType() == 0) {
            Toast.makeText(getContext(),"Stats are not available for custom pools",Toast.LENGTH_LONG);
            bStatCheckOnline.setEnabled(false);
            return false;
        }

        bStatCheckOnline.setEnabled(true);

        return true;
    }

    private void repeatTask() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }

        if (!checkValidState()) {
            return;
        }

        timer = new Timer("Timer");

        TimerTask task = new TimerTask() {
            public void run() {
                ProviderAbstract process = PoolManager.getSelectedPool().getInterface();
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
        repeatTask();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }
}


