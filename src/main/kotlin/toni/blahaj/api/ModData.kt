package toni.blahaj.api

import toni.blahaj.BlahajBuild

data class ModData (
    val id : String,
    val name : String,
    val version : String,
    val group : String,
    val author : String,
    val namespace : String,
    val displayName : String,
    val description : String,
    val discord : String,
    val mcDep : String,
    val license : String,
    val github : String,
    val clientuser  : String,
    val clientuuid : String,
    val mcVersion : String,
    val isActive : Boolean,
    val loader : String,
    val projectName : String,
    val isFabric : Boolean,
    val isForge : Boolean,
    val isNeo : Boolean,
    val depends : MutableMap<String, String>) {

    companion object {
        fun from(txni : BlahajBuild) : ModData {
            return ModData(
                txni.project.properties["mod.id"].toString(),
                txni.project.properties["mod.name"].toString(),
                txni.project.properties["mod.version"].toString(),
                txni.project.properties["mod.group"].toString(),
                txni.project.properties["mod.author"].toString(),
                txni.project.properties["mod.namespace"].toString(),
                txni.project.properties["mod.display_name"].toString(),
                txni.project.properties["mod.description"].toString(),
                txni.project.properties["mod.discord"].toString(),
                txni.getVersion("mod.mc_dep").toString(),
                txni.project.properties["mod.license"].toString(),
                txni.project.properties["mod.github"].toString(),
                txni.project.properties["client.user"].toString(),
                txni.project.properties["client.uuid"].toString(),
                txni.sc.current.project.substringBeforeLast('-'),
                txni.sc.active.project == txni.sc.current.project,
                txni.loader,
                txni.sc.current.project,
                txni.loader == "fabric",
                txni.loader == "forge",
                txni.loader == "neoforge",
                mutableMapOf()
            )

        }
    }

    public fun getDependsBlock() : String {
        val sb = StringBuilder()
        for (depend in depends) {
            var value = depend.value;
            if (isFabric) {
                if (value != "*")
                    value = ">=$value"
                sb.append("\"${depend.key}\": \"${value}\",")
            } else {
                if (value != "*")
                    value = "[$value,)"
                sb.append("[[dependencies.\"${id}\"]]\n" +
                        "modId=\"${depend.key}\"\n" +
                        "mandatory=true\n" +
                        "versionRange=\"${value}\"\n" +
                        "ordering=\"NONE\"\n" +
                        "side=\"BOTH\"")
            }
        }

        return sb.toString()
    }
}