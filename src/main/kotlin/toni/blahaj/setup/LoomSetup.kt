package toni.blahaj.setup

import toni.blahaj.BlahajBuild
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.kotlin.dsl.get


fun loomSetup(template : BlahajBuild) : LoomGradleExtensionAPI.() -> Unit = { template.apply {
    val awPath = when {
        template.config.versionedAccessWideners || template.setting("options.versioned_aw") -> "src/main/resources/${mod.id}_${mod.mcVersion}.accesswidener"
        template.config.platformSpecificAccessWideners -> "src/main/resources/${mod.id}_${mod.loader}_${mod.mcVersion}.accesswidener"
        else -> "src/main/resources/${mod.id}.accesswidener"
    }

    val awFile = project.rootProject.file(awPath)
    if (awFile.exists())
    {
        accessWidenerPath.set(awFile)
        if (mod.loader == "forge")
            forge { convertAccessWideners.set(true) }
    }

    if (mod.loader == "forge") forge {
        mixinConfigs("mixins.${if (template.config.platformSpecificMixins) "${mod.id}_${mod.loader}_${mod.mcVersion}" else if (template.config.versionedMixins) "${mod.id}_${mod.mcVersion}" else mod.id}.json")
    }

    if (mod.isActive) {
        runConfigs.all {
            ideConfigGenerated(true)
            vmArgs("-Dmixin.debug.export=true", "-Dsodium.checks.issue2561=false")
            // Mom look I'm in the codebase!
            programArgs("--username=${mod.clientuser}", "--uuid=${mod.clientuuid}")
            runDir = "../../run/${sc.current.project}/"
        }
    }

    decompilers {
        get("vineflower").apply {
            options.put("mark-corresponding-synthetics", "1")
        }
    }

    runs {
        register("datagen") {
            client()
            name("DataGen Client")
            vmArg("-Dfabric-api.datagen")
            vmArg("-Dfabric-api.datagen.output-dir=" + project.rootDir.toPath().resolve("src/main/generated"))
            vmArg("-Dfabric-api.datagen.modid=${mod.id}")
            ideConfigGenerated(false)
            runDir("build/datagen")
        }
    }
}}