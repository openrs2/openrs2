package dev.openrs2.bundler.transform

import dev.openrs2.asm.transform.Transformer
import dev.openrs2.bundler.Resource

class ResourceTransformer(
    private val resources: List<Resource>? = null,
    private val glResources: List<List<Resource>> = Resource.compressGlResources(),
    private val miscResources: List<Resource> = Resource.compressMiscResources()
) : Transformer()
