SUMMARY = "Linux Kernel 7.0"
SECTION = "kernel"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

inherit kernel

# Fetch kernel 7.0.3 directly from kernel.org
SRC_URI = "https://cdn.kernel.org/pub/linux/kernel/v7.x/linux-7.0.3.tar.xz"
SRC_URI[sha256sum] = "0bedadbf5788693ddebbcc913c893f1a97349af79ddde7144c2a80b401959f1c"

S = "${WORKDIR}/linux-7.0.3"

LINUX_VERSION = "7.0.3"
PV = "7.0.3"

# Point to your kernel config
#SRC_URI += "file://defconfig"

COMPATIBLE_MACHINE = "qemux86-64"
DEPENDS += "elfutils-native ncurses-native zlib-native openssl-native bison-native flex-native"
