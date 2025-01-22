import me.modmuss50.mpp.platforms.curseforge.CurseforgeDependencyContainer
import me.modmuss50.mpp.platforms.modrinth.ModrinthDependencyContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import toni.blahaj.BlahajBuild
import toni.blahaj.api.ModData
import toni.blahaj.setup.modImplementation
import toni.blahaj.setup.modCompileOnly
import toni.blahaj.setup.modRuntimeOnly

val templateSettings = object : BlahajSettings() {
    override fun configure() {
        addRequiredMod("sodium")
            .modrinth()
            .addPlatform("1.21.1-neoforge", "mc1.21.1-0.6.5-neoforge")
            .addPlatform("1.21.1-fabric", "mc1.21.1-0.6.5-fabric") { required() }

        addRequiredMod("txnilib", "1.0.1")
            .modImplementation { id, version -> "toni.${id}:${loader}-${mc}:${version}" }
    }
}

open class BlahajSettings {
    public lateinit var build : BlahajBuild
    public lateinit var mod : ModData
    public lateinit var loader : String
    public lateinit var mc : String
    public lateinit var deps: DependencyHandler
    public var curseforge: CurseforgeDependencyContainer? = null
    public var modrinth: ModrinthDependencyContainer? = null

    public var blahajDependencies : MutableList<BlahajDependency> = mutableListOf()
    public var publishCallbacks : MutableList<() -> Unit?> = mutableListOf()

    public var isConfigured: Boolean = false
    public var customConfigure:  (BlahajSettings.() -> Unit)? = null

    fun addMod(modID: String) : BlahajDependency {
        val dep = BlahajDependency(this, modID)
        blahajDependencies.add(dep)
        return dep
    }

    fun addMod(modID: String, version : String) : BlahajDependency {
        val dep = BlahajDependency(this, modID)
        dep.setVersion(version)
        blahajDependencies.add(dep)
        return dep
    }

    fun addRequiredMod(modID: String) : BlahajDependency {
        val dep = BlahajDependency(this, modID)
        dep.required()
        blahajDependencies.add(dep)
        return dep
    }

    fun addRequiredMod(modID: String, version : String) : BlahajDependency {
        val dep = BlahajDependency(this, modID)
        dep.setVersion(version)
        dep.required()
        blahajDependencies.add(dep)
        return dep
    }

    fun modrinth(name: String, dep: Any?) = "maven.modrinth:$name:$dep"

//    fun modrinthDepends(modID: String, version: Any?, overrideVersion : String? = null) : String {
//        build.mod.depends.putIfAbsent(modID, overrideVersion ?: version.toString())
//        return "maven.modrinth:$modID:$version"
//    }
//
//    fun modrinthDepends(modID: String, version: Any?, overrideVersion : String? = null, overrideModID : String? = null) : String {
//        build.mod.depends.putIfAbsent(overrideModID ?: modID, overrideVersion ?: version.toString())
//        return "maven.modrinth:$modID:$version"
//    }
//
//    fun modrinthDependsAny(modID: String, version: Any?, overrideModID : String? = null) : String {
//        build.mod.depends.putIfAbsent(overrideModID ?: modID, "*")
//        return "maven.modrinth:$modID:$version"
//    }

    fun modloaderRequired(format : String, modID: String, version: String) : String {
        build.mod.depends.putIfAbsent(modID, version)
        return String.format(format, modID, version)
    }

    fun modloaderRequired(modID : String, version: String) {
        build.mod.depends.putIfAbsent(modID, version)
    }

    fun modloaderRequired(modID : String) {
        build.mod.depends.putIfAbsent(modID, "*")
    }

    fun requiredWith(vararg slugs : String) {
        requiredWithCurseforge(*slugs)
        requiredWithModrinth(*slugs)
    }

    fun requiredWithCurseforge(vararg slugs : String)  {
        publishCallbacks.add { curseforge?.requires(*slugs) }
    }

    fun requiredWithModrinth(vararg slugs : String) {
        publishCallbacks.add { modrinth?.requires(*slugs)}
    }

    fun incompatibleWith(vararg slugs : String) {
        incompatibleWithCurseforge(*slugs)
        incompatibleWithModrinth(*slugs)
    }

    fun incompatibleWithCurseforge(vararg slugs : String)  {
        publishCallbacks.add { curseforge?.incompatible(*slugs) }
    }

