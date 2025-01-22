package toni.blahaj

import dev.kikugie.stonecutter.build.StonecutterBuild
import dev.kikugie.stonecutter.settings.StonecutterSettings
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.maven
import java.io.File

class BlahajPlugin : Plugin<Any> {

    override fun apply(target: Any) {
        when (target) {
            is Settings -> {
                target.extensions.create("blahaj", BlahajSettings::class.java, target)
            }
            is Project -> {
                if (target.rootProject.name == target.name)
                    return;
                
                val subprojectName = target.name
                val platform = if (subprojectName.contains("fabric")) "fabric" else if (subprojectName.contains("neoforge")) "neoforge" else "forge"

                target.extensions.extraProperties["loom.platform"] = platform
                //target.gradle.startParameter.projectProperties["loom.platform"] = platform

                target.extensions.create("blahaj", BlahajBuild::class.java, target)
            }
            else -> {
                throw IllegalArgumentException("Unsupported target type: ${target::class.java}")
            }
        }

    }

}



@Suppress("MemberVisibilityCanBePrivate", "unused")
open class BlahajSettings internal constructor(val settings: Settings) {
    fun init(rootProject: ProjectDescriptor) {
        // Apply Stonecutter plugin programmatically
        // settings.pluginManagement {
        //     repositories {
        //         mavenCentral()
        //         gradlePluginPortal()
        //         maven("https://maven.fabricmc.net/")
        //         maven("https://maven.architectury.dev")
        //         maven("https://maven.minecraftforge.net")
        //         maven("https://maven.neoforged.net/releases/")
        //         maven("https://maven.kikugie.dev/snapshots")
        //         maven("https://maven.kikugie.dev/releases")
        //         maven("https://maven.txni.dev/releases")
        //     }
        // }

        // settings.pluginManagement {
        //     plugins {
        //         id("dev.kikugie.stonecutter").version("0.6-alpha.5")
        //         id("dev.architectury.loom").version("1.9-SNAPSHOT")
        //     }
        // }

        val stonecutter = settings.extensions.findByType<StonecutterSettings>()!!
        stonecutter.apply {
            kotlinController = true
            centralScript = "build.gradle.kts"

            //val rootProject = settings.gradle.rootProject as ProjectDescriptor
            create(rootProject) {
                fun mc(version: String, vararg loaders: String) {
                    for (it in loaders) {
                        val versStr = "$version-$it"

                        val dir = File("versions/$versStr")
                        if (!dir.exists()) {
                            dir.mkdirs()
                        }

                        vers(versStr, version)
                    }
                }

                mc("1.20.1", "fabric", "forge")
                mc("1.21.1", "fabric", "neoforge")
            }
        }
    }
}