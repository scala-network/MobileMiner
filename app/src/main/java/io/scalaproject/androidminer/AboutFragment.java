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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Map;

public class AboutFragment extends Fragment {

    private Button bDonate;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false  );

        bDonate = view.findViewById(R.id.donate);

        TextView tvWebsite;
        TextView tvGithub;
        TextView tvDiscord;
        TextView tvEmail;
        TextView tvSystemInfo;
        TextView tvMine2gether;
        TextView tvMonerominer;
        TextView tvFontAwesome;

        tvWebsite = view.findViewById(R.id.websiteURL);
        tvGithub = view.findViewById(R.id.githubURL);
        tvDiscord = view.findViewById(R.id.discordURL);
        tvEmail = view.findViewById(R.id.email);
        tvSystemInfo = view.findViewById(R.id.systemInfo);
        tvMine2gether = view.findViewById(R.id.Mine2getherURL);
        tvMonerominer = view.findViewById(R.id.MoneroMinerURL);
        tvFontAwesome = view.findViewById(R.id.FontAwesomeURL);

        tvWebsite.setText(Html.fromHtml(getString(R.string.websiteLink)));
        tvWebsite.setMovementMethod(LinkMovementMethod.getInstance());

        tvGithub.setText(Html.fromHtml(getString(R.string.githubLink)));
        tvGithub.setMovementMethod(LinkMovementMethod.getInstance());

        tvDiscord.setText(Html.fromHtml(getString(R.string.discordLink)));
        tvDiscord.setMovementMethod(LinkMovementMethod.getInstance());

        tvEmail.setText(Html.fromHtml(getString(R.string.emailLink)));
        tvEmail.setMovementMethod(LinkMovementMethod.getInstance());

        String cpuinfo = Config.read("CPUINFO");
        if(cpuinfo.isEmpty()) {
            try {
                Map<String, String> m = Tools.getCPUInfo();

                cpuinfo = "ABI: " + Tools.getABI() + "\n";
                for (Map.Entry<String, String> pair : m.entrySet()) {
                    cpuinfo += pair.getKey() + ": " + pair.getValue() + "\n";
                }

            } catch (Exception e) {
                cpuinfo = "";
            }

            Config.write("CPUINFO", cpuinfo);
        }

        tvSystemInfo.setText(cpuinfo);

        tvMine2gether.setText(Html.fromHtml(getString(R.string.Mine2getherLink)));
        tvMine2gether.setMovementMethod(LinkMovementMethod.getInstance());

        tvMonerominer.setText(Html.fromHtml(getString(R.string.MoneroMinerLink)));
        tvMonerominer.setMovementMethod(LinkMovementMethod.getInstance());

        tvFontAwesome.setText(Html.fromHtml(getString(R.string.FontAwesomeLink)));
        tvFontAwesome.setMovementMethod(LinkMovementMethod.getInstance());

        bDonate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getString(R.string.DonateURL));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        return view;
    }
}
