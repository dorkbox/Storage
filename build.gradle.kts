/*
 * Copyright 2023 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

///////////////////////////////
//////    PUBLISH TO SONATYPE / MAVEN CENTRAL
////// TESTING : (to local maven repo) <'publish and release' - 'publishToMavenLocal'>
////// RELEASE : (to sonatype/maven central), <'publish and release' - 'publishToSonatypeAndRelease'>
///////////////////////////////

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS   // always show the stacktrace!

plugins {
    id("com.dorkbox.GradleUtils") version "3.17"
    id("com.dorkbox.Licensing") version "2.26"
    id("com.dorkbox.VersionUpdate") version "2.8"
    id("com.dorkbox.GradlePublish") version "1.18"

    kotlin("jvm") version "1.8.0"
}
// TODO: - have an HTTP put/fetch based storage. (where storage can run as the client and server).
//       - allow bridging between storage types so http server can store as XYZ, easily
//       - make this also integrate with the cache?
object Extras {
    // set for the project
    const val description = "Storage system for Java"
    const val group = "com.dorkbox"
    const val version = "1.10"

    // set as project.ext
    const val name = "Storage"
    const val id = "Storage"
    const val vendor = "Dorkbox LLC"
    const val vendorUrl = "https://dorkbox.com"
    const val url = "https://git.dorkbox.com/dorkbox/Storage"
}

/////////////////////////////
///  assign 'Extras'
/////////////////////////////
GradleUtils.load("$projectDir/../../gradle.properties", Extras)
GradleUtils.defaults()
GradleUtils.compileConfiguration(JavaVersion.VERSION_1_8)
GradleUtils.jpms(JavaVersion.VERSION_1_9)


licensing {
    license(License.APACHE_2) {
        description(Extras.description)
        author(Extras.vendor)
        url(Extras.url)
    }
}

tasks.jar.get().apply {
    manifest {
        // https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html
        attributes["Name"] = Extras.name

        attributes["Specification-Title"] = Extras.name
        attributes["Specification-Version"] = Extras.version
        attributes["Specification-Vendor"] = Extras.vendor

        attributes["Implementation-Title"] = "${Extras.group}.${Extras.id}"
        attributes["Implementation-Version"] = GradleUtils.now()
        attributes["Implementation-Vendor"] = Extras.vendor

        attributes["Automatic-Module-Name"] = Extras.id
    }
}

dependencies {
    api("com.dorkbox:ByteUtilities:2.0")
    api("com.dorkbox:Json:1.7")
    api("com.dorkbox:MinLog:2.5")
    api("com.dorkbox:ObjectPool:4.4")
    api("com.dorkbox:Serializers:2.9")
    api("com.dorkbox:Updates:1.1")

    // really fast storage
    // https://github.com/lmdbjava/lmdbjava
//    compileOnly("org.lmdbjava:lmdbjava:0.8.2")

    // https://github.com/OpenHFT/Chronicle-Map
//    compileOnly("net.openhft:chronicle-map:3.21.86")


    implementation("org.slf4j:slf4j-api:2.0.9")


    api("com.esotericsoftware:kryo:5.5.0") {
        exclude("com.esotericsoftware", "minlog") // we use our own minlog, that logs to SLF4j instead
    }

    // really fast storage
    // https://github.com/lmdbjava/lmdbjava
    testImplementation("org.lmdbjava:lmdbjava:0.8.2")

    // https://github.com/OpenHFT/Chronicle-Map
    testImplementation("net.openhft:chronicle-map:3.21.86")

    testImplementation("junit:junit:4.13.2")
    testImplementation("ch.qos.logback:logback-classic:1.4.5")
    testImplementation("org.agrona:agrona:1.14.0")
}

publishToSonatype {
    groupId = Extras.group
    artifactId = Extras.id
    version = Extras.version

    name = Extras.name
    description = Extras.description
    url = Extras.url

    vendor = Extras.vendor
    vendorUrl = Extras.vendorUrl

    issueManagement {
        url = "${Extras.url}/issues"
        nickname = "Gitea Issues"
    }

    developer {
        id = "dorkbox"
        name = Extras.vendor
        email = "email@dorkbox.com"
    }
}
