// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.scalaproject.androidminer.R;
import io.scalaproject.androidminer.Utils;

public class DonationsFragment extends DialogFragment {
    static final String TAG = "CreditsFragment";

    public static DonationsFragment newInstance() {
        return new DonationsFragment();
    }

    public static void display(FragmentManager fm) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        DonationsFragment.newInstance().show(ft, TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_donations, null);

        Button btnDonateBTC = view.findViewById(R.id.btnDonateBTC);
        btnDonateBTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.copyToClipboard("Scala BTC Donation Address", Utils.SCALA_BTC_ADDRESS);
                Utils.showToast(getContext(), getResources().getString(R.string.donationadressbtc_copied), Toast.LENGTH_SHORT);
            }
        });

        Button btnDonateETH = view.findViewById(R.id.btnDonateETH);
        btnDonateETH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.copyToClipboard("Scala ETH Donation Address", Utils.SCALA_ETH_ADDRESS);
                Utils.showToast(getContext(), getResources().getString(R.string.donationadresseth_copied), Toast.LENGTH_SHORT);
            }
        });

        Button btnDonateLTC = view.findViewById(R.id.btnDonateLTC);
        btnDonateLTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.copyToClipboard("Scala LTC Donation Address", Utils.SCALA_LTC_ADDRESS);
                Utils.showToast(getContext(), getResources().getString(R.string.donationadressltc_copied), Toast.LENGTH_SHORT);
            }
        });

        Button btnDonateXLA = view.findViewById(R.id.btnDonateXLA);
        btnDonateXLA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.copyToClipboard("Scala XLA Donation Address", Utils.SCALA_XLA_ADDRESS);
                Utils.showToast(getContext(), getResources().getString(R.string.donationadressxla_copied), Toast.LENGTH_SHORT);
            }
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity(), R.style.MaterialAlertDialogCustom);
        builder.setView(view);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }
}