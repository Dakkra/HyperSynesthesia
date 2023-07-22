/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    `java-library`
    `maven-publish`
    id("org.openjfx.javafxplugin") version "0.0.14"
}

group = "com.dakkra"
version = "0.0.1"
description = "hypersynesthesia"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://repo.avereon.com/avn")
    }
}

dependencies {
    testImplementation("org.assertj:assertj-core:[3.24,)")
    testImplementation("org.junit.jupiter:junit-jupiter:[5.9,)")
    compileOnly("org.projectlombok:lombok:1.18.26")
    compileOnly("com.avereon:xenon:[1.7-SNAPSHOT,)")
}

java {
    withSourcesJar()
    withJavadocJar()
}

javafx {
    version = "17"
    modules("javafx.web", "javafx.swing", "javafx.fxml", "javafx.graphics", "javafx.controls")
}

//publishing {
//    publications.create<MavenPublication>("maven") {
//        from(components["java"])
//    }
//}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}
