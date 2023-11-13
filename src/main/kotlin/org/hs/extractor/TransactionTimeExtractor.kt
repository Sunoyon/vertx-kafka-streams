package org.hs.extractor

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.streams.processor.TimestampExtractor
import org.hs.models.Transaction

class TransactionTimeExtractor : TimestampExtractor {
  override fun extract(record: ConsumerRecord<Any, Any>, partitionTime: Long): Long {
    val transaction: Transaction = record.value() as Transaction
    return transaction.transactionTimestamp?.toEpochSecond()?.times(1000L) ?: partitionTime
  }
}
