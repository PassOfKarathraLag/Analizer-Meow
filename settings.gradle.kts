pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // necesario para MPAndroidChart
    }
}
rootProject.name = "AnalizerMeow"
include(":app")
