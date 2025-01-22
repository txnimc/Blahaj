package toni.blahaj.data

class VersionInfo private constructor() {
    companion object {
        private val versionDefaults: MutableMap<String, MutableMap<String, String>> = mutableMapOf(
            // Forge Version
            "deps.fml" to mutableMapOf(
                "1.20.1-forge" to "47.2.16",
                "1.21.1-neoforge" to "21.1.96"
            ),
            // Fabric Version
            "deps.fabric_loader" to mutableMapOf(
                "1.20.1-fabric" to "0.16.10",
                "1.21.1-fabric" to "0.16.10",
                "1.21.4-fabric" to "0.16.10"
            ),
            // Fabric API
            "deps.fapi" to mutableMapOf(
                "1.20.1-fabric" to "0.91.0+1.20.1",
                "1.21.1-fabric" to "0.102.1+1.21.1",
                "1.21.4-fabric" to "0.114.3+1.21.4"
            ),
            // Forge Config API Port
            "deps.forgeconfigapi" to mutableMapOf(
                "1.20.1-fabric" to "8.0.0",
                "1.21.1-fabric" to "21.1.0",
                "1.21.4-fabric" to "21.4.1"
            ),
            // Mod Menu
            "deps.modmenu" to mutableMapOf(
                "1.20.1-fabric" to "7.2.2",
                "1.21.1-fabric" to "11.0.2",
                "1.21.4-fabric" to "13.0.0",
            ),
            // Minecraft Dependency Block
            "mod.mc_dep" to mutableMapOf(
                "1.20.1-fabric" to ">=1.20 <=1.20.1",
                "1.20.1-forge" to "[1.20.1]",
                "1.21.1-fabric" to ">=1.21",
                "1.21.1-neoforge" to "[1.21.1,)",
                "1.21.4-fabric" to ">=1.21.4",
            ),
            // Curseforge/Modrinth Version Targets
            "mod.mc_targets" to mutableMapOf(
                "1.20.1-fabric" to "1.20 1.20.1",
                "1.20.1-forge" to "1.20 1.20.1",
                "1.21.1-fabric" to "1.21.1",
                "1.21.1-neoforge" to "1.21.1",
                "1.21.4-fabric" to "1.24.1"
            )
        )

        fun getVersion(gradleProperties: Map<String, *>, propertyKey: String, versionString: String) : String? {
            var gradleVersion = gradleProperties[propertyKey] as? String
            if (gradleVersion == "[VERSIONED]" || gradleVersion == "VERSIONED")
                gradleVersion = null

            return gradleVersion ?: versionDefaults[propertyKey]?.get(versionString)
        }

        fun addOrUpdateDefault(propertyKey: String, versionString: String, version: String) {
            versionDefaults.computeIfAbsent(propertyKey) { mutableMapOf() }[versionString] = version
        }

        fun getVersionDefaults(): Map<String, Map<String, String>> {
            return versionDefaults
        }
    }
}