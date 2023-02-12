// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.ui.LibsActivity;

public class CreditsActivity extends LibsActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LibsBuilder builder = new LibsBuilder();
        builder.aboutAppName = getResources().getString(R.string.app_name);
        builder.aboutShowIcon = true;
        builder.aboutShowVersion = true;
        builder.withFields(R.string.class.getFields());
        builder.withActivityTheme(R.style.CustomAboutLibrariesTheme);

        setIntent(builder.intent(this));

        super.onCreate(savedInstanceState);

        // Handle Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setBackground(getApplicationContext().getResources().getDrawable(R.drawable.backgound_toolbar));
        toolbar.setTitle(R.string.credits);
        toolbar.setTitleTextAppearance(getApplicationContext(), R.style.Toolbar_Title);

        setSupportActionBar(toolbar);
    }
}