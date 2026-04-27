# Ort Server Credential Helper

This repository contains multiple implementations of credential helpers that are used to pass credentials managed by ORT Server to external tools.

A _credential helper_ is a small executable that is called by external tools to retrieve credentials for a given URL. The protocol how to invoke the helper and how to return the credentials is defined by the external tool; also, the configuration how to use the helper is tool-specific.

The Gradle build in this repository produces multiple executables in different modules making use of [Kotlin Native](https://kotlinlang.org/docs/native-overview.html). The binaries are included in the ORT Server worker container images, so that they are available in the single steps of a run. They could also be used in other contexts though. Typically, they implement specific functionality required for ORT Server which is not part of already existing credential helper implementations.

The implementation strategy is to have a common framework that defines the format of the credentials and provides the logic to match them against requested URLs. Concrete credential helper implementations then mainly focus on the protocol used by the external tool they interact with.

Since the credential helpers have their own release cycle and can be used independently of ORT Server, they are hosted in a separate repository.

## Git credential helper

This is a [credentials helper](https://git-scm.com/docs/gitcredentials) implementation for [Git](https://git-scm.com/). It uses the same [.git-credentials](https://git-scm.com/docs/git-credential-store#_storage_format) file as the built-in Git credential helper, but adds support for multiple entries with the same host, but different paths. This is needed for ORT Server, which has to support hosting platforms used by different development teams with different credentials. The implementation tries to find the best-matching entry for a given URL based on the host and path. For instance, given the following entries in the credentials file:

```
https://user1:password1@github.com
https://user2:password2@github.com/org1
https://user3:password3@github.com/org1/repo1.git
```

* a request for the URL `https://github.com/org1/repo1.git` would return the credentials `user3:password3` (full match)
* a request for the URL `https://github.com/org1/another-repo.git` would return the credentials `user2:password2` (match for the organization)
* a request for the URL `https://github.com/another-org/repo1.git` would return the credentials `user1:password1` (match for the host)

To use the credential helper for GIT, add the following configuration to `.gitconfig`:

```
[credential]
   helper = "/path/to/executable/credentialhelper.kexe"
   useHttpPath = true
```
