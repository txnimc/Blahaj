@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("me.modmuss50.mod-publish-plugin") version "0.6.3" apply false
    id("systems.manifold.manifold-gradle-plugin") version "0.0.2-alpha" apply false
}

group = "toni.blahaj"
version = "1.0.4"

// Repositories for dependencies
repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.architectury.dev")
    maven("https://maven.minecraftforge.net")
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.kikugie.dev/snapshots")
    maven("https://maven.kikugie.dev/releases")
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
    maven("https://maven.txni.dev/releases")
    maven("https://maven.su5ed.dev/releases")
    maven("https://maven.shedaniel.me/")
    maven("https://www.cursemaven.com")
    maven("https://api.modrinth.com/maven")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    maven("https://libraries.minecraft.net")
}

// Dependencies
dependencies {
    implementation("me.modmuss50:mod-publish-plugin:0.6.3")
    implementation("dev.architectury.loom:dev.architectury.loom.gradle.plugin:1.7-SNAPSHOT") {
        exclude("com.mojang")
    }
    implementation("systems.manifold:manifold-gradle-plugin:0.0.2-alpha")
    implementation("dev.kikugie:stonecutter:0.5-alpha.4")
}

gradlePlugin {
    website = "https://template.txni.dev/"
    vcsUrl = "https://github.com/txni/Blahaj"

    plugins {
        create("blahaj") {
            id = "toni.blahaj"
            implementationClass = "toni.blahaj.BlahajPlugin"
            displayName = "Blahaj"
            description = "Minecraft multiversion plugin for full mod project management, built on Stonecutter"
            tags = setOf("minecraft", "mods")
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()

    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_16)
    }
}


// Publishing configuration
publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = project.group.toString()
            artifactId = "blahaj"
            version = project.version.toString()
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://maven.txni.dev/releases")

            credentials {
                // Ensure credentials are correctly fetched and not null
                username = findProperty("MAVEN_USERNAME") as String? ?: System.getenv("MAVEN_USERNAME")
                password = findProperty("MAVEN_PASSWORD") as String? ?: System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}