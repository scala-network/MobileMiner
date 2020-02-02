package io.scalaproject.androidminer.api;

public interface IProviderListener {
    void onStatsChange(ProviderData data);

    boolean onEnabledRequest();
}
