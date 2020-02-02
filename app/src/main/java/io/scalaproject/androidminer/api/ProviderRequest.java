package io.scalaproject.androidminer.api;

import java.util.Timer;
import java.util.TimerTask;

import io.scalaproject.androidminer.Config;

public class ProviderRequest{

    protected PoolItem mPoolItem;

    private ProviderTask current;
    private IProviderListener mListener;
    public ProviderRequest setListener(IProviderListener listener) {
        if(mListener == listener) {
            return this;
        }
        mListener = listener;
//        stop();
        PoolItem pi = ProviderManager.getSelectedPool();
        if(pi == null) {
            return this;
        }
        pi.getInterface().mListener = listener;
        return this;
    }

    public class ProviderTask extends TimerTask{

        private ProviderAbstract mProvider;

        public ProviderTask(ProviderAbstract abs) {
            mProvider = abs;
        }

        @Override
        public void run(){
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
        current = new ProviderTask(mPoolItem.getInterface());
        timer.schedule(current, Config.statsDelay);
    }

    public class ProviderTimer extends Timer {
        public ProviderTimer() {
            super("ProviderTimer");
        }
    }

}
