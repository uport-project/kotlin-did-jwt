@file:Suppress("unused")

package me.uport.sdk.jwt

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

/**
 * shorthand to get a list adapter for moshi
 * @hide
 */
inline fun <reified E> Moshi.listAdapter(elementType: Type = E::class.java): JsonAdapter<List<E>> {
    return adapter(Types.newParameterizedType(List::class.java, elementType))
}

/**
 * shorthand to get a map adapter for moshi
 * @hide
 */
inline fun <reified K, reified V> Moshi.mapAdapter(
        keyType: Type = K::class.java,
        valueType: Type = V::class.java): JsonAdapter<Map<K, V>> {
    return adapter(Types.newParameterizedType(Map::class.java, keyType, valueType))
}

