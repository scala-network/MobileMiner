// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api;

import java.util.Timer;
import java.util.TimerTask;

import io.scalaproject.androidminer.Config;

public class ProviderRequest {

    protected PoolItem mPoolItem;

    private ProviderTask current;
    private IProviderListener mListener;
    public ProviderRequest setListener(IProviderListener listener) {
        if(mListener == listener) {
            return this;
        }

        mListener = listener;

        PoolItem pi = ProviderManager.getSelectedPool();
        if(pi == null) {
            return this;
        }
        pi.getInterface().mListener = listener;
        return this;
    }

    public class ProviderTask extends TimerTask {

        private ProviderAbstract mProvider;

        public ProviderTask(ProviderAbstract abs) {
            mProvider = abs;
        }

        @Override
        public void run() {
            mProvider.execute();
            repeat();
        }
    }

    ProviderTimer timer;

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            current.cancel();
            timer = null;
            current = null;
        }
    }

    public void execute() {
        start();
        repeat();
    }

    public void run() {
        if(current == null) {
            return;
        }

        current.run();
    }

    public void start() {
        if(current != null || mPoolItem == null) {
            return;
        }

        ProviderAbstract pa = mPoolItem.getInterface();
        pa.mListener = mListener;
        current = new ProviderTask(pa);
        current.run();
    }

    private void repeat() {
        stop();
        timer = new ProviderTimer();
        ProviderAbstract pa = mPoolItem.getInterface();
        pa.mListener = mListener;
        current = new ProviderTask(pa);
        timer.schedule(current, Config.statsDelay);
    }

    public class ProviderTimer extends Timer {
        public ProviderTimer() {
            super("ProviderTimer");
        }
    }
}
