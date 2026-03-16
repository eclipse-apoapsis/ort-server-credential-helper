# Ort Server Credential Helper

The tool to provide credentials to the external tools like GIT or Bazel, 
used by ORT server workers


## GIT credential helper

To use the credential helper for GIT, add the following configuration to `.gitconfig`

```
[credential]
   helper = "/path/to/executable/credentialhelper.kexe git"
```

Helper is using standard `.git-credentials` file to retrieve credentials. It's trying to find best match for given URL, 
so you can have multiple entries in the file, that differ i.e. in path having same host.