    fun incompatibleWithModrinth(vararg slugs : String) {
        publishCallbacks.add { modrinth?.incompatible(*slugs)}
    }


    open fun configure() {}
    open fun addGlobal() {}
    open fun addFabric() {}
    open fun addForge() {}
    open fun addNeo() {}
}

class BlahajDependency(val parent: BlahajSettings, val modID: String) {
    private var version : String? = null
    private var versions : MutableMap<String, String> = mutableMapOf()
    private var binding : ((id: String, version: String) -> Dependency?)? = null
    public var publishCallbacks : MutableList<() -> Unit?> = mutableListOf()

    fun internalApply() {
        val target = version ?: versions.getOrDefault(parent.build.projectName, null);
        if (target == null) {
            throw Exception("Could not find version ${parent.build.projectName} for mod $modID among configured platforms!")
        }

        binding?.invoke(modID, target)
    }

    fun bind(function: (id: String, version: String) -> Dependency? ) : BlahajDependency
    {
        binding = function
        return this
    }

    fun modrinth(slug: String? = null) : BlahajDependency {
        binding = { id, version -> parent.deps.modImplementation(parent.modrinth(slug ?: id, version)) }
        return this
    }

    fun modrinthCompile(slug: String? = null) : BlahajDependency {
        binding = { id, version -> parent.deps.modCompileOnly(parent.modrinth(slug ?: id, version)) }
        return this
    }

    fun modrinthRuntime(slug: String? = null) : BlahajDependency {
        binding = { id, version -> parent.deps.modRuntimeOnly(parent.modrinth(slug ?: id, version)) }
        return this
    }

    fun modImplementation(format: (id: String, version: String) -> String ) : BlahajDependency {
        binding = { id, version -> parent.deps.modImplementation(format(id, version)) }
        return this
    }

    fun setVersion(version: String) : BlahajDependency {
        this.version = version
        return this
    }

    fun modloaderRequired(vers: String = "*") : BlahajDependency {
        parent.modloaderRequired(modID, vers)
        return this
    }

    fun modloaderRequiredFabric(vers: String = "*") : BlahajDependency {
        if (parent.mod.isFabric)
            parent.modloaderRequired(modID, vers)

        return this
    }

    fun modloaderRequiredForge(vers: String = "*") : BlahajDependency {
        if (parent.mod.isForge)
            parent.modloaderRequired(modID, vers)

        return this
    }

    fun modloaderRequiredNeo(vers: String = "*") : BlahajDependency {
        if (parent.mod.isNeo)
            parent.modloaderRequired(modID, vers)

        return this
    }

    fun addPlatform(platform: String, version: String) : BlahajDependency {
        versions[platform] = version
        return this
    }

    fun addPlatform(platform: String, version: String, function: BlahajDependency.(String) -> Unit = {}) : BlahajDependency {
        addPlatform(platform, version)

        if (parent.build.projectName == version)
            function(version)

        return this
    }


    fun required()  : BlahajDependency {
        requiredCurseforgeAndModrinth(modID)
        modloaderRequired()
        return this
    }


    fun requiredCurseforgeAndModrinth()  : BlahajDependency {
        requiredCurseforge(modID)
        requiredModrinth(modID)
        return this
    }

    fun requiredCurseforgeAndModrinth(vararg slugs : String)  : BlahajDependency {
        requiredCurseforge(*slugs)
        requiredModrinth(*slugs)
        return this
    }

    fun requiredCurseforge(vararg slugs : String)  : BlahajDependency {
        publishCallbacks.add { parent.curseforge?.requires(*slugs) }
        return this
    }

    fun requiredModrinth(vararg slugs : String)  : BlahajDependency {
        publishCallbacks.add { parent.modrinth?.requires(*slugs)}
        return this
    }


    fun optionalCurseforgeAndModrinth()  : BlahajDependency {
        optionalCurseforge(modID)
        optionalModrinth(modID)
        return this
    }

    fun optionalCurseforgeAndModrinth(vararg slugs : String)  : BlahajDependency {
        optionalCurseforge(*slugs)
        optionalModrinth(*slugs)
        return this
    }

    fun optionalCurseforge(vararg slugs : String)  : BlahajDependency {
        publishCallbacks.add { parent.curseforge?.optional(*slugs) }
        return this
    }

    fun optionalModrinth(vararg slugs : String)  : BlahajDependency {
        publishCallbacks.add {  parent.modrinth?.optional(*slugs) }
        return this
    }
}