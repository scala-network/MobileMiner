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
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.scalaproject.androidminer.R;

public class CreditsFragment extends DialogFragment {
    static final String TAG = "CreditsFragment";

    public static CreditsFragment newInstance() {
        return new CreditsFragment();
    }

    public static void display(FragmentManager fm) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        CreditsFragment.newInstance().show(ft, TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_credits, null);
        TextView tvScala = view.findViewById(R.id.ScalaURL);
        TextView tvMine2gether = view.findViewById(R.id.Mine2getherURL);
        TextView tvMonerominer = view.findViewById(R.id.MoneroMinerURL);
        TextView tvMaterialDesign = view.findViewById(R.id.MaterialDesignURL);
        TextView tvFontAwesome = view.findViewById(R.id.FontAwesomeURL);

        tvScala.setText(Html.fromHtml(getString(R.string.ScalaLink)));
        tvScala.setMovementMethod(LinkMovementMethod.getInstance());

        tvMine2gether.setText(Html.fromHtml(getString(R.string.Mine2getherLink)));
        tvMine2gether.setMovementMethod(LinkMovementMethod.getInstance());

        tvMonerominer.setText(Html.fromHtml(getString(R.string.MoneroMinerLink)));
        tvMonerominer.setMovementMethod(LinkMovementMethod.getInstance());

        tvMaterialDesign.setText(Html.fromHtml(getString(R.string.MaterialDesignLink)));
        tvMaterialDesign.setMovementMethod(LinkMovementMethod.getInstance());

        tvFontAwesome.setText(Html.fromHtml(getString(R.string.FontAwesomeLink)));
        tvFontAwesome.setMovementMethod(LinkMovementMethod.getInstance());

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