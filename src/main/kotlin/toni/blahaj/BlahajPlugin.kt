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
import org.gradle.kotlin.dsl.plugins
import java.io.File

class BlahajPlugin : Plugin<Any> {

    override fun apply(target: Any) {
        when (target) {
            is Settings -> {
                target.extensions.create("blahaj", BlahajSettings::class.java, target)
            }
            is Project -> {
                if (target.isStonecutterController()) {
                    target.extensions.create("blahaj", BlahajController::class.java, target)
                    addStonecutterChiseled(target)
                    return
                }

                if (target.rootProject.name == target.name)
                    return

                val subprojectName = target.name
                val platform = if (subprojectName.contains("fabric")) "fabric" else if (subprojectName.contains("neoforge")) "neoforge" else "forge"

                target.extensions.extraProperties["loom.platform"] = platform

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

        stonecutter registerChiseled target.tasks.register("buildAll", stonecutter.chiseled) {
            group = "blahaj"
            ofTask("buildAndCollectLatest")
        }

        stonecutter registerChiseled target.tasks.register("copyToModrinthLauncher", stonecutter.chiseled) {
            group = "blahaj"
            ofTask("buildAndCopyToModrinth")
        }

        stonecutter registerChiseled target.tasks.register("publishAllRelease", stonecutter.chiseled) {
            group = "blahaj"
            ofTask("publishMods")
        }

        stonecutter registerChiseled target.tasks.register("publishAllMaven", stonecutter.chiseled) {
            group = "blahaj"
            ofTask("publish")
        }


        target.tasks.register("bumpVersionAndChangelog") {
            group = "blahaj"
            doLast {
                val gradleProperties = target.file("gradle.properties")
                val gradlePropertiesContent = gradleProperties.readText()

                val versionRegex = Regex("""mod\.version=(\d+)\.(\d+)\.(\d+)""")
                val matchResult = versionRegex.find(gradlePropertiesContent)
                if (matchResult == null) {
                    println("Error: mod.version not found in gradle.properties.")
                    return@doLast
                }

                val (major, minor, patch) = matchResult.destructured

                println("Update type? (major, minor, patch):")
                val updateInput = readlnOrNull() ?: "patch"

                val newVersion = when (updateInput) {
                    "major" -> "${major.toInt() + 1}.$minor.$patch"
                    "minor" -> "$major.${minor.toInt() + 1}.$patch"
                    "patch" -> "$major.$minor.${patch.toInt() + 1}"
                    else -> "$major.$minor.${patch.toInt() + 1}"
                }

                val updatedPropertiesContent = gradlePropertiesContent.replace(
                    versionRegex,
                    "mod.version=$newVersion"
                )

                gradleProperties.writeText(updatedPropertiesContent)

                println("Enter the changelog for version $newVersion (separate entries with semicolons):")
                val changelogInput = readLine() ?: ""
                val changelogEntries = changelogInput.split(";").map { "- ${it.trim()}" }

                val changelogFile = target.file("CHANGELOG.md")
                val changelogContent = changelogFile.takeIf { it.exists() }?.readText() ?: ""

                val newChangelogContent = buildString {
                    append("## $newVersion\n")
                    append(changelogEntries.joinToString("\n"))
                    append("\n\n")
                    append(changelogContent)
                }

                changelogFile.writeText(newChangelogContent)

                println("Version bumped to $newVersion in gradle.properties.")
                println("Changelog updated with the following entries:")
                changelogEntries.forEach { println(it) }
            }
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

            System.out.println("[Blahaj] " + settings.rootDir.toString())

            val dir = File(settings.rootDir, "versions/$versStr")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val props = File(settings.rootDir, "versions/$versStr/gradle.properties")
            if (!props.exists()){
                props.writeText("loom.platform=$it")
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