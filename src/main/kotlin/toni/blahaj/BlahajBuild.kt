package toni.blahaj

import BlahajSettings
import dev.kikugie.stonecutter.build.StonecutterBuild
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
import org.gradle.kotlin.dsl.*
import systems.manifold.ManifoldExtension
import toni.blahaj.api.BlahajConfigContainer
import toni.blahaj.api.ModData
import toni.blahaj.data.VersionInfo
import toni.blahaj.setup.dependencies
import toni.blahaj.setup.loomSetup
import toni.blahaj.setup.mavenPublish
import toni.blahaj.setup.tasks
import java.io.File

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class BlahajBuild internal constructor(val project: Project)  {
    lateinit var loom: LoomGradleExtensionAPI
    lateinit var projectName : String
    lateinit var loader : String
    lateinit var sc : StonecutterBuild
    lateinit var mod : ModData

    lateinit var modrinthPath: String
    lateinit var modrinthDir: File

    private var isInitialized = false

    var settings : BlahajSettings = BlahajSettings()
    val config: BlahajConfigContainer = BlahajConfigContainer()

    fun setting(prop : String) : Boolean = project.properties.containsKey(prop) && project.properties[prop] == "true"
    fun property(prop : String) : Any? = if (project.properties.containsKey(prop)) project.properties[prop] else null
    fun getVersion(prop : String) : Any? = VersionInfo.getVersion(project.properties, prop, projectName)

    fun initInternal() {
        if (!isInitialized) {
            init()
        }
    }

    fun config(configure: BlahajConfigContainer.() -> Unit) {
        config.apply(configure)
    }

    fun setup(configure: BlahajSettings.() -> Unit) {
        settings.customConfigure = configure
        initInternal()
    }

    fun init() {
        val stonecutter = project.extensions.findByType<StonecutterBuild>();
        if (stonecutter == null)
        {
            System.out.println("[Blahaj] Could not find Stonecutter for project ${project.name}")
            return
        }

        sc = stonecutter ?: throw Exception("Could not find StonecutterBuild!")

        isInitialized = true
        loom = project.extensions.findByType<LoomGradleExtensionAPI>() ?: throw Exception("Could not find Loom!")
        if (loom.platform == null)
            throw Exception("Could not find loom.platform!")

        loader = loom.platform.get().name.lowercase()

        projectName = sc.current.project

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

        project.repositories {
            maven("https://maven.pkg.github.com/ims212/ForgifiedFabricAPI") {
                credentials {
                    username = "IMS212"
                    // Read only token
                    password = "ghp_" + "DEuGv0Z56vnSOYKLCXdsS9svK4nb9K39C1Hn"
                }
            }
            maven("https://www.cursemaven.com")
            maven("https://api.modrinth.com/maven")
            maven("https://thedarkcolour.github.io/KotlinForForge/")
            maven("https://maven.kikugie.dev/releases")
            maven("https://maven.txni.dev/releases")
            maven("https://jitpack.io")
            maven("https://maven.neoforged.net/releases/")
            maven("https://maven.terraformersmc.com/releases/")
            maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
            maven("https://maven.parchmentmc.org")
            maven("https://maven.su5ed.dev/releases")
            maven("https://maven.su5ed.dev/releases")
            maven("https://maven.fabricmc.net")
            maven("https://maven.shedaniel.me/")
            maven("https://maven.fallenbreath.me/releases")
        }

        // The manifold Gradle plugin version. Update this if you update your IntelliJ Plugin!
        project.extensions.getByType<ManifoldExtension>().apply { manifoldVersion = "2024.1.34" }


        // Loom config
        loom.apply(loomSetup(this))

        // Dependencies
        System.out.println("[Blahaj] Dependency Setup")
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
                "${mod.name} ${mod.loader.replaceFirstChar { it.uppercase() }} ${mod.version} for ${mod.mcVersion}"
            version = mod.version
            changelog = project.rootProject.file("CHANGELOG.md").readText()
            type = STABLE
            modLoaders.add(mod.loader)

            val targets = getVersion("mod.mc_targets").toString().split(' ')

            dryRun = project.providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null ||
                    project.providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

            modrinth {
                projectId = property("publish.modrinth").toString()
                version = "${mod.loader}-${mod.mcVersion}-${mod.version}"
                accessToken = project.providers.environmentVariable("MODRINTH_TOKEN")
                targets.forEach(minecraftVersions::add)

                settings.modrinth = this
                if (!settings.isConfigured)
                {
                    if (settings.customConfigure != null) settings.customConfigure?.let { it(settings) } else settings.configure()
                    settings.isConfigured = true
                }

                settings.blahajDependencies.forEach {
                    it.publishCallbacks.forEach { it() }
                }

                if (mod.isFabric)
                    requires("fabric-api")

                if (setting("options.txnilib"))
                    requires("txnilib")
            }

            curseforge {
                projectId = property("publish.curseforge").toString()
                version = "${mod.loader}-${mod.mcVersion}-${mod.version}"
                accessToken = project.providers.environmentVariable("CURSEFORGE_TOKEN")
                targets.forEach(minecraftVersions::add)

                settings.curseforge = this
                if (!settings.isConfigured)
                {
                    if (settings.customConfigure != null) settings.customConfigure?.let { it(settings) } else settings.configure()
                    settings.isConfigured = true
                }

                settings.blahajDependencies.forEach {
                    it.publishCallbacks.forEach { it() }
                }

                if (mod.isFabric)
                    requires("fabric-api")

                if (setting("options.txnilib"))
                    requires("txnilib")
            }
        })

        project.extensions.getByType<PublishingExtension>().apply(mavenPublish(this))
    }



}
