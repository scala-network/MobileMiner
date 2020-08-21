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
    TextView tvMinersScala;
    TextView tvHrScala;
    TextView tvPoolName;
    TextView tvRecommendPool;
    public String minersScala = "Miners Scala";
    public String hrScala = "0H/s";
    public String poolName = "Scala Pool";
    public boolean recommendPool = false;
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        icon = findViewById(R.id.icon);
        tvPoolName = findViewById(R.id.poolName);
        tvMinersScala = findViewById(R.id.minersScala);
        tvHrScala = findViewById(R.id.hrScala);
        tvRecommendPool = findViewById(R.id.recommendPool);

        refresh();
    }

    public void refresh() {
        tvPoolName.setText(poolName);
        tvHrScala.setText(hrScala);
        tvMinersScala.setText(minersScala);
        tvRecommendPool.setVisibility(recommendPool ? View.VISIBLE : View.INVISIBLE);

//        this.setVisibility(View.INVISIBLE);
//        this.setVisibility(View.GONE);
//        this.setVisibility(View.VISIBLE);
    }


}
