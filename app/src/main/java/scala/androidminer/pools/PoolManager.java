package scala.androidminer.pools;

import java.util.ArrayList;
import scala.androidminer.Config;

public final class PoolManager {

    static private ArrayList<PoolItem> mPools = new ArrayList<PoolItem>();

    static public void add(PoolItem poolItem) {
        mPools.add(poolItem);
    }

    static public void add(String key, String pool,String port, int poolType, String poolUrl) {
        mPools.add(new PoolItem(key,pool,port,poolType,poolUrl ));
    }

    static public PoolItem[] getPools() {
        return mPools.toArray(new PoolItem[mPools.size()]);
    }

    static public PoolItem getPoolById(int idx) {
        return mPools.get(idx);
    }

    static public PoolItem getPoolById(String idx) {
        int index = Integer.parseInt(idx);
        if (idx.equals("") || mPools.size() < index) {
            return null;
        }
        return mPools.get(index);
    }


    static public PoolItem getSelectedPool() {
        String sp = Config.read("selected_pool");
        if (sp.equals("")) {
            return null;
        }

        PoolItem pi = getPoolById(sp);

        return pi;
    }
}
