// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import static io.scalaproject.androidminer.MainActivity.contextOfApplication;

public abstract class BaseActivity extends AppCompatActivity {
    private static int sessionDepth = 0;

    @Override
    protected void onStart() {
        super.onStart();
        sessionDepth++;
        if(sessionDepth == 1){
            //app came to foreground;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (sessionDepth > 0)
            sessionDepth--;
        if (sessionDepth == 0) {
            // app went to background
            if(MainActivity.isDeviceMiningBackground())
                Toast.makeText(contextOfApplication, getResources().getString(R.string.miningbackground), Toast.LENGTH_SHORT).show();
        }
    }
}