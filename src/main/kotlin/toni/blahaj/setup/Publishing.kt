package toni.blahaj.setup

import toni.blahaj.BlahajBuild
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get

fun mavenPublish(template : BlahajBuild) : PublishingExtension.() -> Unit = { template.apply {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${property("mod.group")}.${mod.id}"
            version = mod.version
            artifactId = "${mod.loader}-${mod.mcVersion}" //base.archivesName.get()

            from(project.components["java"])
        }
    }

    repositories {
        val username = "MAVEN_USERNAME".let { System.getenv(it) ?: project.findProperty(it) }?.toString()
        val password = "MAVEN_PASSWORD".let { System.getenv(it) ?: project.findProperty(it) }?.toString()

        if (username == null || password == null) {
            println("No maven credentials found.")
            return@repositories;
        }

        val mavenURI = if (project.properties["publish.use_snapshot_maven"] == "true") "snapshots" else "releases"

        maven {
            name = "${mod.author}_$mavenURI"
            url = project.uri("https://${property("publish.maven_url").toString()}/$mavenURI")
            credentials {
                this.username = System.getenv("MAVEN_USERNAME")
                this.password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}}