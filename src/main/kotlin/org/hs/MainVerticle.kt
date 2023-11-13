package org.hs

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.hs.config.AppConfig
import org.hs.config.AppConfigRetriever
import org.hs.verticles.TransactionConverterTopologyVerticle
import org.hs.verticles.TransactionCountTopologyVerticle

class MainVerticle : CoroutineVerticle() {

  override suspend fun start() {
    // get config
    AppConfigRetriever.getConfig(vertx)
      .onSuccess { config ->

        // deploy web server verticle
        vertx
          .createHttpServer()
          .requestHandler { req ->
            req.response()
              .putHeader("content-type", "text/plain")
              .end("Repository name: ${config.appName}")
          }
          .listen(config.appServingPort) { http ->
            if (http.succeeded()) {
              println("HTTP server started on port ${config.appServingPort}")
            } else {
              println("HTTP server failed to start")
              println(http.cause())
            }
          }
        // deploy other verticles
        Future.all(deployVerticles(vertx, config))
          .onSuccess {
            println("All verticles are deployed successfully")
          }
          .onFailure { throwable ->
            println("Failed to deploy verticles")
            println(throwable.cause)
          }
      }
      .onFailure { throwable ->
        println(throwable.cause)
      }
  }

  fun deployVerticles(vertx: Vertx, config: AppConfig): List<Future<String>> {
    return listOf(
      vertx.deployVerticle(
        TransactionConverterTopologyVerticle(config.kafkaBroker, "currency-conversion")
      ),
      vertx.deployVerticle(
        TransactionCountTopologyVerticle(config.kafkaBroker, "transaction-count")
      )
    )
  }
}
