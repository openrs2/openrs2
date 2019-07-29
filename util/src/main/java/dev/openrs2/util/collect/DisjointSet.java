package dev.openrs2.util.collect;

public interface DisjointSet<T> extends Iterable<DisjointSet.Partition<T>> {
	interface Partition<T> extends Iterable<T> {
		/* empty */
	}

	Partition<T> add(T x);
	Partition<T> get(T x);
	void union(Partition<T> x, Partition<T> y);
	int elements();
	int partitions();
}
