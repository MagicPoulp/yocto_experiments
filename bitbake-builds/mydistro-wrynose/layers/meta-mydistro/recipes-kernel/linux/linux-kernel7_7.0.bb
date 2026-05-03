SUMMARY = "Linux Kernel 7.0"
SECTION = "kernel"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

inherit kernel

LINUX_VERSION = "7.0.3"
PV = "7.0.3"

# Use vanilla x86_64 defconfig as base, then merge fragments on top of it
KBUILD_DEFCONFIG = "x86_64_defconfig"
KCONFIG_MODE = "--olddefconfig"

# Fetch kernel 7.0.3 directly from kernel.org
SRC_URI = "https://cdn.kernel.org/pub/linux/kernel/v7.x/linux-7.0.3.tar.xz"
SRC_URI[sha256sum] = "0bedadbf5788693ddebbcc913c893f1a97349af79ddde7144c2a80b401959f1c"
SRC_URI += "file://qemu-builtin.cfg"

S = "${UNPACKDIR}/linux-7.0.3"

COMPATIBLE_MACHINE = "qemux86-64"

DEPENDS += "elfutils-native ncurses-native zlib-native openssl-native bison-native flex-native"

# since we use inherit kernel, and not inherit kernel-yocto,
# we created below a manual application of the fragment for the Kernel config
# This approach is low level and has advantages to debug more easily the kernel.
# kernel-yocto has nice features (automatic fragments, configcheck, SCC format), but also has drawbacks.
# In the present case, we just want the vanilla kernel with simple kernel configs and we have that.
#
# after running configure and starting the compilation,
# one can check the fragment is used in the config
# bitback linux-kernel7 -c configure && bitback linux-kernel7
# grep CONFIG_EXT4_FS   tmp/work-shared/qemux86-64/kernel-build-artifacts/.config
do_configure:append() {
    ${S}/scripts/kconfig/merge_config.sh -O ${B} ${B}/.config \
        ${UNPACKDIR}/qemu-builtin.cfg
    make -C ${S} O=${B} olddefconfig
}
