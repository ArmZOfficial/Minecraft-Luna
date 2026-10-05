plugins {
    java
}

group = "com.armzofficial"
version = "0.4.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://jitpack.io") {
        content { includeGroup("com.github.MilkBowl") }
    }
}

dependencies {
    // backend ที่ล็อกไว้ใน server/manifest/compatibility-manifest.json
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")

    // soft dependencies — FantasyCore ทำงานได้แม้ไม่มี แต่จะปิดความสามารถที่เกี่ยวข้อง
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.19")
    compileOnly("me.clip:placeholderapi:2.12.3")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") { isTransitive = false }
    // PlaceholderAPI/Paper ใช้ annotation ชุดนี้ใน signature — ต้องมีตอน compile
    compileOnly("org.jetbrains:annotations:26.0.2")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Paper มี sqlite-jdbc ติดมาใน server อยู่แล้ว ใช้ตัวนี้เฉพาะตอนรัน unit test
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.49.1.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks.processResources {
    filteringCharset = "UTF-8"
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("plugin.yml") { expand(props) }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveFileName = "FantasyCore-${project.version}.jar"
}
