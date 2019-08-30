package dev.openrs2.deob;

import java.util.Objects;

import dev.openrs2.asm.MemberRef;
import dev.openrs2.util.collect.DisjointSet;

public final class ArgRef {
	private final DisjointSet.Partition<MemberRef> method;
	private final int arg;

	public ArgRef(DisjointSet.Partition<MemberRef> method, int arg) {
		this.method = method;
		this.arg = arg;
	}

	public DisjointSet.Partition<MemberRef> getMethod() {
		return method;
	}

	public int getArg() {
		return arg;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ArgRef argRef = (ArgRef) o;
		return arg == argRef.arg &&
			method.equals(argRef.method);
	}

	@Override
	public int hashCode() {
		return Objects.hash(method, arg);
	}
}
