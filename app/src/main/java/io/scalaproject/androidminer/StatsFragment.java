// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.scalaproject.androidminer.api.ProviderData;
import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.IProviderListener;
import io.scalaproject.androidminer.api.ProviderManager;

public class StatsFragment extends Fragment {

    private static final String LOG_TAG = "MiningSvc";

    private TextView tvViewStatsOnline;

    protected IProviderListener statsListener;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        statsListener = new IProviderListener() {
            public void onStatsChange(ProviderData d) {
                updateFields(d, view);
            }

            @Override
            public boolean onEnabledRequest() {
                return checkValidState();
            }
        };

        tvViewStatsOnline = view.findViewById(R.id.checkstatsonline);
        tvViewStatsOnline.setPaintFlags(tvViewStatsOnline.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvViewStatsOnline.setEnabled(false);
        tvViewStatsOnline.setTextColor(getResources().getColor(R.color.c_grey));

        ProviderManager.request.setListener(statsListener).start();
        ProviderManager.afterSave();
        updateFields(ProviderManager.data, view);

        return view;
    }

    private void updateFields(ProviderData d, View view) {
        if(view == null || view.getContext() == null)
            return;

        if(d.isNew) {
            enableOnlineStats(false);
            return;
        }

        PoolItem pm = ProviderManager.getSelectedPool();

        // Network

        TextView tvNetworkHashrate = view.findViewById(R.id.hashratenetwork);
        tvNetworkHashrate.setText(d.network.hashrate.isEmpty() ? "n/a" : d.network.hashrate);

        TextView tvNetworkDifficulty = view.findViewById(R.id.difficultynetwork);
        tvNetworkDifficulty.setText(d.network.difficulty.isEmpty() ? "n/a" : d.network.difficulty);

        TextView tvNetworkBlocks = view.findViewById(R.id.lastblocknetwork);
        tvNetworkBlocks.setText(d.network.lastBlockTime.isEmpty() ? "n/a" : d.network.lastBlockTime);

        TextView tvNetworkHeight = view.findViewById(R.id.height);
        tvNetworkHeight.setText(d.network.lastBlockHeight.isEmpty() ? "n/a" : d.network.lastBlockHeight);

        TextView tvNetworkRewards = view.findViewById(R.id.rewards);
        tvNetworkRewards.setText(d.network.lastRewardAmount.isEmpty() ? "n/a" : d.network.lastRewardAmount);

        // Pool

        TextView tvPoolURL = view.findViewById(R.id.poolurl);
        tvPoolURL.setText(pm.getPool() == null ? "" : pm.getPool());

        TextView tvPoolHashrate = view.findViewById(R.id.hashratepool);
        tvPoolHashrate.setText(d.pool.hashrate.isEmpty() ? "n/a" : d.pool.hashrate);

        TextView tvPoolDifficulty = view.findViewById(R.id.difficultypool);
        tvPoolDifficulty.setText(d.pool.difficulty.isEmpty() ? "n/a" : d.pool.difficulty);

        TextView tvPoolBlocks = view.findViewById(R.id.lastblockpool);
        tvPoolBlocks.setText(d.pool.lastBlockTime.isEmpty() ? "n/a" : d.pool.lastBlockTime);

        TextView tvPoolLastBlock = view.findViewById(R.id.blockspool);
        tvPoolLastBlock.setText(d.pool.blocks.isEmpty() ? "n/a" : d.pool.blocks);

        // Address

        String wallet = Config.read("address");
        String prettyaddress = wallet.substring(0, 7) + "..." + wallet.substring(wallet.length() - 7);

        TextView tvWalletAddress = view.findViewById(R.id.walletaddress);
        tvWalletAddress.setText(prettyaddress);

        String sHashrate = d.miner.hashrate;
        if(sHashrate != null) {
            sHashrate = sHashrate.replace("H", "").trim();
            TextView tvAddressHashrate = view.findViewById(R.id.hashrateminer);
            tvAddressHashrate.setText(sHashrate);

            TextView tvAddressLastShare = view.findViewById(R.id.lastshareminer);
            tvAddressLastShare.setText(d.miner.lastShare.isEmpty() ? "n/a" : d.miner.lastShare);

            TextView tvAddressBlocks = view.findViewById(R.id.blocksminedminer);
            tvAddressBlocks.setText(d.miner.blocks.isEmpty() ? "n/a" : d.miner.blocks);

            String sBalance = d.miner.balance.replace("XLA", "").trim();
            TextView tvBalance = view.findViewById(R.id.balance);
            tvBalance.setText(sBalance);

            String sPaid = d.miner.paid.replace("XLA", "").trim();
            TextView tvPaid = view.findViewById(R.id.paid);
            tvPaid.setText(sPaid);
        }

        enableOnlineStats(true);

        String statsUrlWallet = pm.getStatsURL() + "?wallet=" + wallet;
        tvViewStatsOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(statsUrlWallet);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }

    private void enableOnlineStats(boolean enable) {
        tvViewStatsOnline.setEnabled(enable);

        if (enable) {
            tvViewStatsOnline.setTextColor(getResources().getColor(R.color.c_blue));
        }
        else {
            tvViewStatsOnline.setTextColor(getResources().getColor(R.color.c_grey));
        }
    }

    public boolean checkValidState() {
        if(getContext() == null)
            return false;

        if(Config.read("address").equals("")) {
            Toast.makeText(getContext(),"Wallet address is empty.", Toast.LENGTH_LONG).show();
            enableOnlineStats(false);
            return false;
        }

        PoolItem pi = ProviderManager.getSelectedPool();

        if (!Config.read("init").equals("1") || pi == null) {
            Toast.makeText(getContext(),"Start mining to view statistics.", Toast.LENGTH_LONG).show();
            enableOnlineStats(false);
            return false;
        }

        if (pi.getPoolType() == 0) {
            Toast.makeText(getContext(),"Statistics are not available for custom pools.", Toast.LENGTH_LONG).show();
            enableOnlineStats(false);
            return false;
        }

        enableOnlineStats(true);

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        ProviderManager.request.setListener(statsListener).start();
    }

    @Override
    public void onPause() {
        super.onPause();
        enableOnlineStats(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        enableOnlineStats(false);
    }
}