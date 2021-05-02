package com.github.ruediste.laserPcb;

public class Var<T> {

	public T value;

	public T get() {
		return value;
	}

	public T set(T value) {
		this.value = value;
		return value;
	}

	public Var() {
	}

	public Var(T value) {
		this.value = value;
	}

	public static <T> Var<T> of(T value) {
		return new Var<>(value);
	}

	public static <T> Var<T> of() {
		return new Var<>();
	}
}
