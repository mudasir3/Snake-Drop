plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":game-snakedrop"))
}

application {
    mainClass.set("com.snakedrop.tools.SimulationFuzzerKt")
}

tasks.register<JavaExec>("runFuzzer") {
    group = "verification"
    description = "Plays thousands of seeded bot runs and checks engine invariants after every step."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.snakedrop.tools.SimulationFuzzerKt")
    args = listOf("10000")
}
