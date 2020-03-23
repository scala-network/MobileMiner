// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api;

public interface IProviderListener {
    void onStatsChange(ProviderData data);

    boolean onEnabledRequest();
}
