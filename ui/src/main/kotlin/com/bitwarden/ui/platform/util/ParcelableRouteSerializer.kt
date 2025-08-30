package com.bitwarden.ui.platform.util

import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import androidx.core.os.ParcelCompat
import com.bitwarden.annotation.OmitFromCoverage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlin.reflect.KClass

/**
 * A custom [KSerializer] for serializing and deserializing [Parcelable] classes.
 *
 * This serializer is compatible with Jetpack Compose type-safe navigation routes. It allows for
 * complex [Parcelable] types to be easily serialized without needing to specify a `NavType` for
 * all non-primitive properties and properties nested within those types.
 *
 * For example:
 *
 * ```
 * @Parcelize
 * @Serializable(with = MyCustomRoute.Serializer::class)
 * data class MyCustomRoute(
 *     val data: Data,
 * ): Parcelable {
 *     class Serializer : ParcelableSerializer<MyCustomRoute>(MyCustomRoute::class)
 * }
 * ```
 *
 * Where `Data` is a complex type implementing `Parcelable`.
 *
 * In addition, this serializer provides support for directly serializing to the parent types of a
 * sealed class when using `SavedStateHandle.toRoute()` or `NavBackStackEntry.toRoute()`. In order
 * to achieve this while also ensuring each route is unique, a subclass of this serializer should
 * be defined for each parent and child type.
 *
 * Given the following type:
 *
 * ```
 * @Parcelize
 * @Serializable(with = ParentRoute.Serializer::class)
 * sealed class ParentRoute : Parcelable {
 *     class Serializer : ParcelableRouteSerializer<ParentRoute>(ParentRoute::class)
 *
 *     @Parcelize
 *     @Serializable(with = Child1.Serializer::class)
 *     data class Child1(
 *         val data1: Data1,
 *     ) : ParentRoute() {
 *         class Serializer : ParcelableRouteSerializer<Child1>(Child1::class)
 *     }
 *
 *     @Parcelize
 *     @Serializable(with = Child2.Serializer::class)
 *     data class Child2(
 *         val data2: Data2,
 *     ) : ParentRoute() {
 *         class Serializer : ParcelableRouteSerializer<Child2>(Child2::class)
 *     }
 * }
 * ```
 *
 * The route information for a navigation to the `Child1` destination could be derived using both:
 * ```
 * savedStateHandle.toRoute<ParentRoute.Child1>()
 * ```
 *
 * As well as:
 * ```
 * when (savedStateHandle.toRoute<ParentRoute>()) {
 *     is Child1 -> // ...
 *     is Child2 -> // ...
 * }
 * ```
 *
 * The latter is useful in cases where the same `ViewModel` is used to handle these routes.
 */
@OmitFromCoverage
@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
open class ParcelableRouteSerializer<T : Parcelable>(
    private val kClass: KClass<T>,
) : KSerializer<T> {

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(serialName = requireNotNull(kClass.qualifiedName)) {
            element<String>(elementName = "encodedData")
        }

    override fun deserialize(decoder: Decoder): T =
        decoder.decodeStructure(descriptor) {
            var encodedString: String? = null
            while (true) {
                when (decodeElementIndex(descriptor)) {
                    0 -> encodedString = decodeStringElement(descriptor = descriptor, index = 0)
                    else -> break
                }
            }
            encodedString
                ?.toParcelable()
                ?: throw IllegalStateException("Invalid decoding for ${kClass.qualifiedName}.")
        }

    override fun serialize(encoder: Encoder, value: T) {
        val valueAsString = value.toEncodedString()
        encoder.encodeStructure(descriptor = descriptor) {
            encodeStringElement(descriptor = descriptor, index = 0, value = valueAsString)
        }
    }

    // Helpers for encoding Parcelable data

    private fun Parcelable.toBytes(): ByteArray {
        val parcel = Parcel.obtain().apply {
            writeParcelable(this@toBytes, 0)
        }
        return parcel
            .marshall()
            .also { parcel.recycle() }
    }

    private fun Parcelable.toEncodedString(): String =
        Base64.encodeToString(toBytes(), Base64.URL_SAFE)

    private fun <T> ByteArray.toParcelable(): T? {
        val parcel = Parcel.obtain().apply {
            unmarshall(this@toParcelable, 0, this@toParcelable.size)
            setDataPosition(0)
        }
        val value = try {
            @Suppress("UNCHECKED_CAST")
            ParcelCompat.readParcelable(
                parcel,
                ParcelableRouteSerializer::class.java.classLoader,
                kClass.java,
            ) as T?
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: IllegalStateException) {
            null
        }
        parcel.recycle()
        return value
    }

    private fun <T> String.toParcelable(): T? = Base64.decode(this, Base64.URL_SAFE).toParcelable()
}
