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

    public String minersScala = "Miners Scala";
    public String hrScala = "0 H/s";

    String poolName = "Scala Pool";
    boolean recommendPool = false;

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

        if(selected)
            inflater.inflate(R.layout.widget_pool_banner_blue, this);
        else
            inflater.inflate(R.layout.widget_pool_banner, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setSelected(Context context, boolean selected) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if(selected)
            inflater.inflate(R.layout.widget_pool_banner_blue, this);
        else
            inflater.inflate(R.layout.widget_pool_banner, this);

        refresh();
    }

    public void refresh() {
        icon = findViewById(R.id.icon);

        if(mPoolItem != null) {
            poolName = mPoolItem.getKey();
            recommendPool = mPoolItem.getKey().toLowerCase().contains("official");
        }

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
