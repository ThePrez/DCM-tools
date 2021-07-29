# DCM Tools for IBM i
Command-line tools for working with Digital Certificate Manager (DCM) on IBM i.

Currently still under development and without a published release. Proceed at your own risk. 

# Current features

Currently, the only tool in this toolset is `dcmimport`, which is used to
add CAs as trusted entities in DCM. 

It can be used to import files of type:
- Binary DER-encoded certificate files
- Binary DER-encoded certificate bundles
- Human-readable DER-encoded certificate files
- Human-readable DER-encoded certificate bundles
- JKS trust stores
- JCEKS trust stores
- PKCS#12 bundles
It can also be used to fetch certificates from a remote host and import to DCM.

# Have feedback or want to contribute?
Feel free to [open an issue](https://github.com/ThePrez/DCM-tools/issues/new/choose) with any questions, problems, or other comments. If you'd like to contribute to the project, see [CONTRIBUTING.md](https://github.com/ThePrez/DCM-tools/blob/main/CONTRIBUTING.md) for more information on how to get started. 

In any event, we're glad to have you aboard in any capacity, whether as a user, spectator, or contributor!

# Installation

`make install`

# Basic usage

Usage of the command is summarized as:
```text
Usage: dcmimport  [options] [[filename] ..]

    Valid options include:
        -y:                            Do not ask for confirmation
        --password[=password]:         Indicate that the input file is password-protected,
                                       and optionally provide a password
        --target=<system/filename>:    Specify the target keystore, or specify 'system'
                                       to indicate the *SYSTEM store (default)
        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)
        --fetch-from=<hostname>[:port] Fetch the certificate from the given hostname/port
        --installed-certs:             import all certificates that are installed into PASE
                                       environment, for instance, certificates in the
                                       ca-certificates-mozilla package

```

## Usage examples
Import certs from file `myfile`:
```
dcmimport myfile
```
Import all PASE-installed certificates (such as `ca-certificates-mozilla`) into DCM, without asking questions:
```
dcmimport --installed-certs --target=system --dcm-password=abc123 -y
```
Import the Java certificates from JV1's Java 8
```
dcmimport /QOpenSys/QIBM/ProdData/JavaVM/jdk80/64bit/jre/lib/security/cacerts
```