# Ort Server Credential Helper

This repository contains multiple implementations of credential helpers that are used to pass credentials managed by ORT Server to external tools.

A _credential helper_ is a small executable that is called by external tools to retrieve credentials for a given URL. The protocol how to invoke the helper and how to return the credentials is defined by the external tool; also, the configuration how to use the helper is tool-specific.

The Gradle build in this repository produces multiple executables in different modules making use of [Kotlin Native](https://kotlinlang.org/docs/native-overview.html). The binaries are included in the ORT Server worker container images, so that they are available in the single steps of a run. They could also be used in other contexts though. Typically, they implement specific functionality required for ORT Server which is not part of already existing credential helper implementations.

The implementation strategy is to have a common framework that defines the format of the credentials and provides the logic to match them against requested URLs. Concrete credential helper implementations then mainly focus on the protocol used by the external tool they interact with.

Since the credential helpers have their own release cycle and can be used independently of ORT Server, they are hosted in a separate repository.

## GIT credential helper

To use the credential helper for GIT, add the following configuration to `.gitconfig`

```
[credential]
   helper = "/path/to/executable/credentialhelper.kexe git"
```

Helper is using standard `.git-credentials` file to retrieve credentials. It's trying to find best match for given URL, 
so you can have multiple entries in the file, that differ i.e. in path having same host.
