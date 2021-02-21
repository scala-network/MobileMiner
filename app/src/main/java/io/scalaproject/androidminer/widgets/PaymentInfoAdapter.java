// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.widgets;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.scalaproject.androidminer.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.scalaproject.androidminer.Utils;
import io.scalaproject.androidminer.api.PaymentItem;

public class PaymentInfoAdapter extends RecyclerView.Adapter<PaymentInfoAdapter.ViewHolder> {

    public interface OnShowPaymentListener {
        void onShowPayment(View view, PaymentItem item);
    }

    private final PaymentInfoAdapter.OnShowPaymentListener onShowPaymentListener;

    private final List<PaymentItem> paymentItems = new ArrayList<>();

    public PaymentInfoAdapter(Context context, PaymentInfoAdapter.OnShowPaymentListener onShowPaymentListener) {
        this.onShowPaymentListener = onShowPaymentListener;
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

    public void setPayments(Collection<PaymentItem> data) {
        paymentItems.clear();
        if (data != null) {
            paymentItems.addAll(data);
        }

        Collections.sort(paymentItems, PaymentItem.PaymentComparator);
        Collections.reverse(paymentItems);

        dataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
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

            tvAmount.setText("+ " + paymentItem.mAmount);
            tvFee.setText(paymentItem.mFee);

            tvHash.setText(Utils.getPrettyTx(paymentItem.mHash));

            tvTimestamp.setText(Utils.formatTimestamp(paymentItem.mTimestamp));

            // Options
            itemView.setOnClickListener(this);
            itemView.setClickable(true);
        }

        @Override
        public void onClick(View view) {
            if (onShowPaymentListener != null) {
                onShowPaymentListener.onShowPayment(view, paymentItem);
            }
        }
    }
}
