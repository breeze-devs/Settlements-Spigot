import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "1.4.1"
    id("xyz.jpenilla.run-paper") version "2.0.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("net.minecrell.plugin-yml.bukkit") version "0.5.2" // Generates plugin.yml
}

group = "dev.breeze.settlements"
version = "0.3.42"
description = "Enhanced village mechanics"

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    paperDevBundle("1.19.3-R0.1-SNAPSHOT")
    // paperweightDevBundle("com.example.paperfork", "1.19.3-R0.1-SNAPSHOT")

    // You will need to manually specify the full dependency if using the groovy gradle dsl
    // (paperDevBundle and paperweightDevBundle functions do not work in groovy)
    // paperweightDevelopmentBundle("io.papermc.paper:dev-bundle:1.19.3-R0.1-SNAPSHOT")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")

    // Apache commons lang 3.0
    // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
    implementation("org.apache.commons:commons-lang3:3.0")
}

tasks {
    // Configure reobfJar to run when invoking the build task
    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(17)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    reobfJar {
        // This is an example of how you might change the output location for reobfJar. It's recommended not to do this
        // for a variety of reasons, however it's asked frequently enough that an example of how to do it is included here.
        outputJar.set(layout.buildDirectory.file("D:\\One\\Minecraft\\Server\\plugins\\${project.name}-${project.version}.jar"))
    }
}

// Configure plugin.yml generation
bukkit {
    // Default values can be overridden if needed
    // name = "TestPlugin"
    // version = "1.0"
    // description = "This is a test plugin"
    // website = "https://example.com"
    // author = "Notch"

    // Plugin main class (required)
    main = "dev.breeze.settlements.Main"

    // API version (should be set for 1.13+)
    apiVersion = "1.19"

    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    authors = listOf("Breeze")
//  depend = listOf("WorldEdit")
//  softDepend = listOf("Essentials")
//  loadBefore = listOf("BrokenPlugin")
    prefix = "Settlements"
//  defaultPermission = BukkitPluginDescription.Permission.Default.OP // TRUE, FALSE, OP or NOT_OP
//  provides = listOf("TestPluginOldName", "TestPlug")

    commands {
        register("settlements_test") {
            description = "Main command for testing interface"
            aliases = listOf("s_test")
            permission = "${project.name}.admin"
            usage = "Just run the command!"
            permissionMessage = "Permission denied"
        }
    }

    permissions {
        register("${project.name}.placeholder") {
            description = "Placeholder permission, not actually used in operation"
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("${project.name}.admin") {
            description = "Grants all permissions in this plugin"
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }

}
