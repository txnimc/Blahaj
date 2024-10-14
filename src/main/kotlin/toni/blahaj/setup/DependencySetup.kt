package toni.blahaj.setup

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import toni.blahaj.BlahajBuild
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.configure
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
fun DependencyHandler.runtimeOnly(dependencyNotation: Any) = add("runtimeOnly", dependencyNotation)
fun DependencyHandler.include(dependencyNotation: Any) = add("include", dependencyNotation)
fun DependencyHandler.vineflowerDecompilerClasspath(dependencyNotation: Any) = add("vineflowerDecompilerClasspath", dependencyNotation)

fun dependencies(template: BlahajBuild): DependencyHandlerScope.() -> Unit = { template.apply {
    minecraft("com.mojang:minecraft:${mod.mcVersion}")

    implementation(annotationProcessor("systems.manifold:manifold-preprocessor:${project.extensions.getByType<ManifoldExtension>().manifoldVersion.get()}")!!)
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    add("mappings", project.the<LoomGradleExtensionAPI>().layered {
        officialMojangMappings()
        val parchmentVersion = when (mod.mcVersion) {
            "1.18.2" -> "1.18.2:2022.11.06"
            "1.19.2" -> "1.19.2:2022.11.27"
            "1.20.1" -> "1.20.1:2023.09.03"
            "1.21.1" -> "1.21:2024.07.28"
            else -> ""
        }
        if (parchmentVersion.isNotEmpty()) {
            parchment("org.parchmentmc.data:parchment-$parchmentVersion@zip")
        }
    })

    if (setting("options.txnilib"))
        modImplementation("toni.txnilib:${mod.loader}-${mod.mcVersion}:${project.properties["options.txnilib_version"]}")

    settings.depsHandler.addGlobal(mod, dependencies)

    if (mod.isFabric) {
        modImplementation(settings.depsHandler.modrinth("modmenu", property("deps.modmenu")))

        settings.depsHandler.addFabric(mod, dependencies)
        modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fapi")}")
        modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")

        if (setting("runtime.sodium"))
            modRuntimeOnly(settings.depsHandler.modrinth("sodium", when (mod.mcVersion) {
                "1.21.1" -> "mc1.21-0.6.0-beta.1-fabric"
                "1.20.1" -> "mc1.20.1-0.5.11"
                else -> null
            }))

        // JarJar Forge Config API
        if (setting("options.forgeconfig"))
            include(
                when (mod.mcVersion) {
                    "1.19.2" -> modApi("net.minecraftforge:forgeconfigapiport-fabric:${property("deps.forgeconfigapi")}")
                    else -> modApi("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:${property("deps.forgeconfigapi")}")
                }!!
            )
    }

    if (mod.isForge) {
        settings.depsHandler.addForge(mod, dependencies)
        add("forge", "net.minecraftforge:forge:${mod.mcVersion}-${property("deps.fml")}")
    }

    if (mod.isNeo) {
        settings.depsHandler.addNeo(mod, dependencies)
        add("neoForge", "net.neoforged:neoforge:${property("deps.fml")}")

        template.loom.neoForge {
            // Configure the NeoForge specific loom settings here if needed
        }

        if (setting("runtime.sodium"))
            runtimeOnly(settings.depsHandler.modrinth("sodium", "mc1.21-0.6.0-beta.1-neoforge"))
    }

    vineflowerDecompilerClasspath("org.vineflower:vineflower:1.10.1")
}}