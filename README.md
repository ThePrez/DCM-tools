# DCM Tools for IBM i
Command-line tools for working with Digital Certificate Manager (DCM) on IBM i.

Can be used in conjunction with [CertBot](https://ibmi-oss-docs.readthedocs.io/en/latest/certbot.html) to automate the acquisition/assignment/renewal of LetsEncrypt certificates

Currently still under development and without complete testing. Proceed at your own risk. I'm not kidding.

# Current features

### `dcmimport`

Used to import certificates into DCM.

It can be used to import files of type:
- Binary DER-encoded certificate files
- Binary DER-encoded certificate bundles
- Human-readable DER-encoded certificate files
- Human-readable DER-encoded certificate bundles
- JKS trust stores
- JCEKS trust stores
- PKCS#12 or PFX bundles
- A directory containing any of the above
- A `.zip` file containing any of the above

It can also be used to fetch certificates from a remote host and import to DCM.

### `dcmexport`

Used to export the entire DCM keystore to file

### `dcmexportcert`

Used to export a single certificate from a DCM keystore to file

### `dcmassign`

Used to assign a certificate to a registered application

### `dcmrenew`

Used to renew a certificate, given a new certificate file, for instance, a new LetsEncrypt certificate from CertBot [CertBot](https://ibmi-oss-docs.readthedocs.io/en/latest/certbot.html)

### `dcmview`

View contents of a certificate store

### `dcmremovecert`

Remove a certificate from a certificate store

### `dcmrenamecert`

Rename a certificate in a certificate store

### `dcmcreate`

Create a certificate store

### `dcmchangepw`

Change a certificate store password



# Future features

Check out the issues board for this project to see things that may be future enhancements to this toolset

# Have feedback or want to contribute?
Feel free to [open an issue](https://github.com/ThePrez/DCM-tools/issues/new/choose) with any questions, problems, or other comments. If you'd like to contribute to the project, see [CONTRIBUTING.md](https://github.com/ThePrez/DCM-tools/blob/main/CONTRIBUTING.md) for more information on how to get started. 

In any event, we're glad to have you aboard in any capacity, whether as a user, spectator, or contributor!

# IBM i release requirements
The ability to "Renew" a certificate (with the `dcmrenew` tool) requires IBM i 7.4.

# Installation

```
yum install https://github.com/ThePrez/DCM-tools/releases/download/v0.3.0/dcmtools-0.3.0-0.ibmi7.2.ppc64.rpm
```
Or, to build from source, clone this repository and run `make install`
Note: yum install might fail when the IBM i job is not set to US English language settings. It is recommended to change the job
settings CHGJOB LANGID(ENU) CNTRYID(US) CCSID(37) prior to starting the install process.

# Basic usage

Usage of the command is summarized as:
```text
Usage: dcmimport  [options] [[filename] ..]

    Valid options include:
        -y:                            Do not ask for confirmation
        --password[=password]:         Indicate that the input file is password-protected,
                                       and optionally provide a password
        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'
                                       to indicate the *SYSTEM store (default)
        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)
        --fetch-from=<hostname>[:port] Fetch CA certificate(s) from the given hostname/port
        --ca-only                      Only import CA Certificates
        --cert=<id>                    Recommend a certificate ID when imported into DCM
        --installed-certs:             import all certificates that are installed into PASE
                                       environment, for instance, certificates in the
                                       ca-certificates-mozilla package


Usage: dcmexport <filename>

    Valid options include:
        -y:                            Do not ask for confirmation
        --password[=password]:         Indicate that the output file is password-protected,
                                       and optionally provide a password
        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'
                                       to indicate the *SYSTEM store (default)
        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)
        --format=<format>              Format of the output file (jceks, pks, pkcs12).
                                        (default: pkcs12)
                                        
                          
Usage: dcmexportcert [options] <filename>

    Valid options include:
        -y:                            Do not ask for confirmation
        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'
                                       to indicate the *SYSTEM store (default)
        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)
        --cert=<id>:                   ID of the certificate to export
        --format=<format>:             Format of the output file (PEM/DER).
                                        (default: PEM)


Usage: dcmassign [options] <application_id>...

    Valid options include:
        -y:                              Do not ask for confirmation
        --cert=<id>:                     Certificate ID to assign
        --dcm-store=<system/filename>:   Specify the DCM certificate store, or specify 'system'
                                         to indicate the *SYSTEM store (default)

    For application id, specify the id as defined in DCM, or a 'shorthand' identifier.
    Valid shorthand identifiers include:
        5250
        TELNET
        HOSTSERVERS
        HOSTSERVER
        HOSTSVR
        CENTRAL
        DATABASE
        DTAQ
        NETPRT
        RMTCMD
        SIGNON
        FILE
        DIRSRV
        SMTP
        FTP
        POP
        OBJC
        
 
Usage: dcmrenew [[filename] ..]

    Valid options include:
        -y:                            Do not ask for confirmation

Usage: dcmview [options]

    Valid options include:
        -y:                            Do not ask for confirmation
        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'
                                       to indicate the *SYSTEM store (default)
        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)

Usage: dcmemovecert  [options]

    Valid options include:
        -y:                            Do not ask for confirmation
        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'
                                       to indicate the *SYSTEM store (default)
        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)
        --label=<label>:               Label of the certificate to remove

Usage: dcmrenamecert  [options]

    Valid options include:
        -y:                            Do not ask for confirmation
        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'
                                       to indicate the *SYSTEM store (default)
        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)
        --old-label=<label>:           Label of the certificate to rename
        --new-label=<label>:           Label of the certificate to rename

Usage: dcmcreate  [options]

    Valid options include:
        -y:                            Do not ask for confirmation
        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'
                                       to indicate the *SYSTEM store (default)
        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)

Usage: dcmchangepw  [options]

    Valid options include:
        -y:                            Do not ask for confirmation
        --dcm-store=<system/filename>: Specify the target keystore, or specify 'system'
                                       to indicate the *SYSTEM store (default)
        --dcm-password=<password>:     Provide the DCM keystore password (not recommended)
        --password[=password]:         Provide new password (not recommended)


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
Renew a LetsEncrypt certificate
```
/opt/certbot/bin/certbot renew
dcmrenew /etc/letsencrypt/live/mydomain.dom/fullchain.pem
```

# Testimonials
> "I wrote some code. It seems to work. ¯\\_(ツ)_/¯"
>
> &nbsp; --[@ThePrez](https://github.com/ThePrez/), creator of DCM Tools
