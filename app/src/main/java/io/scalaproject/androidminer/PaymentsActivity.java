// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import io.scalaproject.androidminer.api.PaymentItem;
import io.scalaproject.androidminer.api.ProviderData;
import io.scalaproject.androidminer.widgets.PaymentInfoAdapter;
import io.scalaproject.androidminer.widgets.Toolbar;

public class PaymentsActivity extends BaseActivity implements PaymentInfoAdapter.OnShowPaymentListener {
    private static final String LOG_TAG = "PaymentsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        setContentView(R.layout.fragment_payments);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        toolbar.setOnButtonListener(new Toolbar.OnButtonListener() {
            @Override
            public void onButtonMain(int type) {
                switch (type) {
                    case Toolbar.BUTTON_MAIN_CLOSE:
                        onBackPressed();
                }
            }

            @Override
            public void onButtonOptions(int type) {
                // Does nothing in this view
            }
        });

        toolbar.setTitle("Payments");
        toolbar.setButtonMain(Toolbar.BUTTON_MAIN_CLOSE);
        toolbar.setButtonOptions(Toolbar.BUTTON_OPTIONS_NONE);

        View view = findViewById(android.R.id.content).getRootView();

        RecyclerView rvPayments = view.findViewById(R.id.rvPayments);
        PaymentInfoAdapter paymentsAdapter = new PaymentInfoAdapter(this, this);
        rvPayments.setAdapter(paymentsAdapter);

        // Set payments data
        Set<PaymentItem> allPayments = new HashSet<>();
        for (int i = 0 ; i < StatsFragment.poolData.miner.payments.size(); i++) {
            ProviderData.Payment payment = StatsFragment.poolData.miner.payments.get(i);

            PaymentItem pi = new PaymentItem();
            pi.mAmount = String.valueOf(payment.amount);
            pi.mTimestamp = Utils.getDate(Long.parseLong(payment.timestamp));
            pi.mFee = String.valueOf(payment.fee);
            pi.mHash = payment.hash;

            allPayments.add(pi);
        }

        paymentsAdapter.setPayments(allPayments);

        LinearLayout llNoPayments = view.findViewById(R.id.llNoPayments);
        llNoPayments.setVisibility(allPayments.isEmpty() ? View.VISIBLE : View.GONE);

        Utils.hideKeyboard(this);
    }

    @Override
    public void onShowPayment(final View view, final PaymentItem paymentItem) {
        String paymentURL = "https://explorer.scalaproject.io/tx?tx_info=" + paymentItem.mHash;
        Uri uri = Uri.parse(paymentURL);
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}