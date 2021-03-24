package com.github.ruediste.laserPcb;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Observable<T> {

	private Set<Consumer<T>> observers = ConcurrentHashMap.newKeySet();

	public void add(Consumer<T> observer) {
		observers.add(observer);
	}

	public void remove(Consumer<T> observer) {
		observers.remove(observer);
	}

	public void send(T event) {
		observers.forEach(x -> x.accept(event));
	}
}
