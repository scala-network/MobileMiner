// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
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

        bStatCheckOnline = view.findViewById(R.id.checkstatsonline);

        checkValidState();

        statsListener = new ProviderListenerInterface(){
            public void onStatsChange(Data d) {
                if (!checkValidState()) {
                    return;
                }

                PoolItem pm = PoolManager.getSelectedPool();

                // Network
                TextView tvNetworkHashrate = view.findViewById(R.id.hashratenetwork);
                tvNetworkHashrate.setText(d.getNetwork().hashrate);

                TextView tvNetworkDifficulty = view.findViewById(R.id.difficultypool);
                tvNetworkDifficulty.setText(d.getNetwork().difficulty);

                TextView tvNetworkBlocks = view.findViewById(R.id.lastblocknetwork);
                tvNetworkBlocks.setText(d.getNetwork().lastBlockTime);

                TextView tvNetworkHeight = view.findViewById(R.id.height);
                tvNetworkHeight.setText(d.getNetwork().lastBlockHeight);

                TextView tvNetworkRewards = view.findViewById(R.id.rewards);
                tvNetworkRewards.setText(d.getNetwork().lastRewardAmount);

                // Pool
                TextView tvPoolURL = view.findViewById(R.id.poolurl);
                tvPoolURL.setText(pm.getPool());

                TextView tvPoolHashrate = view.findViewById(R.id.hashratepool);
                tvPoolHashrate.setText(d.getPool().hashrate);

                TextView tvPoolDifficulty = view.findViewById(R.id.difficultypool);
                tvPoolDifficulty.setText(d.getPool().difficulty);

                TextView tvPoolBlocks = view.findViewById(R.id.lastblockpool);
                tvPoolBlocks.setText(d.getPool().lastBlockTime);

                TextView tvPoolLastBlock = view.findViewById(R.id.blockspool);
                tvPoolLastBlock.setText(d.getPool().blocks);

                // Address
                String wallet = Config.read("address");
                String addresspretty = wallet.substring(0, 7) + "..." + wallet.substring(wallet.length() - 7);

                TextView tvWalletAddress = view.findViewById(R.id.walletaddress);
                tvWalletAddress.setText(addresspretty);

                String sHashrate = d.getMiner().hashrate;
                sHashrate.replace("H", "");
                sHashrate.trim();
                TextView tvAddressHashrate = view.findViewById(R.id.hashrateminer);
                tvAddressHashrate.setText(sHashrate);

                TextView tvAddressLastShare = view.findViewById(R.id.lastshareminer);
                tvAddressLastShare.setText(d.getMiner().lastShare);

                TextView tvAddressBlocks = view.findViewById(R.id.blocksminedminer);
                tvAddressBlocks.setText(d.getMiner().blocks);

                String sBalance = d.getMiner().balance;
                sBalance.replace("XLA", "");
                sBalance.trim();
                TextView tvBalance = view.findViewById(R.id.balance);
                tvBalance.setText(sBalance);

                String sPaid = d.getMiner().balance;
                sPaid.replace("XLA", "");
                sPaid.trim();
                TextView tvPaid = view.findViewById(R.id.paid);
                tvPaid.setText(sPaid);

                /*if(pm.getPoolType() == 1) {
                    dataParsedAddress+= "Shares Accepted: " + d.getMiner().blocks;
                } else {
                    dataParsedAddress+= "Blocks Found: " + d.getMiner().blocks;

                }*/

                String statsUrl = pm.getStatsURL();
                String statsUrlWallet = statsUrl + "?wallet=" + wallet;

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

    private void enableOnlineStats(boolean enable) {
        Drawable buttonDrawable = bStatCheckOnline.getBackground();
        buttonDrawable = DrawableCompat.wrap(buttonDrawable);
        buttonDrawable = bStatCheckOnline.getBackground();
        buttonDrawable = DrawableCompat.wrap(buttonDrawable);

        bStatCheckOnline.setEnabled(enable);

        if (enable) {
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_lighter));
            bStatCheckOnline.setBackground(buttonDrawable);
            bStatCheckOnline.setTextColor(getResources().getColor(R.color.c_white));
        }
        else {
            DrawableCompat.setTint(buttonDrawable, getResources().getColor(R.color.bg_black));
            bStatCheckOnline.setBackground(buttonDrawable);
            bStatCheckOnline.setTextColor(getResources().getColor(R.color.c_black));
        }
    }

    private boolean checkValidState() {

        PoolItem pi = PoolManager.getSelectedPool();

        if(Config.read("address").equals("")) {
            Toast.makeText(getContext(),"Wallet address is empty", Toast.LENGTH_LONG);
            enableOnlineStats(false);
            return false;
        }

        if (Config.read("init").equals("1") == false || pi == null) {
            Toast.makeText(getContext(),"Start mining to view statistics", Toast.LENGTH_LONG);
            enableOnlineStats(false);
            return false;
        }

        if (pi.getPoolType() == 0) {
            Toast.makeText(getContext(),"Statistics are not available for custom pools", Toast.LENGTH_LONG);
            enableOnlineStats(false);
            return false;
        }

        enableOnlineStats(true);

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


