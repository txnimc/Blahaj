package toni.blahaj.api

class BlahajConfigContainer {
    var yarn: Boolean = false
    fun yarn() { yarn = true }

    var versionedAccessWideners: Boolean = false
    fun versionedAccessWideners() { versionedAccessWideners = true }

    var platformSpecificAccessWideners: Boolean = false
    fun platformSpecificAccessWideners() { platformSpecificAccessWideners = true }

    var versionedMixins: Boolean = false
    fun versionedMixins() { versionedMixins = true }

    var platformSpecificMixins: Boolean = false
    fun platformSpecificMixins() { platformSpecificMixins = true }
}
