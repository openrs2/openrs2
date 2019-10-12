package dev.openrs2.bundler;

import com.google.common.collect.ImmutableList;
import dev.openrs2.asm.transform.Transformer;
import dev.openrs2.bundler.transform.CachePathTransformer;
import dev.openrs2.bundler.transform.HostCheckTransformer;
import dev.openrs2.bundler.transform.LoadLibraryTransformer;
import dev.openrs2.bundler.transform.MacResizeTransformer;
import dev.openrs2.bundler.transform.RightClickTransformer;

public final class Bundler {
	public static final ImmutableList<Transformer> TRANSFORMERS = ImmutableList.of(
		new CachePathTransformer(),
		new HostCheckTransformer(),
		new MacResizeTransformer(),
		new RightClickTransformer(),
		new LoadLibraryTransformer()
	);
}
