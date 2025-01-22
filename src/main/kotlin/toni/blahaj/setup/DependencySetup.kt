package toni.blahaj.setup

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import toni.blahaj.BlahajBuild
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import systems.manifold.ManifoldExtension

fun DependencyHandler.minecraft(dependencyNotation: Any) = add("minecraft", dependencyNotation)
fun DependencyHandler.implementation(dependencyNotation: Any) = add("implementation", dependencyNotation)
fun DependencyHandler.compileOnly(dependencyNotation: Any) = add("compileOnly", dependencyNotation)
fun DependencyHandler.annotationProcessor(dependencyNotation: Any) = add("annotationProcessor", dependencyNotation)
fun DependencyHandler.modImplementation(dependencyNotation: Any) = add("modImplementation", dependencyNotation)
fun DependencyHandler.modApi(dependencyNotation: Any) = add("modApi", dependencyNotation)
fun DependencyHandler.modRuntimeOnly(dependencyNotation: Any) = add("modRuntimeOnly", dependencyNotation)
fun DependencyHandler.modCompileOnly(dependencyNotation: Any) = add("modCompileOnly", dependencyNotation)
fun DependencyHandler.runtimeOnly(dependencyNotation: Any) = add("runtimeOnly", dependencyNotation)
fun DependencyHandler.include(dependencyNotation: Any) = add("include", dependencyNotation)
fun DependencyHandler.vineflowerDecompilerClasspath(dependencyNotation: Any) = add("vineflowerDecompilerClasspath", dependencyNotation)

fun dependencies(template: BlahajBuild): DependencyHandlerScope.() -> Unit = { val outerDependencyHandler = this.dependencies; template.apply {
    minecraft("com.mojang:minecraft:${mod.mcVersion}")

    implementation(annotationProcessor("systems.manifold:manifold-preprocessor:${project.extensions.getByType<ManifoldExtension>().manifoldVersion.get()}")!!)
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")


    if (template.config.yarn || setting("options.yarn"))
    {
        if (mod.isFabric) {
            add("mappings", getYarnVersion(mod.mcVersion))
        }
        else if (mod.isForge) {
            add("mappings", getYarnVersion(mod.mcVersion) + ":v2")
        }
        else if (mod.isNeo) {
            add("mappings", project.the<LoomGradleExtensionAPI>().layered {
                mappings(getYarnVersion(mod.mcVersion))
                mappings("dev.architectury:yarn-mappings-patch-neoforge:1.21+build.4")
            })
        }
    }
    else {
        add("mappings", project.the<LoomGradleExtensionAPI>().layered {
            officialMojangMappings()
            val parchmentVersion = when (mod.mcVersion) {
                "1.18.2" -> "1.18.2:2022.11.06"
                "1.19.2" -> "1.19.2:2022.11.27"
                "1.20.1" -> "1.20.1:2023.09.03"
                "1.21.1" -> "1.21:2024.07.28"
                "1.21.4" -> "1.21.4:2024.12.22"
                else -> ""
            }
            if (parchmentVersion.isNotEmpty()) {
                parchment("org.parchmentmc.data:parchment-$parchmentVersion@zip")
            }
        })
    }

    val depsHandler = settings
    depsHandler.build = this
    depsHandler.mod = this.mod
    depsHandler.loader = this.loader
    depsHandler.mc = this.mod.mcVersion
    depsHandler.deps = outerDependencyHandler

    if (!depsHandler.isConfigured)
    {
        if (settings.customConfigure != null) settings.customConfigure?.let { it(settings) } else settings.configure()
        depsHandler.isConfigured = true
    }

    depsHandler.blahajDependencies.forEach { it.internalApply() }

    depsHandler.addGlobal()

    if (setting("options.txnilib"))
    {
        val txniLibVersion = project.properties["options.txnilib_version"].toString()
        modImplementation(depsHandler.modloaderRequired("toni.%s:${mod.loader}-${mod.mcVersion}:%s", "txnilib", txniLibVersion))
    }

    if (mod.isFabric) {
        modImplementation(depsHandler.modrinth("modmenu", getVersion("deps.modmenu")))

        depsHandler.addFabric()
        modImplementation("net.fabricmc.fabric-api:fabric-api:${getVersion("deps.fapi")}")
        modImplementation("net.fabricmc:fabric-loader:${getVersion("deps.fabric_loader")}")

//        if (setting("runtime.sodium"))
//            modRuntimeOnly(depsHandler.modrinth("sodium", when (mod.mcVersion) {
//                "1.21.1" -> "mc1.21-0.6.0-beta.1-fabric"
//                "1.20.1" -> "mc1.20.1-0.5.11"
//                else -> null
//            }))

        // JarJar Forge Config API
        if (setting("options.forgeconfig"))
            include(
                when (mod.mcVersion) {
                    "1.19.2" -> modApi("net.minecraftforge:forgeconfigapiport-fabric:${getVersion("deps.forgeconfigapi")}")
                    else -> modApi("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:${getVersion("deps.forgeconfigapi")}")
                }!!
            )
    }

    if (mod.isForge) {
        depsHandler.addForge()
        add("forge", "net.minecraftforge:forge:${mod.mcVersion}-${getVersion("deps.fml")}")
    }

    if (mod.isNeo) {
        depsHandler.addNeo()
        add("neoForge", "net.neoforged:neoforge:${getVersion("deps.fml")}")

        template.loom.neoForge {
            // Configure the NeoForge specific loom settings here if needed
        }

//        if (setting("runtime.sodium"))
//            runtimeOnly(depsHandler.modrinth("sodium", "mc1.21-0.6.0-beta.1-neoforge"))
    }

    vineflowerDecompilerClasspath("org.vineflower:vineflower:1.10.1")
}}

fun getYarnVersion(mcVersion: String): String = when (mcVersion) {
    "1.18.2" -> "net.fabricmc:yarn:1.18.2+build.4"
    "1.19.2" -> "net.fabricmc:yarn:1.19.2+build.28"
    "1.20.1" -> "net.fabricmc:yarn:1.20.1+build.10"
    "1.21.1" -> "net.fabricmc:yarn:1.21.1+build.3"
    "1.21.4" -> "net.fabricmc:yarn:1.21.4+build.4"
    else -> ""
}