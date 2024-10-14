# Blahaj
Minecraft multiversion plugin, with full mod project management, built on Stonecutter

To get started, you should refer to the TxniTemplate documentation, a barebones template mod that utilizes `blahaj`.

https://template.txni.dev/

In short, you will need to set up your project with Stonecutter, reference and then apply the plugin, and initialize it
with a TxniTemplateSettings object. This interface is intended to provide a single configuration location for
99% of Gradle setup during mod development.

```kotlin
plugins { 
	id("toni.blahaj") version "1.0.4"
}

val templateSettings = object : TxniTemplateSettings { ... }

blahaj {
	sc = stonecutter
	settings = templateSettings
	init()
}

// Dependencies
repositories {
	maven("https://maven.txni.dev/releases") 
}
```