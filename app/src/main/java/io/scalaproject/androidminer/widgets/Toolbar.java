/*
 * Copyright (c) 2017 m2049r
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ////////////////
 *
 * Copyright (c) 2021 Scala
 *
 * Please see the included LICENSE file for more information.*/

// based on https://code.tutsplus.com/tutorials/creating-compound-views-on-android--cms-22889

package io.scalaproject.androidminer.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import io.scalaproject.androidminer.R;

public class Toolbar extends androidx.appcompat.widget.Toolbar {
    public interface OnButtonListener {
        void onButtonMain(int type);
        void onButtonOptions(int type);
    }

    OnButtonListener onButtonListener;

    public void setOnButtonListener(OnButtonListener listener) {
        onButtonListener = listener;
    }

    TextView toolbarTitle;
    TextView toolbarTitleCenter;
    ImageButton bMainIcon;
    ImageButton bOptionIcon;

    public Toolbar(Context context) {
        super(context);
        initializeViews(context);
    }

    public Toolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public Toolbar(Context context,
                   AttributeSet attrs,
                   int defStyle) {
        super(context, attrs, defStyle);
        initializeViews(context);
    }

    /**
     * Inflates the views in the layout.
     *
     * @param context the current context for the view.
     */
    private void initializeViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_toolbar, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitleCenter = findViewById(R.id.toolbarTitleCenter);

        bMainIcon = findViewById(R.id.bMainLogo);
        bMainIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (onButtonListener != null) {
                    onButtonListener.onButtonMain(mainButtonType);
                }
            }
        });

        bOptionIcon = findViewById(R.id.bShare);
        bOptionIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (onButtonListener != null) {
                    onButtonListener.onButtonOptions(optionsButtonType);
                }
            }
        });

        bOptionIcon.setVisibility(View.GONE);
    }

    public void setTitle(String title) {
        this.setTitle(title, true);
    }

    public void setTitle(String title, boolean centered) {
        if(!centered) {
            toolbarTitle.setVisibility(View.VISIBLE);
            toolbarTitle.setText(title);
            toolbarTitleCenter.setVisibility(View.GONE);
        } else {
            toolbarTitleCenter.setVisibility(View.VISIBLE);
            toolbarTitleCenter.setText(title);
            toolbarTitle.setVisibility(View.GONE);
        }
    }

    public String getTitle() { return toolbarTitle.getText().toString(); }

    public final static int BUTTON_MAIN_NONE = -1;
    public final static int BUTTON_MAIN_LOGO = 0;
    public final static int BUTTON_MAIN_BACK = 1;
    public final static int BUTTON_MAIN_CLOSE = 2;

    int mainButtonType = BUTTON_MAIN_LOGO;

    public void setButtonMain(int type) {
        switch (type) {
            case BUTTON_MAIN_NONE:
                bMainIcon.setBackground(getResources().getDrawable(R.mipmap.ic_logo_colors));
                bMainIcon.setVisibility(View.INVISIBLE);
                break;
            case BUTTON_MAIN_LOGO:
                bMainIcon.setBackground(getResources().getDrawable(R.mipmap.ic_logo_colors));
                bMainIcon.setVisibility(View.VISIBLE);
                break;
            case BUTTON_MAIN_BACK:
                bMainIcon.setBackground(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
                bMainIcon.setVisibility(View.VISIBLE);
                break;
            case BUTTON_MAIN_CLOSE:
                bMainIcon.setBackground(getResources().getDrawable(R.drawable.ic_close_white_24dp));
                bMainIcon.setVisibility(View.VISIBLE);
                break;
            default:
                bMainIcon.setBackground(getResources().getDrawable(R.mipmap.ic_logo_colors));
                bMainIcon.setVisibility(View.INVISIBLE);
        }

        mainButtonType = type;
    }

    public final static int BUTTON_OPTIONS_NONE = -1;
    public final static int BUTTON_OPTIONS_STAR = 0;
    public final static int BUTTON_OPTIONS_SHARE = 1;
    public final static int BUTTON_OPTIONS_SHOW_CORES = 2;
    public final static int BUTTON_OPTIONS_STATS = 3;
    public final static int BUTTON_OPTIONS_COPY = 4;

    int optionsButtonType = BUTTON_OPTIONS_NONE;

    public void setButtonOptions(int type) {
        switch (type) {
            case BUTTON_OPTIONS_NONE:
                bOptionIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_share));
                bOptionIcon.setVisibility(View.INVISIBLE);
                break;
            case BUTTON_OPTIONS_STAR:
                bOptionIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_star));
                bOptionIcon.setVisibility(View.VISIBLE);
                break;
            case BUTTON_OPTIONS_SHARE:
                bOptionIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_share));
                bOptionIcon.setVisibility(View.VISIBLE);
                break;
            case BUTTON_OPTIONS_SHOW_CORES:
                bOptionIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_refresh));
                bOptionIcon.setVisibility(View.VISIBLE);
                break;
            case BUTTON_OPTIONS_STATS:
                bOptionIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_stats_online));
                bOptionIcon.setVisibility(View.VISIBLE);
                break;
            case BUTTON_OPTIONS_COPY:
                bOptionIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_copy));
                bOptionIcon.setVisibility(View.VISIBLE);
                break;
            default:
                bOptionIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_share));
                bOptionIcon.setVisibility(View.INVISIBLE);
        }

        optionsButtonType = type;
    }
}