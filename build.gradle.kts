import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  maven
  `java-library`
  kotlin("jvm") version "1.2.60"
}
tasks.withType<Wrapper> {
  gradleVersion = "4.9"
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_1_8.toString()
    freeCompilerArgs = listOf("-Xjvm-default=enable")
  }
}

repositories {
  jcenter()
}
dependencies {
  implementation(create(kotlin("stdlib"), closureOf<ExternalModuleDependency> {
    exclude("org.jetbrains", "annotations")
  }))
  compileOnly("org.jetbrains:annotations:16.0.2")
  api("com.github.ben-manes.caffeine:caffeine:2.6.2")
}
