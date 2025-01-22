package toni.blahaj

import dev.kikugie.stonecutter.controller.StonecutterController
import dev.kikugie.stonecutter.data.tree.TreeBuilder
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
                target.pluginManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                        maven("https://maven.fabricmc.net/")
                        maven("https://maven.architectury.dev")
                        maven("https://maven.minecraftforge.net")
                        maven("https://maven.neoforged.net/releases/")
                        maven("https://maven.kikugie.dev/snapshots")
                        maven("https://maven.kikugie.dev/releases")
                        maven("https://maven.txni.dev/releases")
                    }
                }

                target.pluginManagement {
                    plugins {
                        id("dev.kikugie.stonecutter").version("0.6-alpha.5").apply(false)
                        id("dev.architectury.loom").version("1.9-SNAPSHOT").apply(false)
                        //id("dev.kikugie.j52j").version("1.0").apply(false)
                        id("me.modmuss50.mod-publish-plugin").version("0.7.4").apply(false)
                        id("systems.manifold.manifold-gradle-plugin").version("0.0.2-alpha").apply(false)
                    }
                }

                target.extensions.create("blahaj", BlahajSettings::class.java, target)
            }
            is Project -> {
                if (target.isStonecutterController()) {
                    target.extensions.create("blahaj", BlahajController::class.java, target)
                    addStonecutterChiseled(target)
                }

                if (target.rootProject.name == target.name)
                    return;

                val subprojectName = target.name
                val platform = if (subprojectName.contains("fabric")) "fabric" else if (subprojectName.contains("neoforge")) "neoforge" else "forge"

                target.extensions.extraProperties["loom.platform"] = platform
                //target.gradle.startParameter.projectProperties["loom.platform"] = platform

                with(target.plugins) {
                    apply("maven-publish")
                    apply("application")
                    apply("org.jetbrains.kotlin.jvm")
                    apply("org.jetbrains.kotlin.plugin.serialization")
                    apply("dev.architectury.loom")
                    apply("me.modmuss50.mod-publish-plugin")
                    apply("systems.manifold.manifold-gradle-plugin")
                }

                target.extensions.create("blahaj", BlahajBuild::class.java, target)
            }
            else -> {
                throw IllegalArgumentException("Unsupported target type: ${target::class.java}")
            }
        }

    }

    fun addStonecutterChiseled(target: Project) {
        val stonecutter = target.extensions.findByType<StonecutterController>()!!

        stonecutter registerChiseled target.tasks.register("chiseledBuild", stonecutter.chiseled) {
            group = "project"
            ofTask("build")
        }

        stonecutter registerChiseled target.tasks.register("chiseledBuildAndCollect", stonecutter.chiseled) {
            group = "project"
            ofTask("buildAndCollect")
        }

        stonecutter registerChiseled target.tasks.register("chiseledBuildAndCopyToModrinth", stonecutter.chiseled) {
            group = "project"
            ofTask("buildAndCopyToModrinth")
        }

        stonecutter registerChiseled target.tasks.register("chiseledPublishMods", stonecutter.chiseled) {
            group = "project"
            ofTask("publishMods")
        }

        stonecutter registerChiseled target.tasks.register("chiseledPublishMaven", stonecutter.chiseled) {
            group = "project"
            ofTask("publish")
        }
    }

     fun Project.isStonecutterController() = when (buildFile.name) {
         "stonecutter.gradle" -> true
         "stonecutter.gradle.kts" -> true
         else -> false
    }

}


@Suppress("MemberVisibilityCanBePrivate", "unused")
open class BlahajSettings internal constructor(val settings: Settings) {

    fun TreeBuilder.mc(version: String, vararg loaders: String) {
        for (it in loaders) {
            val versStr = "$version-$it"

            val dir = File("versions/$versStr")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            vers(versStr, version)
        }
    }

    fun init(rootProject: ProjectDescriptor, configure: TreeBuilder.() -> Unit) {
        // Apply Stonecutter plugin programmatically
        val stonecutter = settings.extensions.findByType<StonecutterSettings>()!!
        stonecutter.apply {
            kotlinController = true
            centralScript = "build.gradle.kts"

            //val rootProject = settings.gradle.rootProject as ProjectDescriptor
            create(rootProject) {
                configure()
            }
        }
    }
}

open class BlahajController internal constructor(val project: Project) {

}