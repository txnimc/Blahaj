import org.gradle.api.artifacts.dsl.DependencyHandler
import toni.blahaj.api.ModData
import toni.blahaj.api.DependencyContainer

interface TxniTemplateSettings {
    val depsHandler : TxniDependencyHandler
    val publishHandler : TxniPublishDependencyHandler
}


interface TxniDependencyHandler {
    fun modrinth(name: String, dep: Any?) = "maven.modrinth:$name:$dep"

    fun addGlobal(mod : ModData, deps: DependencyHandler)
    fun addFabric(mod : ModData, deps: DependencyHandler)
    fun addForge(mod : ModData, deps: DependencyHandler)
    fun addNeo(mod : ModData, deps: DependencyHandler)
}

interface TxniPublishDependencyHandler {
    fun addShared(mod : ModData, deps: DependencyContainer)
    fun addCurseForge(mod : ModData, deps: DependencyContainer)
    fun addModrinth(mod : ModData, deps: DependencyContainer)
}