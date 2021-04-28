package com.yangweiye;

public interface BalanceTree<V> {
    void put(Comparable key, V value);

    V search(Comparable key);

    void delete(Comparable key);
}
