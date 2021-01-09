package io.scalaproject.androidminer.widgets;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.scalaproject.androidminer.R;

import java.net.HttpURLConnection;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import io.scalaproject.androidminer.Utils;
import io.scalaproject.androidminer.api.PoolItem;

public class PoolInfoAdapter extends RecyclerView.Adapter<PoolInfoAdapter.ViewHolder> {

    public interface OnMenuPoolListener {
        //void onInteraction(View view, PoolItem item);
        boolean onContextInteraction(MenuItem item, PoolItem infoItem);
    }

    public interface OnSelectPoolListener {
        void onSelectPool(View view, PoolItem item);
    }

    private final List<PoolItem> poolItems = new ArrayList<>();

    private final OnMenuPoolListener onMenuPoolListener;
    private final OnSelectPoolListener onSelectPoolListener;

    private Context context;

    public PoolInfoAdapter(Context context, OnSelectPoolListener onSelectPoolListener, OnMenuPoolListener onMenuPoolListener) {
        this.context = context;
        this.onSelectPoolListener = onSelectPoolListener;
        this.onMenuPoolListener = onMenuPoolListener;
    }

    @Override
    public @NonNull
    ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pool, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final @NonNull ViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return poolItems.size();
    }

    public void addPool(PoolItem pool) {
        if (!poolItems.contains(pool))
            poolItems.add(pool);

        dataSetChanged(); // in case the poolitem has changed
    }

    public void dataSetChanged() {
        //Collections.sort(poolItems, PoolItem.BestNodeComparator);
        notifyDataSetChanged();
    }

    public List<PoolItem> getPools() {
        return poolItems;
    }

    public void setPools(Collection<PoolItem> data) {
        poolItems.clear();
        if (data != null) {
            for (PoolItem pool : data) {
                if (!poolItems.contains(pool))
                    poolItems.add(pool);
            }
        }

        dataSetChanged();
    }

    private boolean itemsClickable = true;

    public void allowClick(boolean clickable) {
        itemsClickable = clickable;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvName;
        TextView tvMiners;
        TextView tvHr;
        ImageView ivIcon;
        ImageButton ibOptions;
        boolean popupOpen = false;

        PoolItem poolItem;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvMiners = itemView.findViewById(R.id.tvMiners);
            tvHr = itemView.findViewById(R.id.tvHr);
            ivIcon = itemView.findViewById(R.id.ivIcon);

            ibOptions = itemView.findViewById(R.id.ibOptions);
            ibOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (popupOpen) return;
                    //creating a popup menu
                    PopupMenu popup = new PopupMenu(context, ibOptions);
                    //inflating menu from xml resource
                    popup.inflate(R.menu.pool_context_menu);
                    popupOpen = true;
                    //adding click listener
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (onMenuPoolListener != null) {
                                return onMenuPoolListener.onContextInteraction(item, poolItem);
                            }
                            return false;
                        }
                    });
                    //displaying the popup
                    popup.show();
                    popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            popupOpen = false;
                        }
                    });

                }
            });
        }

        void bind(final int position) {
            poolItem = poolItems.get(position);

            tvName.setText(poolItem.getKey());

            //ibOptions.setVisibility(poolItem.isUserDefined() ? View.VISIBLE : View.GONE);

            if(poolItem.isValid()) {
                // Miners
                tvMiners.setVisibility(View.VISIBLE);
                tvMiners.setText(String.format("%s %s", poolItem.getMiners(), context.getResources().getString(R.string.miners)));

                // Hashrate
                tvHr.setVisibility(View.VISIBLE);
                float hashrate = poolItem.getHr();

                String frmt = "k";
                if(hashrate > 1000) {
                    frmt = "M";
                    hashrate /= 1000.0f;
                }

                tvHr.setText(String.format("%s %sH/s", new DecimalFormat("##.#").format(hashrate), frmt));
            } else {
                tvMiners.setVisibility(View.GONE);
                tvHr.setVisibility(View.GONE);
            }

            Bitmap icon = poolItem.getIcon();
            if(icon != null) {
                int dim = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, itemView.getResources().getDisplayMetrics());
                ivIcon.getLayoutParams().height = dim;
                ivIcon.getLayoutParams().width = dim;

                ivIcon.setImageBitmap(Utils.getCroppedBitmap(poolItem.getIcon()));
            }
            else {
                int dim = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, itemView.getResources().getDisplayMetrics());
                ivIcon.getLayoutParams().height = dim;
                ivIcon.getLayoutParams().width = dim;

                ivIcon.setImageBitmap(Utils.getCroppedBitmap(Utils.getBitmap(context, R.drawable.ic_pool)));
            }

            itemView.setOnClickListener(this);
            itemView.setClickable(itemsClickable);
        }

        @Override
        public void onClick(View view) {
            if (onSelectPoolListener != null) {
                int position = getAdapterPosition(); // gets item position
                if (position != RecyclerView.NO_POSITION) { // Check if an item was deleted, but the user clicked it before the UI removed it
                    onSelectPoolListener.onSelectPool(view, poolItems.get(position));
                }
            }
        }
    }

    /*static public int getPingIcon(NodeInfo nodeInfo) {
        if (nodeInfo.isUnauthorized()) {
            return R.drawable.ic_wifi_lock_24dp;
        }

        if (nodeInfo.isValid()) {
            final double ping = nodeInfo.getResponseTime();
            if (ping < NodeInfo.PING_GOOD) {
                return R.drawable.ic_signal_wifi_4_bar_24dp;
            } else if (ping < NodeInfo.PING_MEDIUM) {
                return R.drawable.ic_signal_wifi_3_bar_24dp;
            } else if (ping < NodeInfo.PING_BAD) {
                return R.drawable.ic_signal_wifi_2_bar_24dp;
            } else {
                return R.drawable.ic_signal_wifi_1_bar_24dp;
            }
        } else {
            return R.drawable.ic_signal_wifi_off_24dp;
        }
    }

    static public String getResponseErrorText(Context ctx, int responseCode) {
        if (responseCode == 0) {
            return ctx.getResources().getString(R.string.node_general_error);
        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            return ctx.getResources().getString(R.string.node_auth_error);
        } else {
            return ctx.getResources().getString(R.string.node_test_error, responseCode);
        }
    }*/
}
