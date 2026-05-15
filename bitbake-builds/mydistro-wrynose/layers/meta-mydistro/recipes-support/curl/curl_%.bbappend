# Strip openssl out of the custom defaults dynamically
COMMON_PACKAGECONFIG := "${@' '.join([x for x in d.getVar('COMMON_PACKAGECONFIG').split() if x != 'openssl'])}"

# Define the wolfssl package config block
PACKAGECONFIG[wolfssl] = "--with-wolfssl=${STAGING_DIR_TARGET}${prefix},--without-wolfssl,wolfssl"

# Apply wolfssl and fixes STRICTLY to the target device build
PACKAGECONFIG:append:class-target = " wolfssl"
CPPFLAGS:append:class-target = " ${@bb.utils.contains('PACKAGECONFIG', 'wolfssl', '-DUSE_WOLFSSL', '', d)}"
EXTRA_OECONF:append:class-target = " --without-libpsl"

# configure for wolfssl
PACKAGECONFIG[wolfssl-native] = "--with-wolfssl=${STAGING_DIR_NATIVE}${prefix},--without-wolfssl,wolfssl-native"
PACKAGECONFIG:append:class-native = " wolfssl-native"
EXTRA_OECONF:append:class-native = " --without-libpsl"

# FORCE REMOVE --without-ssl right before configure executes.
python () {
    oe_conf = d.getVar('EXTRA_OECONF') or ""
    if '--without-ssl' in oe_conf:
        # Split into words, filter out the flag, and reconstruct the string
        conf_list = oe_conf.split()
        conf_list = [item for item in conf_list if item != '--without-ssl']
        d.setVar('EXTRA_OECONF', ' '.join(conf_list))
}

# add flags
EXTRA_OECONF:append:class-target = " WOLFSSL_ENABLED=1 SSL_DISABLED="

