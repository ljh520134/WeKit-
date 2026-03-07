plugins {
    kotlin("jvm") version "2.3.10" apply false
    kotlin("kapt") version "2.3.10" apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
}
