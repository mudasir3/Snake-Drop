plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// AdMob IDs come from Gradle properties (e.g. ~/.gradle/gradle.properties), never from the repo.
// Without them the app uses Google's public sample IDs, which only ever serve test ads.
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val testRewardedId = "ca-app-pub-3940256099942544/5224354917"

fun adId(
    name: String,
    fallback: String,
): String = (project.findProperty(name) as String?) ?: fallback

val buildingRelease = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
if (buildingRelease && !project.hasProperty("admobAppId") && !project.hasProperty("allowTestAds")) {
    throw GradleException(
        "Release build needs real AdMob IDs: set admobAppId, admobInterstitialId, admobRewardedId " +
            "(or pass -PallowTestAds to build a release with Google's test ads, never to publish).",
    )
}

android {
    namespace = "com.snakedrop.app"
    compileSdk = 35
    defaultConfig {
        // TODO(applicationId): placeholder until the software-house name is chosen. Never change after first upload.
        applicationId = "com.snakedrop.game.tbd"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["admobAppId"] = adId("admobAppId", testAppId)
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${adId("admobInterstitialId", testInterstitialId)}\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"${adId("admobRewardedId", testRewardedId)}\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":engine-render"))
    implementation(project(":engine-audio"))
    implementation(project(":platform"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
