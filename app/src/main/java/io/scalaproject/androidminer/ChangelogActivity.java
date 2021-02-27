// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import io.scalaproject.androidminer.api.ChangelogItem;
import io.scalaproject.androidminer.widgets.ChangelogInfoAdapter;
import io.scalaproject.androidminer.widgets.Toolbar;

public class ChangelogActivity extends BaseActivity {
    private static final String LOG_TAG = "ChangelogActivity";

    static private final String DEFAULT_CHANGELOG_REPOSITORY = "https://raw.githubusercontent.com/scala-network/MobileMiner/master/fastlane/metadata/android/en-US/changelog/";

    private ChangelogInfoAdapter changelogAdapter;
    private final Set<ChangelogItem> allChangelogItems = new HashSet<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        setContentView(R.layout.fragment_changelog);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        toolbar.setOnButtonListener(new Toolbar.OnButtonListener() {
            @Override
            public void onButtonMain(int type) {
                if (type == Toolbar.BUTTON_MAIN_BACK) {
                    onBackPressed();
                }
            }

            @Override
            public void onButtonOptions(int type) {
                // Does nothing in this view
            }
        });

        toolbar.setTitle(getString(R.string.changelog));
        toolbar.setButtonMain(Toolbar.BUTTON_MAIN_BACK);
        toolbar.setButtonOptions(Toolbar.BUTTON_OPTIONS_NONE);

        View view = findViewById(android.R.id.content).getRootView();

        RecyclerView rvChangelog = view.findViewById(R.id.rvChangelog);
        changelogAdapter = new ChangelogInfoAdapter();
        rvChangelog.setAdapter(changelogAdapter);

        refresh();

        changelogAdapter.setChangelogs(allChangelogItems);

        Utils.hideKeyboard(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private AsyncLoadChangelog asyncLoadChangelogs = null;

    private void refresh() {
        if (asyncLoadChangelogs != null) return; // ignore refresh request as one is ongoing

        asyncLoadChangelogs = new ChangelogActivity.AsyncLoadChangelog();
        asyncLoadChangelogs.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class AsyncLoadChangelog extends AsyncTask<Void, ChangelogItem, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            changelogAdapter.setChangelogs(null);

            showProgressDialog(R.string.loading_changelog);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            allChangelogItems.clear();

            // Set Changelog data

            for(int i = 1; i < 100; i++) {
                ChangelogItem changelogItem = new ChangelogItem();
                String strChangelogFile = DEFAULT_CHANGELOG_REPOSITORY + i + ".txt";

                if (Tools.isURLReachable(strChangelogFile)) {
                    URL url;
                    try {
                        changelogItem.mVersion = String.valueOf(i);
                        url = new URL(strChangelogFile);
                        HttpsURLConnection uc = (HttpsURLConnection) url.openConnection();
                        InputStream in = uc.getInputStream();

                        long dateTime = uc.getLastModified();
                        if(dateTime > 0.0f)
                            changelogItem.mDate = "(" + DateFormat.getDateInstance(DateFormat.LONG).format(dateTime) + ")";

                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        while (true) {
                            String line;
                            if ((line = br.readLine()) == null)
                                break;

                            changelogItem.mChanges.add(line);
                        }

                        allChangelogItems.add(changelogItem);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                } else {
                    break;
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            complete();
        }

        @Override
        protected void onCancelled(Boolean result) {
            complete();
        }

        private void complete() {
            asyncLoadChangelogs = null;

            changelogAdapter.setChangelogs(allChangelogItems);

            LinearLayout llNoChangelog = findViewById(R.id.llNoChangelog);
            llNoChangelog.setVisibility(allChangelogItems.isEmpty() ? View.VISIBLE : View.GONE);

            dismissProgressDialog();
        }
    }
}