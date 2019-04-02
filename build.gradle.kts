plugins {
  maven
  `java-library`
  kotlin("jvm") version "1.2.60"
}

tasks.getByName<Wrapper>("wrapper") {
  gradleVersion = "5.3.1"
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_1_8.toString()
    freeCompilerArgs = listOf("-Xjvm-default=enable")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
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

  testImplementation("io.kotlintest:kotlintest-runner-junit5:3.1.9")
}
