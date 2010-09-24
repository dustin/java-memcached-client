package net.spy.memcached.vbucket;

import java.util.Observer;
import java.util.Observable;

public class BucketObserverMock implements Observer {
    boolean updateCalled = false;

    public void update(Observable o, Object arg) {
        updateCalled = true;
    }

    public boolean isUpdateCalled() {
        return updateCalled;
    }
}
