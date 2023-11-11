package org.hs.config

import io.vertx.core.json.JsonObject

data class AppConfig(
  val appName: String,
  val appServingPort: Int,
  val kafkaBroker: String,
)

fun JsonObject.toAppConfig(): AppConfig = AppConfig(
  appName = this.getString("APP_NAME"),
  appServingPort = this.getInteger("SERVING_PORT"),
  kafkaBroker = this.getString("KAFKA_BROKER"),
)
