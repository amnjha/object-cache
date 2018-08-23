package com.here.object.cache.data;

import org.redisson.api.RAtomicLong;

/**
 * 
 * @author amajha
 *
 */
public class AtomicCounter {
	
	private RAtomicLong counterValue;
	
	protected AtomicCounter(RAtomicLong counterValue) {
		this.counterValue=counterValue;
	}

	/**
	 *
	 * @return
	 */
	public long incrementAndGet() {
		return counterValue.incrementAndGet();
	}

	/**
	 *
	 * @param delta
	 * @return
	 */
	public long incrementAndGet(long delta){
		return counterValue.addAndGet(delta);
	}

	/**
	 *
	 * @return
	 */
	public long getAndIncrement() {
		return counterValue.getAndIncrement();
	}

	/**
	 *
	 * @param delta
	 * @return
	 */
	public long getAndIncrement(long delta){
		return counterValue.getAndAdd(delta);
	}

	/**
	 *
	 * @return
	 */
	public long decrementAndGet() {
		return counterValue.decrementAndGet();
	}

	/**
	 *
	 * @return
	 */
	public long getAndDecrement() {
		return counterValue.decrementAndGet();
	}

	/**
	 *
	 * @param value
	 * @return
	 */
	public long decrementAndGet(long value) {
		return counterValue.addAndGet(value*-1);
	}

	/**
	 *
	 * @param value
	 * @return
	 */
	public long getAndDecrement(long value) {
		return counterValue.getAndAdd(value*-1);
	}

	/**
	 *
	 * @return
	 */
	public long get(){
		return counterValue.get();
	}

	/**
	 *
	 * @param newValue
	 */
	public void set(long newValue){
		counterValue.set(newValue);
	}

	/**
	 *
	 * @return
	 */
	public long reset(){
		return counterValue.getAndSet(0);
	}
}
