// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.scalaproject.androidminer.R;
import io.scalaproject.androidminer.SettingsFragment;
import io.scalaproject.androidminer.Utils;
import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderManager;

public class PoolView extends LinearLayout {
    public interface OnButtonListener {
        void onButton();
    }

    PoolView.OnButtonListener onButtonListener;

    public void setOnButtonListener(PoolView.OnButtonListener listener) {
        onButtonListener = listener;
    }

    ImageView ivIcon;
    TextView tvPoolName;
    TextView tvPoolURL;
    ImageView ivOptions;

    Context mContext = null;

    public PoolView(Context context) {
        super(context);
        initializeViews(context);
    }

    public PoolView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public PoolView(Context context,
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
        inflater.inflate(R.layout.view_pool, this);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        ivIcon = findViewById(R.id.ivIcon);
        tvPoolName = findViewById(R.id.tvName);
        tvPoolURL = findViewById(R.id.tvURL);

        ivOptions = findViewById(R.id.ibOptions);
        ivOptions.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (onButtonListener != null) {
                    onButtonListener.onButton();
                }
            }
        });

        PoolItem poolItem = SettingsFragment.selectedPoolTmp == null ? ProviderManager.getSelectedPool() : SettingsFragment.selectedPoolTmp;

        if(poolItem == null)
            return;

        tvPoolName.setText(poolItem.getKey());
        tvPoolURL.setText(poolItem.getPool());

        Bitmap icon = poolItem.getIcon();
        if(icon != null) {
            int dim = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 34, getResources().getDisplayMetrics());
            ivIcon.getLayoutParams().height = dim;
            ivIcon.getLayoutParams().width = dim;

            ivIcon.setImageBitmap(Utils.getCroppedBitmap(poolItem.getIcon()));
        }
        else {
            int dim = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, getResources().getDisplayMetrics());
            ivIcon.getLayoutParams().height = dim;
            ivIcon.getLayoutParams().width = dim;

            ivIcon.setImageBitmap(Utils.getCroppedBitmap(Utils.getBitmap(getContext(), R.drawable.ic_pool_default)));
        }
    }
}
