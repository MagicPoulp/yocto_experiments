WOLFSSL_VERSION = "${PV}"

# Ensure wolfssl builds for the host (native) and the SDK environment (nativesdk)
BBCLASSEXTEND:append = " native nativesdk"

# Force the target recipe to provide standard openssl names
PROVIDES += "openssl virtual/openssl virtual/crypt"

# Force the host/native version to explicitly provide openssl-native names
PROVIDES:class-native += "openssl-native virtual/openssl-native virtual/crypt-native"
PROVIDES:class-nativesdk += "openssl-nativesdk virtual/openssl-nativesdk virtual/crypt-nativesdk"

# Map the runtime packaging requirements
RPROVIDES:${PN} += "openssl"
RPROVIDES:${PN}-dev += "openssl-dev"
RPROVIDES:${PN}-staticdev += "openssl-staticdev"
RPROVIDES:${PN}-native += "openssl-native"
RPROVIDES:${PN}-nativesdk += "openssl-nativesdk"

RPROVIDES:${PN} += "openssl openssl-bin"
RPROVIDES:${PN}-dev += "openssl-dev"
RPROVIDES:${PN}-staticdev += "openssl-staticdev"

# Maps host-side utilities
RPROVIDES:${PN}-native += "openssl-native openssl-bin-native"

# Maps cross-compilation SDK utility environments (fixes your current error!)
RPROVIDES:${PN}-nativesdk += "openssl-nativesdk nativesdk-openssl-bin nativesdk-openssl-conf"

# needed by erlang
EXTRA_OECONF += " \
--enable-opensslall \
--enable-opensslextra \
--enable-aesofb \
--enable-aescfb \
--enable-aesgcm \
--enable-aesccm \
"

# Create the symlinks Erlang is looking for
do_install:append() {
    # Fix the header path (from our previous step)
    ln -snf . ${D}${includedir}/wolfssl/include

    # Fixes "#include <wolfssl/...>" lookups failing
    # This creates a symlink named 'wolfssl' pointing back to its own parent directory (.)
    ln -snf . ${D}${includedir}/wolfssl/wolfssl

    # Fix the library names or erlang-native erts/crypto.ac does not find the libs
    # Erlang looks for libcrypto.so and libssl.so
    ln -sf libwolfssl.so ${D}${libdir}/libcrypto.so
    ln -sf libwolfssl.so ${D}${libdir}/libssl.so

    # the static ones are faster
    ln -sf libwolfssl.a ${D}${libdir}/libcrypto.a
    ln -sf libwolfssl.a ${D}${libdir}/libssl.a
}

# Ensure Yocto packages these symlinks in the -dev package
FILES:${PN}-dev += "${includedir}/wolfssl/include"
