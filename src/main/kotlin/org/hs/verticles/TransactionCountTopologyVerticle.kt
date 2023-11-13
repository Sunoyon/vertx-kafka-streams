package org.hs.verticles

import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.*
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.kstream.Consumed
import org.hs.extractor.TransactionTimeExtractor
import org.hs.models.Transaction
import org.hs.models.TransactionCount
import org.hs.serde.JsonSerde
import org.hs.utils.Constants
import org.hs.utils.Utils
import java.time.*
import java.util.*

class TransactionCountTopologyVerticle(
  private val kafkaBroker: String,
  private val verticleName: String
): CoroutineVerticle() {

  private fun topology(): Topology {
    val streamsBuilder = StreamsBuilder()
    val transactionJsonSerde = JsonSerde(Transaction::class.java)

    val aggregates =
      streamsBuilder
        .stream(Constants.TOPIC_TRANSACTION_ALL_CURRENCIES, Consumed.with(Serdes.String(), transactionJsonSerde)
          .withTimestampExtractor(TransactionTimeExtractor()))
        .groupBy({ _, _ -> "window" }, Grouped.with(Serdes.String(), transactionJsonSerde))
        .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofSeconds(60), Duration.ZERO))
        .count(Materialized.with(Serdes.String(), Serdes.Long()))

    aggregates
      .toStream()
      .peek{ k, v -> println("k: $k, v:xz $v") }
      .map { ws, i -> KeyValue(
        "${Utils.convertEpochMillisToString(ws.window().start())}_${Utils.convertEpochMillisToString(ws.window().end())}",
        TransactionCount(
          OffsetDateTime.ofInstant(Instant.ofEpochMilli(ws.window().start()), ZoneOffset.UTC),
          OffsetDateTime.ofInstant(Instant.ofEpochMilli(ws.window().end()), ZoneOffset.UTC),
          i)
        )
      }
      .to(Constants.TOPIC_TRANSACTION_COUNT, Produced.with(Serdes.String(), JsonSerde(TransactionCount::class.java)))
    return streamsBuilder.build()
  }

  override suspend fun start() {
    val kafkaStreamConfig: Properties = Properties().apply {
      put(StreamsConfig.APPLICATION_ID_CONFIG, Constants.TRANSACTION_COUNT_TOPOLOGY_CONSUMER_GROUP)
      put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBroker)
      put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
      put("commit.interval.ms", 0)
    }

    val topology = topology()
    val kafkaStreams = KafkaStreams(topology, kafkaStreamConfig)
    kafkaStreams.start().also {
      println("Started $verticleName topology ... ")
    }

    Runtime.getRuntime().addShutdownHook(Thread { kafkaStreams.close() })
  }

}
