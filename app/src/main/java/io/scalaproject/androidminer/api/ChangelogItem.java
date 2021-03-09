// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api;

import java.util.ArrayList;
import java.util.Comparator;

public class ChangelogItem {

    public int mVersion;
    public String mDate;
    public final ArrayList<String> mChanges = new ArrayList<>();

    public ChangelogItem() {
    }

    static public final Comparator<ChangelogItem> ChangelogComparator = new Comparator<ChangelogItem>() {
        @Override
        public int compare(ChangelogItem o1, ChangelogItem o2) {
            return o1.mVersion - o2.mVersion;
        }
    };
}
