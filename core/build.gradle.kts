// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

// The catalog test reads the real rudiments file shipped in the app.
tasks.test {
    systemProperty("rudimentsJson", rootProject.file("app/src/main/assets/rudiments.json").absolutePath)
}
