package dev.openrs2.deob.ast.util;

import java.util.function.Consumer;

import com.github.javaparser.ast.Node;

public final class NodeUtils {
	@SuppressWarnings("unchecked")
	public static <T extends Node> void walk(Node node, Node.TreeTraversal traversal, Class<T> type, Consumer<T> consumer) {
		node.walk(traversal, n -> {
			if (type.isAssignableFrom(n.getClass())) {
				consumer.accept((T) n);
			}
		});
	}

	private NodeUtils() {
		/* empty */
	}
}
