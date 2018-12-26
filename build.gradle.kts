import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("de.fayard.buildSrcVersions") version Versions.de_fayard_buildsrcversions_gradle_plugin
    kotlin("jvm") version Versions.org_jetbrains_kotlin_jvm_gradle_plugin
    application
}

application {
    applicationName = "reactive"
    group = "com.notarize"
    mainClassName = "com.notarize.reactive.MainKt"
    version = "0.2.0"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile(Libs.kotlin_stdlib_jdk8)
    compile(Libs.org_eclipse_egit_github_core)
    compile(Libs.snakeyaml)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

sourceSets["main"].withConvention(KotlinSourceSet::class) {
    kotlin.srcDir(file("src/main/kotlin"))
}

sourceSets["test"].withConvention(KotlinSourceSet::class) {
    kotlin.srcDir(file("src/test/kotlin"))
}

tasks {
    withType<Jar> {
        manifest {
            attributes(mapOf("Main-Class" to application.mainClassName))
        }

        archiveName = "${application.applicationName}-$version.jar"
        from(configurations.compile.map { if (it.isDirectory) it else zipTree(it) })
    }

}