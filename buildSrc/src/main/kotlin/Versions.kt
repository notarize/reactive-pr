import kotlin.String

/**
 * Find which updates are available by running
 *     `$ ./gradlew buildSrcVersions`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version. */
object Versions {
    const val de_fayard_buildsrcversions_gradle_plugin: String = "0.3.2" 

    const val org_eclipse_egit_github_core: String = "2.1.5" // available: "3.4.0.201406110918-r"

    const val org_jetbrains_kotlin_jvm_gradle_plugin: String = "1.3.11" 

    const val org_jetbrains_kotlin: String = "1.3.11" 

    const val snakeyaml: String = "1.23" 

    /**
     *
     *   To update Gradle, edit the wrapper file at path:
     *      ./gradle/wrapper/gradle-wrapper.properties
     */
    object Gradle {
        const val runningVersion: String = "4.10"

        const val currentVersion: String = "5.0"

        const val nightlyVersion: String = "5.2-20181218000039+0000"

        const val releaseCandidate: String = "5.1-rc-2"
    }
}
