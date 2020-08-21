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

public class PoolBannerWidget extends LinearLayout {

    public PoolBannerWidget(Context context) {
        super(context);
        _initContext(context);
    }
    public PoolBannerWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        _initContext(context);
    }


    public PoolBannerWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        _initContext(context);
    }

    private void _initContext(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_pool_banner, this);

    }
    ImageView icon;
    TextView minersScala;
    TextView hrScala;
    TextView poolName;
    TextView recommendPool;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        icon = findViewById(R.id.icon);
        poolName = findViewById(R.id.poolName);
        minersScala = findViewById(R.id.minersScala);
        hrScala = findViewById(R.id.hrScala);
        recommendPool = findViewById(R.id.recommendPool);
    }

    public PoolBannerWidget setPoolName(String pool_name) {
        poolName.setText(pool_name);
        return this;
    }

    public PoolBannerWidget isRecommendPool(boolean rec) {
        recommendPool.setVisibility(rec ? View.VISIBLE : View.INVISIBLE);
        return this;
    }

    public PoolBannerWidget setHrScala(String hr) {
        hrScala.setText(hr);
        return this;
    }

    public PoolBannerWidget setMinerScala(String miners_scala) {
        minersScala.setText(miners_scala);
        return this;
    }

}
