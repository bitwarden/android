package com.bitwarden.ui.platform.util

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * Gets the route string for an object.
 */
@OptIn(InternalSerializationApi::class)
fun <T : Any> T.toObjectNavigationRoute(): String = this::class.toObjectKClassNavigationRoute()

/**
 * Gets the route string for a [KClass] of an object.
 */
@OptIn(InternalSerializationApi::class)
fun <T : Any> KClass<T>.toObjectKClassNavigationRoute(): String =
    this.serializer().descriptor.serialName
