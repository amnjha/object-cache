package com.here.object.cache.data.collections;

import org.redisson.api.RSet;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * @author amajha
 * @param <E>
 */
public class CacheSet<E> extends AbstractSet<E> {

    private RSet<E> set;

    public CacheSet(RSet<E> set){
        this.set= set;
    }

    /**
     * {@inheritDoc}
     *
     * @param e
     * @return
     */
    @Override
    public boolean add(E e) {
        return set.add(e);
    }

    /**
     * {@inheritDoc}
     *
     * @param o
     * @return
     */
    @Override
    public boolean remove(Object o) {
        return set.remove(o);
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void clear() {
        set.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @param c
     * @return
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        return set.retainAll(c);
    }

    /**
     * {@inheritDoc}
     *
     * @param filter
     * @return
     */
    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return set.removeIf(filter);
    }

    /**
     * {@inheritDoc}
     *
     * @param c
     * @return
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        return set.removeAll(c);
    }

    /**
     * {@inheritDoc}
     *
     * @param c
     * @return
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        return set.addAll(c);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public Iterator<E> iterator() {
        return set.iterator();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public int size() {
        return set.size();
    }
}
