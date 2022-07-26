import java.net.URI
import java.util.Properties

plugins {
  `java-library`
  `maven-publish`
  id("io.papermc.paperweight.userdev") version "1.3.5"
  id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "ru.shk"
version = "1.3.36"

val nexusRepository = Properties()
nexusRepository.load(file("nexus.properties").inputStream())
publishing {
  repositories {
    maven {
      url = URI.create("https://nexus.shoker.su/repository/maven-releases/")
      credentials {
        username = "${nexusRepository["user"]}"
        password = "${nexusRepository["password"]}"
      }
    }
  }
  publications {
    create<MavenPublication>("maven") {
      groupId = "${group}"
      artifactId = "${project.name}"
      version = "${version}"

      artifact("/build/libs/${project.name}.jar")
    }
  }
}
java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
repositories {
  maven {
    url = URI.create("https://nexus.shoker.su/repository/maven-releases/")
  }
  maven {
    url = URI.create("https://maven.enginehub.org/repo/")
  }
  maven {
    url = URI.create("https://papermc.io/repo/repository/maven-public/")
  }
  maven {
    url = URI.create("https://oss.sonatype.org/content/groups/public/")
  }
  maven {
    url = URI.create("https://libraries.minecraft.net/")
  }
  maven {
    url = URI.create("https://repo.codemc.io/repository/maven-snapshots/")
  }
  maven {
    url = URI.create("https://mvn.exceptionflug.de/repository/exceptionflug-public/")
  }
  maven {
    url = URI.create("https://nexus.shoker.su/repository/maven-shield/")
    credentials {
        username = "shield"
        password = "KQBVXvQh7fedYNU"
    }
  }
}

dependencies {
  paperDevBundle("1.18.2-R0.1-SNAPSHOT")
  compileOnly(files("D:/Libraries/spigot-1.17.1.jar"))
  implementation("org.apache.commons:commons-lang3:3.12.0")
  compileOnly(files("D:/Libraries/worldedit-bukkit-7.3.0.jar"))
  compileOnly(files("D:/Libraries/worldedit-core-7.3.0.jar"))
  implementation("commons-io:commons-io:2.11.0")
  compileOnly("dev.simplix:protocolize-api:2.1.2")
  compileOnly("net.md-5:bungeecord-api:1.18-R0.1-SNAPSHOT")
  implementation(files("net.wesjd:anvilgui:1.5.3-SNAPSHOT"))
  compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")
  compileOnly("com.mojang:authlib:1.5.21")

  implementation("org.projectlombok:lombok:1.18.22")
  annotationProcessor("org.projectlombok:lombok:1.18.22")

  compileOnly("land.shield:PlayerAPI:1.4.0")
  compileOnly("ru.shk:MySQLAPI:2.0.1")
  compileOnly(files("D:/Libraries/ProtocolLib.jar"))
  // paperweightDevBundle("com.example.paperfork", "1.18.2-R0.1-SNAPSHOT")

  // You will need to manually specify the full dependency if using the groovy gradle dsl
  // (paperDevBundle and paperweightDevBundle functions do not work in groovy)
  // paperweightDevelopmentBundle("io.papermc.paper:dev-bundle:1.18.2-R0.1-SNAPSHOT")
}

tasks {
  // Configure reobfJar to run when invoking the build task
  assemble {
    dependsOn(reobfJar)
    dependsOn(shadowJar)
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
    val props = kotlin.Pair<String, Any>("version", version)
    filesMatching("*.yml"){
      expand(props)
    }
  }

  reobfJar {
    // This is an example of how you might change the output location for reobfJar. It's recommended not to do this
    // for a variety of reasons, however it's asked frequently enough that an example of how to do it is included here.
    outputJar.set(layout.buildDirectory.file("libs/${project.name}.jar"))
  }
}
//task("deleteUnused") {
//  delete("build/libs/*-dev*.jar")
//}
tasks.create<Delete>("deleteUnused"){
  delete("build/libs/${project.name}-${project.version}-dev.jar", "build/libs/${project.name}-${project.version}-dev-all.jar")
}
