package moe.ouom.wekit.utils

// prefer this over android's own LruCache since kotlin.collections provide several useful methods
class LruCache<K, V>(
    initialCapacity: Int = 16,
    loadFactor: Float = 0.75f,
    private val maxLimit: Int = 100
) : LinkedHashMap<K, V>(initialCapacity, loadFactor, true) {

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > maxLimit
    }
}