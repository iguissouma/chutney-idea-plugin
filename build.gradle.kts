plugins {
    id("org.jetbrains.intellij") version "0.4.10"
    java
    kotlin("jvm") version "1.3.61"
}

configurations.all {
    exclude("org.sl4j")
}

group = "com.chutneytesting.idea"
version = "1.7-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven {
        url = uri("https://repo1.maven.org/maven2")
    }
    flatDir {
        dirs = setOf(file("libs"))
    }
    maven { url = uri("https://dl.bintray.com/jetbrains/intellij-plugin-service") }
}

dependencies {
    implementation(fileTree("libs"))
    implementation("com.google.guava", "guava", "28.1-jre")
    implementation("org.hjson", "hjson", "2.1.1")
    implementation("com.fasterxml.jackson.core", "jackson-core", "2.9.9")
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.9.9")
    implementation("com.fasterxml.jackson.core", "jackson-annotations", "2.9.9")
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.9.9")
    implementation("me.andrz.jackson", "jackson-json-reference-core", "0.3.2") {
        // exclude("org.sl4j") does not exclude
        isTransitive = false // this exclude "org.sl4j"
    }
    implementation("com.github.wnameless.json", "json-flattener", "0.8.1")
    implementation("org.jetbrains.kotlin", "kotlin-main-kts", "1.3.72")
    implementation("org.jetbrains.kotlin","kotlin-script-runtime", "1.3.72")
    implementation("org.jetbrains.kotlin", "kotlin-scripting-jvm",  "1.3.72")
    implementation("org.jetbrains.kotlin","kotlin-compiler-embeddable", "1.3.72")
    implementation("org.jetbrains.kotlin","kotlin-script-util", "1.3.72")
    implementation("org.jetbrains.kotlin","kotlin-scripting-compiler-embeddable", "1.3.72")
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("junit", "junit", "4.12")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "IU-193.6494.35"
    pluginName = "chutney-idea-plugin"
    downloadSources = false
    updateSinceUntilBuild = false
    setPlugins("java", "Spring", "yaml", "kotlin")
    tasks{
        buildSearchableOptions{
            enabled = false
        }
    }

}
configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.wrapper {
    gradleVersion = "6.2"
    distributionType = Wrapper.DistributionType.ALL
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes(
        """
      Add change notes here.<br>
      <em>most HTML tags may be used</em>"""
    )
}
