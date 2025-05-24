package dev.arabicdictionary.pro

import dev.arabicdictionary.pro.utils.applyIfNeeded
import dev.arabicdictionary.pro.utils.libs
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named

plugins.applyIfNeeded("java")
private val ktLintConfig = configurations.create("ktlint")

dependencies {
    "ktlint"(libs.ktlint.cli) {
        attributes {
            attribute(
                Bundling.BUNDLING_ATTRIBUTE,
                getObjects().named<Bundling>(Bundling.EXTERNAL),
            )
        }
    }
}

private val sources = listOf("**/src/**/*.kt", "!**/build/generated/**")
private val reportsDirPath = project.layout.buildDirectory.dir("reports/ktlint").get().asFile.path
private val ktLintCliMainClass = "com.pinterest.ktlint.Main"
private val reportArgs = listOf(
    "--reporter=plain",
    "--reporter=checkstyle,output=$reportsDirPath/ktlint.xml",
    "--reporter=html,output=$reportsDirPath/ktlint.html",
)

val ktLintCheckTask = tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Check Kotlin code style."
    classpath = ktLintConfig
    mainClass = ktLintCliMainClass
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args = reportArgs + sources
}

tasks.named("check") {
    dependsOn(ktLintCheckTask)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Fix Kotlin code style deviations."
    classpath = ktLintConfig
    mainClass = ktLintCliMainClass
    jvmArgs = listOf("--add-opens=java.base/java.lang=ALL-UNNAMED")
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args = listOf("-F") + sources
}