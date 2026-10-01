package com.erns.alertauni.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive

object SafeStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SafeString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        return try {
            val jsonDecoder = decoder as? JsonDecoder
            val element = jsonDecoder?.decodeJsonElement()
            if (element == null || element.toString() == "null" || element.jsonPrimitive.content == "null") {
                ""
            } else {
                element.jsonPrimitive.content
            }
        } catch (e: Exception) {
            ""
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

@Serializable
data class StudentEnrollment(
    @Serializable(with = SafeStringSerializer::class)
    @SerialName("course_catalog_id") val course_catalog_id: String = "",
    @Serializable(with = SafeStringSerializer::class)
    @SerialName("course_id") val courseId: String = "",
    @Serializable(with = SafeStringSerializer::class)
    @SerialName("course_code") val courseCode: String = "",
    @Serializable(with = SafeStringSerializer::class)
    @SerialName("course_name") val courseName: String = "",
    @Serializable(with = SafeStringSerializer::class)
    @SerialName("semester") val semester: String = "",
    @Serializable(with = SafeStringSerializer::class)
    @SerialName("course_type") val courseType: String = "",
    @Serializable(with = SafeStringSerializer::class)
    @SerialName("group_type") val groupType: String = "",
    @Serializable(with = SafeStringSerializer::class)
    @SerialName("firstname") val firstname: String = "",
    @Serializable(with = SafeStringSerializer::class)
    @SerialName("surname") val surname: String = "",
    @Serializable(with = SafeStringSerializer::class)
    @SerialName("email") val email: String = ""
)
