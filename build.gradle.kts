// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Versiones mínimas seguras para librerías que los plugins de Gradle (AGP/Kotlin) traen de forma
// transitiva. Solo se usan al compilar (no van dentro del APK), pero Dependabot las reporta.
// Se pueden quitar cuando AGP traiga por sí mismo versiones iguales o mayores.
buildscript {
    dependencies {
        constraints {
            classpath("org.bouncycastle:bcprov-jdk18on:1.86") {
                because("GHSA-9pwp-9qqc-pr26 (crítica), GHSA-qp49-qgx5-5m26, GHSA-c3fc-8qff-9hwx")
            }
            classpath("org.bouncycastle:bcpkix-jdk18on:1.86") { because("GHSA-wg6q-6289-32hp") }
            classpath("org.bouncycastle:bcutil-jdk18on:1.86") { because("Alinear con bcprov") }
            classpath("org.bitbucket.b_c:jose4j:0.9.7") { because("GHSA-3677-xxcr-wjqv") }
            classpath("org.jdom:jdom2:2.0.6.1") { because("GHSA-2363-cqg2-863c (XXE)") }
            classpath("org.apache.commons:commons-lang3:3.21.0") { because("GHSA-j288-q9x7-2f5v") }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
