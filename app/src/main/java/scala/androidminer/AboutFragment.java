// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.
package scala.androidminer;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Map;

public class AboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_about, container, false  );

        TextView tvWebsite;
        TextView tvGithub;
        TextView tvDiscord;
        TextView tvEmail;
        TextView tvSystemInfo;

        tvWebsite = view.findViewById(R.id.websiteURL);
        tvGithub = view.findViewById(R.id.githubURL);
        tvDiscord = view.findViewById(R.id.discordURL);
        tvEmail = view.findViewById(R.id.email);
        tvSystemInfo = view.findViewById(R.id.systemInfo);

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
        return view;

    }

}
