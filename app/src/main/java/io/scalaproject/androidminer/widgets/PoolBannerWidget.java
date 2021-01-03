package io.scalaproject.androidminer.widgets;

import android.content.Context;
import android.media.Image;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.scalaproject.androidminer.R;
import io.scalaproject.androidminer.api.PoolItem;

public class PoolBannerWidget extends LinearLayout {

    ImageView icon;
    TextView tvMinersScala;
    TextView tvHrScala;
    TextView tvPoolName;
    TextView tvRecommendPool;
    View tvMainWrapper;
    public String minersScala = "Miners Scala";
    public String hrScala = "0 H/s";

    String poolName = "Scala Pool";
    boolean recommendPool = false;
    public boolean isSelected = false;
    PoolItem mPoolItem = null;

    public PoolBannerWidget(Context context, boolean selected) {
        super(context);
        _initContext(context, selected);
    }

    public PoolBannerWidget(Context context, PoolItem pi, boolean selected) {
        super(context);
        _initContext(context, selected);

        mPoolItem = pi;
    }

    public PoolBannerWidget(Context context, AttributeSet attrs, boolean selected) {
        super(context, attrs);
        _initContext(context, selected);
    }

    public PoolBannerWidget(Context context, AttributeSet attrs, int defStyle,  boolean selected) {
        super(context, attrs, defStyle);
        _initContext(context, selected);
    }

    private void _initContext(Context context, boolean selected) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_pool_banner, this);

            this.setBackgroundResource(R.drawable.corner_radius_grey);
        isSelected = selected;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setSelected(Context context, boolean selected) {
//        LayoutInflater inflater = (LayoutInflater) context
//                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        isSelected = selected;
        refresh();
    }

    public void refresh() {
        icon = findViewById(R.id.icon);

        if(mPoolItem != null) {
            poolName = mPoolItem.getKey();
            recommendPool = mPoolItem.getKey().toLowerCase().contains("official");
        }
        tvMainWrapper = findViewById(R.id.main_wrapper);
        tvMainWrapper.setBackgroundResource(isSelected?R.drawable.corner_radius_lighter_border_blue:R.drawable.corner_radius_lighter_border_grey);
        tvMainWrapper.setPadding(0,15,0,15);
        tvPoolName = findViewById(R.id.poolName);
        tvPoolName.setText(poolName);

        tvMinersScala = findViewById(R.id.minersScala);
        tvMinersScala.setText(minersScala);

        tvHrScala = findViewById(R.id.hrScala);
        tvHrScala.setText(hrScala);

        tvRecommendPool = findViewById(R.id.recommendPool);
        tvRecommendPool.setVisibility(recommendPool ? View.VISIBLE : View.INVISIBLE);

//        this.setVisibility(View.INVISIBLE);
//        this.setVisibility(View.GONE);
//        this.setVisibility(View.VISIBLE);
    }
}
