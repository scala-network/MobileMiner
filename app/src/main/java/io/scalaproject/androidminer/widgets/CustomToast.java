// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.widgets;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import io.scalaproject.androidminer.R;

public class CustomToast extends Toast {
    public CustomToast(Context context, String message, int length, int YOffset) {
        super(context);

        init(context, message, length, YOffset);
    }

    public CustomToast(Context context, String message, int length) {
        super(context);

        init(context, message, length, getYOffset());
    }

    private void init(Context context, String message, int length, int YOffset) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_toast, null);
        TextView txtMsg = view.findViewById(R.id.tvToastMessage);
        txtMsg.setText(message);

        setGravity(getGravity() | Gravity.FILL_HORIZONTAL, getXOffset(), YOffset);

        setView(view);
        setDuration(length);
    }
}