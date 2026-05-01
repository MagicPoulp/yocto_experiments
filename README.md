## Author

Thierry Vilmart
2026

## Buildable Yocto distro with kernel 7 and CVE/SBOM generation

    - Custom mydistro layer targeting qemux86-64 with glibc
    - Linux 7.0.3 via custom kernel recipe with hardened defconfig
    - CVE scanning enabled via INHERIT cve-check on every build
    - Root account locked, non-root user provisioned via extrausers
    - SBOM manifest auto-generated at deploy time

## How to use it

The vulnerabilities report is generated using an option INHERIT += "cve-check"

It is buildable using:
```sh
# have the poky repo cloned next to build
cd build
# Still inside the build directory
source ../poky/oe-init-build-env .

# Build your image
bitbake mydistro-image
```

# Detailed info

See in particualar the shortened new_distro guide.

Or the full_guide file

The illustration below needs to be opened in a text editor

workspace/
├── poky/                    ← Yocto build system
├── meta-mydistro/           ← YOUR layer
│   ├── conf/
│   │   ├── layer.conf
│   │   └── distro/
│   │       └── mydistro.conf   ← YOUR distro definition
│   ├── recipes-core/
│   │   └── images/
│   │       └── mydistro-image.bb
│   └── recipes-kernel/
│       └── linux/
│           └── linux-kernel7_7.0.bb
└── build/                   ← build output
    ├── conf/
    │   └── local.conf
    └── tmp/deploy/images/   ← your final image lands here
