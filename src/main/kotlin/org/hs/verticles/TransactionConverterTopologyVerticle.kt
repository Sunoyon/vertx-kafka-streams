package org.hs.verticles

import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.hs.models.Transaction
import org.hs.serde.JsonSerde
import java.util.*

class TransactionConverterTopologyVerticle (
  private val kafkaBroker: String,
) : CoroutineVerticle() {

  companion object {
    const val ALL_CURRENCIES_TRANSACTION_TOPIC = "transaction.all_currencies"
    const val USD_TRANSACTION_TOPIC = "transaction.usd"
    const val CONSUMER_GROUP = "transaction-converter"

    val usdConversionRate = mapOf(
      Transaction.Currency.USD to 1.0, // USD to USD
      Transaction.Currency.EUR to 1.2, // EUR to USD
      Transaction.Currency.GBP to 1.4, // GBP to USD
    )
  }


  /**
   * The topology will validate the transaction
   * and convert the currency value to USD currency
   */
  private fun topology(): Topology {
    val streamsBuilder = StreamsBuilder()
    val transactionJsonSerde = JsonSerde(Transaction::class.java)

    streamsBuilder
      .stream(ALL_CURRENCIES_TRANSACTION_TOPIC, Consumed.with(Serdes.String(), transactionJsonSerde))
      .filter{ _, value -> value.currency in usdConversionRate.keys && value.amount > 0}
      .mapValues { _, value -> convertToUsd(value) }
      .to(USD_TRANSACTION_TOPIC, Produced.with(Serdes.String(), transactionJsonSerde))

    return streamsBuilder.build()
  }

  private fun convertToUsd(transaction: Transaction): Transaction {
    val conversionRate = usdConversionRate[transaction.currency]!!

    val convertedTransaction = Transaction(transaction)
    convertedTransaction.amount = convertedTransaction.amount * conversionRate
    convertedTransaction.currency = Transaction.Currency.USD
    return convertedTransaction
  }

  override suspend fun start() {
    val kafkaStreamConfig: Properties = Properties().apply {
      put(StreamsConfig.APPLICATION_ID_CONFIG, CONSUMER_GROUP)
      put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBroker)
      put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
//      put("commit.interval.ms", 0)
    }

    val topology = topology()
    val kafkaStreams = KafkaStreams(topology, kafkaStreamConfig)
    kafkaStreams.start().also {
      println("Started currency-conversion topology ... ")
    }

    Runtime.getRuntime().addShutdownHook(Thread { kafkaStreams.close() })
  }
}
