package scala.androidminer.pools.api;

import scala.androidminer.pools.PoolItem;

public final class NoPool extends PoolTypeAbstract  {

    public NoPool(PoolItem poolItem){
        super(poolItem);
    }

    @Override
    public String getBlockHeight() {
        return "";
    }
}
