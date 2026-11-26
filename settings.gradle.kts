pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SnakeDrop"

// Pure JVM modules: always included, buildable without the Android SDK.
include(":engine-core", ":game-snakedrop", ":tools")

// Android modules: skip with -PpureOnly (e.g. CI or a machine without the SDK).
if (!providers.gradleProperty("pureOnly").isPresent) {
    include(":engine-render", ":engine-audio", ":platform", ":app")
}
