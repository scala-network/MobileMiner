// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2021 Scala
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.scalaproject.androidminer.dialogs.CreditsFragment;
import io.scalaproject.androidminer.dialogs.DonationsFragment;

public class AboutFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView tvBuild = view.findViewById(R.id.build);
        tvBuild.setText(BuildConfig.VERSION_NAME + " (" + Utils.getBuildTime() + ")");

        TextView tvScala = view.findViewById(R.id.tvScala);
        tvScala.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getString(R.string.ScalaLinkClean));
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        });

        LinearLayout llGetSupport = view.findViewById(R.id.llSupport);
        llGetSupport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SupportActivity.class);
                startActivity(intent);
            }
        });

        LinearLayout llCredits = view.findViewById(R.id.llCredits);
        llCredits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onShowCredits();
            }
        });

        LinearLayout llShare = view.findViewById(R.id.llShare);
        llShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onShareApp();
            }
        });

        LinearLayout llDonations = view.findViewById(R.id.llDonations);
        llDonations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onShowDonations();
            }
        });

        ImageView ivDiscord = view.findViewById(R.id.ivDiscord);
        ivDiscord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getResources().getString(R.string.discordLink));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        ImageView ivTelegram = view.findViewById(R.id.ivTelegram);
        ivTelegram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getResources().getString(R.string.telegramLink));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        ImageView ivMedium = view.findViewById(R.id.ivMedium);
        ivMedium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getResources().getString(R.string.mediumLink));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        ImageView ivTwitter = view.findViewById(R.id.ivTwitter);
        ivTwitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getResources().getString(R.string.twitterLink));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        ImageView ivReddit = view.findViewById(R.id.ivReddit);
        ivReddit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getResources().getString(R.string.twitterLink));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        return view;
    }

    private void onShowCredits() {
        CreditsFragment.display(getActivity().getSupportFragmentManager());
    }

    private void onShowDonations() {
        DonationsFragment.display(getActivity().getSupportFragmentManager());
    }

    private void onShareApp() {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String shareBody = "Did you you you can mine cryptocurrencies on any Android device? Download the app to start mining on your phone: http://mobileminer.scalaproject.io/";
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Scala Mobile Miner");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);

        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }
}