package org.hs.config

import io.vertx.config.ConfigRetriever
import io.vertx.core.Future
import io.vertx.core.Vertx

object AppConfigRetriever {

  fun getConfig(vertx: Vertx): Future<AppConfig> =
    ConfigRetriever.create(vertx).config
      .map { it.toAppConfig() }
}
