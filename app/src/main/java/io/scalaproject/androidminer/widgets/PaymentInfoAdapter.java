// Copyright (c) 2021, Scala
//
// Please see the included LICENSE file for more information.

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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.scalaproject.androidminer.Utils;
import io.scalaproject.androidminer.api.PaymentItem;
import io.scalaproject.androidminer.api.PoolItem;

public class PaymentInfoAdapter extends RecyclerView.Adapter<PaymentInfoAdapter.ViewHolder> {

    private final List<PaymentItem> paymentItems = new ArrayList<>();

    private Context context;

    public PaymentInfoAdapter(Context context) {
        this.context = context;
    }

    @Override
    public @NonNull
    ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final @NonNull ViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return paymentItems.size();
    }

    public void dataSetChanged() {
        notifyDataSetChanged();
    }

    public List<PaymentItem> getPayments() {
        return paymentItems;
    }

    public void setPayments(Collection<PaymentItem> data) {
        paymentItems.clear();
        if (data != null) {
            for (PaymentItem payment : data) {
                paymentItems.add(payment);
            }
        }

        //Collections.sort(paymentItems, PoolItem.PoolComparator);

        dataSetChanged();
    }

    private boolean itemsClickable = true;

    public void allowClick(boolean clickable) {
        itemsClickable = clickable;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmount;
        TextView tvFee;
        TextView tvHash;
        TextView tvTimestamp;

        PaymentItem paymentItem;

        ViewHolder(View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tx_amount);
            tvFee = itemView.findViewById(R.id.tx_fee);
            tvHash = itemView.findViewById(R.id.tx_hash);
            tvTimestamp = itemView.findViewById(R.id.tx_datetime);
        }

        void bind(final int position) {
            paymentItem = paymentItems.get(position);

            tvAmount.setText(paymentItem.mAmount);
            tvFee.setText(paymentItem.mFee);
            tvHash.setText(Utils.getPrettyTx(paymentItem.mHash));
            tvTimestamp.setText(paymentItem.mTimestamp);

            // Options
            //itemView.setOnClickListener(this);
            itemView.setClickable(itemsClickable);
        }
    }
}
