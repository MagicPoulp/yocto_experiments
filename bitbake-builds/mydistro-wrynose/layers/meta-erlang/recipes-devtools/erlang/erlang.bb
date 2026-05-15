SUMMARY = "Erlang/OTP - A high-level programming language and runtime"
DESCRIPTION = "Erlang is a programming language used to build massively scalable \
soft real-time systems with requirements on high availability."
HOMEPAGE = "http://www.erlang.org"
LICENSE = "MIT"

# Note: Ensure the checksum matches the LICENSE.txt file in the source root
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=ff253ad767462c46be284da12dda33e8"

# Source URL based on documentation
SRC_URI = "https://github.com/erlang/otp/releases/download/OTP-${PV}/otp_src_${PV}.tar.gz"
SRC_URI += "file://0001-fix-autoconf-aux-dir.patch"
SRC_URI += "file://0001-fix-autoconf-version.patch"
SRC_URI += "file://0001-fix-wolfssl-autoconf-crypto-ac-valid-include.patch"
SRC_URI += "file://openssl_config.h"

# Checksum for the OTP-29.0 source archive
SRC_URI[sha256sum] = "149bb67708427ae50fce861d54ff676134e003438012efb41187d28122938564"

PV = "${ERLANG_VERSION}"

# Source directory as extracted
S = "${UNPACKDIR}/otp_src_${PV}"

# Erlang uses a custom configure script compatible with autotools
inherit autotools-brokensep pkgconfig

# Required utilities and libraries mentioned in the docs
DEPENDS = "zlib perl-native wolfssl"
DEPENDS += "erlang-native"

# Configuration options derived from the erlang documentation
# https://www.erlang.org/doc/system/install.html
# ssl was removed
EXTRA_OECONF = " \
    --enable-jit \
    --without-javac \
    --disable-kernel-poll \
    --disable-saved-compile-time \
    --enable-pie \
    --enable-builtin-ryu \
    --enable-ei-dynamic-lib \
    --without-termcap \
    --with-ssl=${STAGING_INCDIR}/.. \
    --with-ssl-incl=${STAGING_INCDIR}/wolfssl \
    --disable-static \
"

# Set ERL_TOP as required by the Erlang build system
export ERL_TOP = "${S}"

# configure erlang's build for wolfssl
# the OPENSSL_NO are obsolete cryptography algorithms
WOLFSSL_EXTRA_CFLAGS += " \
-DOPENSSL_EXTRA  \
-DWOLFSSL_OPENSSH  \
-DEXTERNAL_OPENS_H \
-DwolfSSL_OPENSSL_HEADERS \
-DWOLFSSL_OPTIONS_H \
-I${STAGING_INCDIR}/wolfssl \
-I${STAGING_INCDIR}/wolfssl/include \
-DOPENSSL_NO_RC2 \
-DOPENSSL_NO_RC4 \
-DOPENSSL_NO_RC5 \
-DOPENSSL_NO_MDC2 \
-DOPENSSL_NO_IDEA \
-DOPENSSL_NO_CAST \
-DOPENSSL_NO_BLOWFISH \
-DOPENSSL_NO_SEED \
-DWOLFSSL_VERSION=${WOLFSSL_VERSION} \
-DOPENSSL_VERSION=0 \
"

TARGET_CFLAGS += "${WOLFSSL_EXTRA_CFLAGS}"
# preprocessor flags
TARGET_CPPFLAGS += "${WOLFSSL_EXTRA_CFLAGS}"
BUILD_CFLAGS:append:class-native = " ${WOLFSSL_EXTRA_CFLAGS}"
BUILD_CPPFLAGS:append:class-native = " ${WOLFSSL_EXTRA_CFLAGS}"

#DED_CFLAGS='${CFLAGS} -DOPENSSL_VERSION=0 -DWOLFSSL_VERSION=${WOLFSSL_VERSION}'

# Ensure the linker knows to actually use the wolfssl library
TARGET_LDFLAGS += "-lwolfssl -lcrypto -lssl"
# Add this to your Erlang recipe
EXTRA_OECONF += " \
    SSL_CRYPTO_LIBNAME='wolfssl' \
    SSL_SSL_LIBNAME='wolfssl' \
    erl_cv_crypto_libname='-lwolfssl' \
    erl_cv_ssl_libname='-lwolfssl' \
    erl_cv_valid_openssl_header=yes \
"

#export CC:append = " ${WOLFSSL_EXTRA_CFLAGS}"
#export DED_BASIC_CFLAGS = " ${WOLFSSL_EXTRA_CFLAGS}"

CACHED_CONFIGUREVARS += "erl_xcomp_sysroot=${STAGING_DIR_TARGET}"

# best practice to use the yocto flags to build custom makefiles
EXTRA_OEMAKE = " \
    'CC=${CC}' \
    'LD=${LD}' \
"
# raw ld does not understand yocto flags
EXTRA_OEMAKE:class-native = " \
    'CC=${CC}' \
"

#BUILD_LDFLAGS:remove = "-Werror=undef"
#TARGET_LDFLAGS:remove = "-Werror=undef"

EXTRA_OECONF += " \
    --host=${HOST_SYS} \
    --build=${BUILD_SYS} \
"

#CLEANBROKEN = "1" # if the clean step is stuck

do_buildclean() {
    bbnote "Skipping default do_buildclean to protect workspace from erlc missing errors"
    :
}

autotools_preconfigure() {
    bbnote "Skipping autotools_preconfigure to prevent erlc missing errors during clean"
    :
}

# Erlang source has already the configure generated
# however, wolfssl has its own headers for openssl compatibility mode
# and a patch is applied on the erlang source on erts/crypto.ac
do_configure:prepend() {
    cp ${UNPACKDIR}/openssl_config.h ${S}/lib/crypto/c_src/openssl_config.h
    cd ${S}
    ./otp_build update_configure
}

# cf. "Updating configure scripts in"
# https://www.erlang.org/docs/26/installation_guide/install
do_configure() {
    # The release tarball already contains pre-generated configure scripts.
    # Per Erlang docs, otp_build configure uses these directly without
    # needing to run autoreconf.
    cd ${S}
    ./otp_build configure ${EXTRA_OECONF}
}

do_configure:append() {
    cd ${S}
    # Fix hard-coded paths in pre-generated .erl files
    find ${S} -name "*.erl" | \
        xargs -r grep -l "/daily_build/otp_src" 2>/dev/null | \
        xargs -r sed -i "s|/daily_build/otp_src|${S}|g"
    # Fix text-based dependency and makefiles
    find ${S} \( -name "*.mk" -o -name "*.dep" -o -name "Makefile" \) | \
        xargs -r grep -l "/daily_build/otp_src" 2>/dev/null | \
        xargs -r sed -i "s|/daily_build/otp_src|${S}|g"
    #./otp_build boot -a
}

do_compile:prepend() {
    # Documentation suggests setting LANG if Perl behaves strangely
    export LANG=C
}

do_install() {
    # INSTALL_PREFIX is often used by Erlang's Makefile to redirect the
    # installation into Yocto's temporary image folder
    oe_runmake DESTDIR=${D} install
}

do_install:append() {
    # Optional: Remove large test folders if not needed in the image
    rm -rf ${D}${libdir}/erlang/lib/*/test
}

BBCLASSEXTEND = "native"
