import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin ("jvm") version "1.7.21"
  application
  id("com.github.johnrengelman.shadow") version "7.1.2"
  id("org.jsonschema2dataclass") version "6.0.0"
  java
}

group = "org.hs"
version = "1.0.0"

repositories {
  mavenCentral()
}

val vertxVersion = "4.4.6"
val junitJupiterVersion = "5.9.2"

val mainVerticleName = "org.hs.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

val schemaDirectory = "${projectDir}/src/main/resources/schema"
val targetPackage = "org.hs.models"

application {
  mainClass.set(launcherClassName)
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-config")
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-lang-kotlin-coroutines")
  implementation("io.vertx:vertx-lang-kotlin")
  implementation(kotlin("stdlib-jdk8"))
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
  implementation("org.apache.kafka:kafka-streams:3.6.0")
  compileOnly("org.projectlombok:lombok:1.18.30")
  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "17"

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf("run", mainVerticleName, "--redeploy=$watchForChange", "--launcher-class=$launcherClassName", "--on-redeploy=$doOnChange")
}

jsonSchema2Pojo {
  executions {
    create("main") {
      io {
        source.setFrom(files(schemaDirectory))
        sourceType.set("jsonschema")
        targetJavaVersion.set(JavaVersion.VERSION_17.toString())
      }

      klass {
        targetPackage.set("org.hs.models")
        annotationStyle.set("jackson")
        annotateSerializable.set(true)
        jackson2InclusionLevel.set("USE_DEFAULTS")
        nameUseTitle.set(true)
      }

      constructors {
        allProperties.set(true)
        copyConstructor.set(true)
      }

      dateTime {
        dateTimeType.set("java.time.OffsetDateTime")
      }

      fields {
        initializeCollections.set(true)
        integerUseLong.set(true)
      }
    }
  }
}
