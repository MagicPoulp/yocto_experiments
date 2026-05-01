SUMMARY = "My minimal custom distro image"

inherit core-image

# Start from scratch — only what you explicitly add
IMAGE_INSTALL = " \
    packagegroup-core-boot \
    busybox \
"

# Lock down the image
IMAGE_FEATURES  = "read-only-rootfs"
IMAGE_FEATURES:remove = " \
    debug-tweaks \
    tools-debug \
    tools-sdk \
    dev-pkgs \
    dbg-pkgs \
"

inherit extrausers

# from openssl passwd -6 user
# and remove root

EXTRA_USERS_PARAMS = " \
    useradd -m -s /bin/sh user; \
    usermod -p '$6$wp6fDyisBdwbhW3F$pKgcEfsgOcfLiQm6BPFenVz77xQ4s/UHv5uymX4WMVp6nOrrJKGEb2PO/Fcgbks8gTxitbfsoWrQ0tnJD6Dh//' user; \
    usermod -p '!' root; \
"
