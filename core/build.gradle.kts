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

// The catalog tests read the real rudiments and styles files shipped in the app.
tasks.test {
    systemProperty("rudimentsJson", rootProject.file("app/src/main/assets/rudiments.json").absolutePath)
    systemProperty("stylesJson", rootProject.file("app/src/main/assets/styles.json").absolutePath)
}
