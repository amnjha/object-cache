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
	
	public long incrementAndGet() {
		return counterValue.incrementAndGet();
	}
	
	public long decrementAndGet() {
		return counterValue.decrementAndGet();
	}
}
