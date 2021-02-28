// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

import io.scalaproject.androidminer.widgets.ChangelogInfoAdapter;
import io.scalaproject.androidminer.widgets.Toolbar;

public class ChangelogActivity extends BaseActivity {
    private static final String LOG_TAG = "ChangelogActivity";

    private ChangelogInfoAdapter changelogAdapter;

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

        changelogAdapter.setChangelogs(MainActivity.allChangelogItems);

        LinearLayout llNoChangelog = findViewById(R.id.llNoChangelog);
        llNoChangelog.setVisibility(MainActivity.allChangelogItems.isEmpty() ? View.VISIBLE : View.GONE);

        Utils.hideKeyboard(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}