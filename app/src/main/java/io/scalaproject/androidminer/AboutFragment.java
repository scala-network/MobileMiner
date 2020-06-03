// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class AboutFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView tvBuild, tvScala, tvMine2gether, tvMonerominer, tvMaterialDesign, tvFontAwesome;

        tvBuild = view.findViewById(R.id.build);

        tvScala = view.findViewById(R.id.ScalaURL);
        tvMine2gether = view.findViewById(R.id.Mine2getherURL);
        tvMonerominer = view.findViewById(R.id.MoneroMinerURL);
        tvMaterialDesign = view.findViewById(R.id.MaterialDesignURL);
        tvFontAwesome = view.findViewById(R.id.FontAwesomeURL);

        Button btnGitHub = view.findViewById(R.id.btnGitHub);
        btnGitHub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getResources().getString(R.string.githubLink));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        Button btnDiscord = view.findViewById(R.id.btnDiscord);
        btnDiscord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getResources().getString(R.string.discordLink));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        Button btnMedium = view.findViewById(R.id.btnMedium);
        btnMedium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getResources().getString(R.string.mediumLink));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        Button btnTwitter = view.findViewById(R.id.btnTwitter);
        btnTwitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getResources().getString(R.string.twitterLink));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        Button btnTelegram = view.findViewById(R.id.btnTelegram);
        btnTelegram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getResources().getString(R.string.telegramLink));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        Button btnDonationAddressesHelp = view.findViewById(R.id.btnDonationsHelp);
        btnDonationAddressesHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // inflate the layout of the popup window
                View popupView = inflater.inflate(R.layout.helper_donation_addresses, null);
                Utils.showPopup(v, inflater, popupView);
            }
        });

        Button btnDonateBTC = view.findViewById(R.id.btnDonateBTC);
        btnDonateBTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.copyToClipboard("Scala BTC Donation Address", Utils.SCALA_BTC_ADDRESS);
                Toast.makeText(getContext(), getResources().getString(R.string.donationadressbtc_copied), Toast.LENGTH_SHORT).show();
            }
        });

        Button btnDonateETH = view.findViewById(R.id.btnDonateETH);
        btnDonateETH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.copyToClipboard("Scala ETH Donation Address", Utils.SCALA_ETH_ADDRESS);
                Toast.makeText(getContext(), getResources().getString(R.string.donationadresseth_copied), Toast.LENGTH_SHORT).show();
            }
        });

        Button btnDonateXLA = view.findViewById(R.id.btnDonateXLA);
        btnDonateXLA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.copyToClipboard("Scala XLA Donation Address", Utils.SCALA_XLA_ADDRESS);
                Toast.makeText(getContext(), getResources().getString(R.string.donationadressxla_copied), Toast.LENGTH_SHORT).show();
            }
        });

        StringBuilder cpuinfo = new StringBuilder(Config.read("CPUINFO").trim());
        if(cpuinfo.length() == 0) {
            try {
                Map<String, String> m = Tools.getCPUInfo();

                cpuinfo = new StringBuilder("ABI: " + Tools.getABI() + "\n");
                for (Map.Entry<String, String> pair : m.entrySet()) {
                    cpuinfo.append(pair.getKey()).append(": ").append(pair.getValue()).append("\n");
                }
            } catch (Exception e) {
                cpuinfo = new StringBuilder();
            }

            Config.write("CPUINFO", cpuinfo.toString().trim());
        }

        // Convert build time to readable date
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(BuildConfig.BUILD_TIME);
        String build_time = DateFormat.getDateInstance(DateFormat.LONG).format(calendar.getTime());

        tvBuild.setText(BuildConfig.VERSION_NAME + " (" + build_time + ")");

        tvScala.setText(Html.fromHtml(getString(R.string.ScalaLink)));
        tvScala.setMovementMethod(LinkMovementMethod.getInstance());

        tvMine2gether.setText(Html.fromHtml(getString(R.string.Mine2getherLink)));
        tvMine2gether.setMovementMethod(LinkMovementMethod.getInstance());

        tvMonerominer.setText(Html.fromHtml(getString(R.string.MoneroMinerLink)));
        tvMonerominer.setMovementMethod(LinkMovementMethod.getInstance());

        tvMaterialDesign.setText(Html.fromHtml(getString(R.string.MaterialDesignLink)));
        tvMaterialDesign.setMovementMethod(LinkMovementMethod.getInstance());

        tvFontAwesome.setText(Html.fromHtml(getString(R.string.FontAwesomeLink)));
        tvFontAwesome.setMovementMethod(LinkMovementMethod.getInstance());

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH);
        String build_time_debug = formatter.format(calendar.getTime());

        String sDebugInfo = "Version Code: " + BuildConfig.VERSION_CODE + "\n" +
                "Version Name: " + BuildConfig.VERSION_NAME + "\n" +
                "Build Time: " + build_time_debug + "\n\n" +
                "Device Name: " + Tools.getDeviceName() + "\n" +
                "CPU Info: " + cpuinfo;

        Button btnDebugInfo = view.findViewById(R.id.btnDebugInfo);
        btnDebugInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.copyToClipboard("Scala Miner Debug Info", sDebugInfo);
                Toast.makeText(getContext(), getResources().getString(R.string.debuginfo_copied), Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}