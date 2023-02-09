# Dokka Gradle Plugin 2

Re-implemented Dokka Gradle Plugin.

* Uses Gradle Worker Daemon to run Dokka Generation in a separate process
* Safe handling of multiple Gradle subprojects
* Supports Build Cache and Configuration Cache
* Functional tests using Gradle TestKit

### Project set up

The Dokka Gradle Plugin 2 is set up as an included build, so it is independent of the Dokka project.

### Running tests

Run tests (from the base Dokka project directory)

```bash
./gradlew :runners:gradle-plugin-2:check
```

Check the sample projects in [`build/functional-tests`](./build/functional-tests).

#### Functional tests

Functional tests use Gradle TestKit to run the plugin as it would in a real project.

The functional test pick up the Dokka Gradle Plugin via a file-based Maven repository, which is published into
[`build/test-maven-repo`](./build/test-maven-repo). This is passed to the functional tests via the `testMavenRepoDir`
system property.

By default, the functional tests will produce output in [`build/functional-tests`](./build/functional-tests).
This is configured via the `funcTestTempDir` system property.
This directory will be automatically cleaned by Gradle.
