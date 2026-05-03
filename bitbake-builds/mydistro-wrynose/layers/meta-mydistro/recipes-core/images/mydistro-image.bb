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

# the hashed password was created as follows
# openssl passwd -6 user
# then the dollar is automatically changed badly
# to correct this problem we load the real password from a text file using the custom function
#
# besides, on the target, /etc/login.defs contains this: ENCRYPT_METHOD SHA512
#
# besides, also we remove the root user

EXTRA_USERS_PARAMS = " \
    usermod -p '!' root; \
"

# the file will not be copied, it is justed tracked
SRC_URI += "file://password.txt"

# This function runs AFTER the rootfs is constructed
my_custom_password_func() {
    # Use useradd with -R (rootfs) to create the user properly
    # This updates both /etc/passwd and /etc/shadow correctly
    if ! grep -q "^user:" "${IMAGE_ROOTFS}/etc/passwd"; then
        useradd -m -s /bin/sh -R ${IMAGE_ROOTFS} user
        usermod -R ${IMAGE_ROOTFS} -p 'w6wDDbilNpHYxXfR699wGDU3ekic8q8RRIFvk5WOflQASsHyq7WqSn1PlOcB2Qs/a8ORjT.ABPEG59dn6o6pBa0i.LlwDTtHLc3JpPRFP1' user;
    fi
    SHADOW_FILE="${IMAGE_ROOTFS}/etc/shadow"
    # Get the hash from the file
    PASS_FILE="${THISDIR}/files/password.txt"
    NEW_HASH=$(cat ${PASS_FILE})
    # Use sed to replace the password field for 'user' in the actual rootfs
    sed -i "s|user:[^:]*|user:${NEW_HASH}|" ${SHADOW_FILE}
}

# Tell Yocto to run this function at the end of the image creation
ROOTFS_POSTPROCESS_COMMAND += "my_custom_password_func; "
