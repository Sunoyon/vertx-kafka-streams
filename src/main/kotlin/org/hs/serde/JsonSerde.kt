package org.hs.serde

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import lombok.SneakyThrows
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class JsonSerde<T>(private val type: Class<T>) : Serde<T> {

  companion object {
    val OBJECT_MAPPER: ObjectMapper = ObjectMapper()
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .registerModule(JavaTimeModule())
  }

  override fun serializer(): Serializer<T> {
    return Serializer { _: String?, data: T -> serialize(data) }
  }

  @SneakyThrows
  private fun serialize(data: T): ByteArray {
    return OBJECT_MAPPER.writeValueAsBytes(data)
  }

  override fun deserializer(): Deserializer<T> {
    return Deserializer { _: String?, bytes: ByteArray ->
      deserialize(
        bytes
      )
    }
  }

  @SneakyThrows
  private fun deserialize(bytes: ByteArray): T {
    return OBJECT_MAPPER.readValue(bytes, type)
  }
}
