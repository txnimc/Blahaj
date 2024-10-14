package toni.blahaj

import toni.blahaj.api.DependencyContainer
import BlahajSettings
import dev.kikugie.stonecutter.StonecutterBuild
import me.modmuss50.mpp.ModPublishExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import systems.manifold.ManifoldExtension
import toni.blahaj.api.ModData
import toni.blahaj.setup.dependencies
import toni.blahaj.setup.loomSetup
import toni.blahaj.setup.mavenPublish
import toni.blahaj.setup.tasks
import java.io.File

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class BlahajBuild internal constructor(val project: Project)  {
    lateinit var loom: LoomGradleExtensionAPI
    lateinit var loader : String
    lateinit var sc : StonecutterBuild
    lateinit var settings : BlahajSettings
    lateinit var mod : ModData

    lateinit var modrinthPath: String
    lateinit var modrinthDir: File

    fun setting(prop : String) : Boolean = project.properties.containsKey(prop) && project.properties[prop] == "true"
    fun property(prop : String) : Any? = if (project.properties.containsKey(prop)) project.properties[prop] else null

    fun init() {
        loom = project.extensions.findByType<LoomGradleExtensionAPI>()!!
        loader = loom.platform.get().name.lowercase()
        mod = ModData.from(this)

        modrinthDir = File(project.properties["client.modrinth_profiles_dir"].toString())
        modrinthPath = when (loader) {
            "fabric" -> "Fabric ${mod.mcVersion}"
            "neoforge" -> "NeoForge ${mod.mcVersion}"
            "forge" -> "Forge ${mod.mcVersion}"
            else -> ""
        }

        // Versioning Setup
        project.run {
            version = "${mod.version}-${mod.mcVersion}"
            group = mod.group

            val baseExtension = project.extensions.getByType(BasePluginExtension::class.java)
            baseExtension.archivesName.set("${mod.id}-${mod.loader}")
        }

        // The manifold Gradle plugin version. Update this if you update your IntelliJ Plugin!
        project.extensions.getByType<ManifoldExtension>().apply { manifoldVersion = "2024.1.34" }


        // Loom config
        loom.apply(loomSetup(this))
        // Dependencies
        DependencyHandlerScope.of(project.dependencies).apply(dependencies(this))
        // Tasks
        project.tasks.apply(tasks(this))


        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        mainSourceSet.resources {
            srcDir("src/main/generated")
            exclude(".cache/")
        }

        sc.apply {
            val j21 = eval(mod.mcVersion, ">=1.20.6")
            project.extensions.getByType(JavaPluginExtension::class.java).apply {
                withSourcesJar()
                sourceCompatibility = if (j21) JavaVersion.VERSION_21 else JavaVersion.VERSION_17
                targetCompatibility = if (j21) JavaVersion.VERSION_21 else JavaVersion.VERSION_17
            }
        }

        // this won't let me move it to a different class so fuck it, it goes here
        project.extensions.getByType<ModPublishExtension>().apply(fun ModPublishExtension.() {
            file = project.tasks.named("remapJar", RemapJarTask::class.java).get().archiveFile
            additionalFiles.from(project.tasks.named("remapSourcesJar", RemapSourcesJarTask::class.java).get().archiveFile)
            displayName =
                "${mod.name} ${mod.loader.replaceFirstChar { it.uppercase() }} ${mod.version} for ${property("mod.mc_title")}"
            version = mod.version
            changelog = project.rootProject.file("CHANGELOG.md").readText()
            type = STABLE
            modLoaders.add(mod.loader)

            val targets = property("mod.mc_targets").toString().split(' ')

            dryRun = project.providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null ||
                    project.providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

            modrinth {
                projectId = property("publish.modrinth").toString()
                accessToken = project.providers.environmentVariable("MODRINTH_TOKEN")
                targets.forEach(minecraftVersions::add)
                val deps = DependencyContainer(null, this)
                settings.publishHandler.addModrinth(mod, deps)
                settings.publishHandler.addShared(mod, deps)
                addTxniDeps(deps)
            }

            curseforge {
                projectId = property("publish.curseforge").toString()
                accessToken = project.providers.environmentVariable("CURSEFORGE_TOKEN")
                targets.forEach(minecraftVersions::add)
                val deps = DependencyContainer(this, null)
                settings.publishHandler.addCurseForge(mod, deps)
                settings.publishHandler.addShared(mod, deps)
                addTxniDeps(deps)
            }
        })

        project.extensions.getByType<PublishingExtension>().apply(mavenPublish(this))
    }

    private fun addTxniDeps(deps: DependencyContainer) {
        if (setting("options.txnilib"))
            deps.requires("txnilib")
    }


}
