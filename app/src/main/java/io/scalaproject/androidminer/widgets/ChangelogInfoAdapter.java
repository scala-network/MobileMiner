// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.widgets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.scalaproject.androidminer.R;
import io.scalaproject.androidminer.Utils;
import io.scalaproject.androidminer.api.ChangelogItem;

public class ChangelogInfoAdapter extends RecyclerView.Adapter<ChangelogInfoAdapter.ViewHolder> {

    private final List<ChangelogItem> changelogItems = new ArrayList<>();

    public ChangelogInfoAdapter() {
    }

    @Override
    public @NonNull
    ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_changelog, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final @NonNull ViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return changelogItems.size();
    }

    public void dataSetChanged() {
        notifyDataSetChanged();
    }

    public void setChangelogs(Collection<ChangelogItem> data) {
        changelogItems.clear();
        if (data != null) {
            changelogItems.addAll(data);
        }

        Collections.sort(changelogItems, ChangelogItem.ChangelogComparator);
        Collections.reverse(changelogItems);

        dataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvVersion;
        final TextView tvDate;
        final TextView tvChangelog;
        final LinearLayout llUpdate;

        ChangelogItem changelogItem;

        ViewHolder(View itemView) {
            super(itemView);
            tvVersion = itemView.findViewById(R.id.tvVersion);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvChangelog = itemView.findViewById(R.id.tvChangelog);
            llUpdate = itemView.findViewById(R.id.llUpdate);
        }

        void bind(final int position) {
            changelogItem = changelogItems.get(position);

            tvVersion.setText(String.valueOf(changelogItem.mVersion));
            tvDate.setText(changelogItem.mDate);

            StringBuilder changes = new StringBuilder();
            for (String change: changelogItem.mChanges) {
                changes.append(change).append('\n');
            }
            tvChangelog.setText(changes);

            llUpdate.setVisibility(Utils.needUpdate() && position == 0 ? View.VISIBLE : View.GONE);
        }
    }
}
