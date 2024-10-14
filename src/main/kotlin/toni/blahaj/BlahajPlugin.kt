package toni.blahaj

import org.gradle.api.Plugin
import org.gradle.api.plugins.ExtensionAware


class BlahajPlugin : Plugin<ExtensionAware> {


    override fun apply(target: ExtensionAware) {
        target.extensions.create("blahaj", BlahajBuild::class.java, target)
    }
}
