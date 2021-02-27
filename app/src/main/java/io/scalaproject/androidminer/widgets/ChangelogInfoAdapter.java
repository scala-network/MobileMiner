// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.widgets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.scalaproject.androidminer.R;
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
        TextView tvVersion, tvDate, tvChangelog;

        ChangelogItem changelogItem;

        ViewHolder(View itemView) {
            super(itemView);
            tvVersion = itemView.findViewById(R.id.tvVersion);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvChangelog = itemView.findViewById(R.id.tvChangelog);
        }

        void bind(final int position) {
            changelogItem = changelogItems.get(position);

            tvVersion.setText(String.valueOf(changelogItem.mVersion));
            tvDate.setText(changelogItem.mDate);

            StringBuffer text = new StringBuffer();
            for (String change: changelogItem.mChanges) {
                text.append(change).append('\n');
            }
            tvChangelog.setText(text);
        }
    }
}
