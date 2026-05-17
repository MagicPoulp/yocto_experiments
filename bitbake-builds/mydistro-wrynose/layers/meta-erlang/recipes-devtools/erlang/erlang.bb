SUMMARY = "Erlang/OTP - A high-level programming language and runtime"
DESCRIPTION = "Erlang is a programming language used to build massively scalable \
soft real-time systems with requirements on high availability."
HOMEPAGE = "http://www.erlang.org"
LICENSE = "Apache-2.0"

# Note: Ensure the checksum matches the LICENSE.txt file in the source root
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=ff253ad767462c46be284da12dda33e8"

# Source URL based on documentation
SRC_URI = "https://github.com/erlang/otp/releases/download/OTP-${PV}/otp_src_${PV}.tar.gz"
SRC_URI += "file://0001-fix-autoconf-aux-dir.patch"
SRC_URI += "file://0001-fix-autoconf-version.patch"

# Checksum for the OTP-29.0 source archive
SRC_URI[sha256sum] = "149bb67708427ae50fce861d54ff676134e003438012efb41187d28122938564"

PV = "${ERLANG_VERSION}"

# Source directory as extracted
S = "${UNPACKDIR}/otp_src_${PV}"

# Erlang uses a custom configure script compatible with autotools
inherit autotools-brokensep pkgconfig

BBCLASSEXTEND = "native"

# Required utilities and libraries mentioned in the docs
DEPENDS = "zlib perl-native openssl"
DEPENDS:append:class-target = " erlang-native"

# Configuration options derived from the erlang documentation
# https://www.erlang.org/doc/system/install.html
SSL_SYSROOT_PATH = "${STAGING_DIR_HOST}${prefix}"
SSL_SYSROOT_PATH:class-native = "${STAGING_DIR_NATIVE}${prefix_native}"
EXTRA_OECONF = " \
--enable-jit \
--without-javac \
--without-wx \
--disable-kernel-poll \
--disable-saved-compile-time \
--enable-pie \
--enable-builtin-ryu \
--enable-ei-dynamic-lib \
--without-termcap \
--with-ssl=${SSL_SYSROOT_PATH} \
"
# --disable-static is standard practice in production (reduce RAM, or for auditing dependencies)
EXTRA_OECONF:append = " \
--disable-static \
"
# Force Yocto to pass the path of the native erlc tool into the target configuration
EXTRA_OECONF:append:class-target = " \
--with-erlc=${STAGING_BINDIR_NATIVE}/erlc \
"

# Set ERL_TOP as required by the Erlang build system
#export ERL_TOP = "${S}"

# configure erlang's build
# the OPENSSL_NO are obsolete cryptography algorithms
ERLANG_OPENSSL_EXTRA_CFLAGS += " \
-DOPENSSL_NO_RC2 \
-DOPENSSL_NO_RC4 \
-DOPENSSL_NO_RC5 \
-DOPENSSL_NO_MDC2 \
-DOPENSSL_NO_IDEA \
-DOPENSSL_NO_CAST \
-DOPENSSL_NO_BLOWFISH \
-DOPENSSL_NO_SEED \
-DOPENSSL_VERSION=0 \
"

EXTRA_CONFIGURE_FLAGS = "\
with_termcap=no \
"

# DOPENSSL_VERSION=0 corresponds to version 3 in an enum

TARGET_CFLAGS += "${OPENSSL_EXTRA_CFLAGS}"
# preprocessor flags
TARGET_CPPFLAGS += "${ERLANG_OPENSSL_EXTRA_CFLAGS}"
BUILD_CFLAGS:append:class-native = " ${ERLANG_OPENSSL_EXTRA_CFLAGS}"
BUILD_CPPFLAGS:append:class-native = " ${ERLANG_OPENSSL_EXTRA_CFLAGS}"

# Ensure the linker knows the libraries
TARGET_LDFLAGS:append:class-target = " -lcrypto -lssl"

CACHED_CONFIGUREVARS:append:class-target = " erl_xcomp_sysroot=${STAGING_DIR_TARGET}"
CACHED_CONFIGUREVARS:append:class-target = " lt_cv_sys_lib_dlsearch_path_spec='${STAGING_DIR_TARGET}/usr/lib'"
CACHED_CONFIGUREVARS:append:class-target = " oldincludedir='${STAGING_DIR_TARGET}/usr/include'"
CACHED_CONFIGUREVARS:append:class-target = " erl_xcomp_CC_FOR_BUILD='${BUILD_CC}'"
CACHED_CONFIGUREVARS:append:class-target = " erl_xcomp_CFLAGS_FOR_BUILD='${BUILD_CFLAGS}'"
CACHED_CONFIGUREVARS:append:class-target = " ERLC=${STAGING_BINDIR_NATIVE}/erlc"

# needed to have erl, erlc in the PATH in subshells
PATH:prepend:class-target = "${STAGING_BINDIR_NATIVE}:"

# best practice to use the yocto flags to build custom makefiles
EXTRA_OEMAKE:class-target = " \
'CC=${CC}' \
'LD=${LD}' \
'CC_FOR_BUILD=${BUILD_CC}' \
'CFLAGS_FOR_BUILD=${BUILD_CFLAGS}' \
'ERLC=${STAGING_BINDIR_NATIVE}/erlc' \
'YCF_EXECUTABLE_PATH=${S}/erts/lib_src/yielding_c_fun/bin/x86_64-pc-linux-gnu/yielding_c_fun' \
"
# raw ld does not understand yocto flags
EXTRA_OEMAKE:class-native = " \
'CC=${CC}' \
"

EXTRA_OECONF += " \
--host=${HOST_SYS} \
--build=${BUILD_SYS} \
"

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
    cd ${S}
    # Fix hard-coded paths in pre-generated .erl files
    find ${S} -name "*.erl" | \
        xargs -r grep -l "/daily_build/otp_src" 2>/dev/null | \
        xargs -r sed -i "s|/daily_build/otp_src|${S}|g"
    # Fix text-based dependency and makefiles
    find ${S} \( -name "*.mk" -o -name "*.dep" -o -name "Makefile" \) | \
        xargs -r grep -l "/daily_build/otp_src" 2>/dev/null | \
        xargs -r sed -i "s|/daily_build/otp_src|${S}|g"

    export ERLC="${STAGING_BINDIR_NATIVE}/erlc"
    export PATH="${STAGING_DIR_NATIVE}/usr/lib/erlang/bin:${STAGING_BINDIR_NATIVE}:${PATH}"
}

# Erlang source has already the configure generated
# however, we may want to regenerate them
# cf. "Updating configure scripts in"
# https://www.erlang.org/docs/26/installation_guide/install
do_configure() {
    cd ${S}
    # Export xcomp vars so all sub-configures inherit them
    # Only needed for cross-compilation, not native
    if [ "${BUILD_SYS}" != "${HOST_SYS}" ]; then
        SYSROOT="${STAGING_DIR_TARGET}"
        export erl_xcomp_sysroot="$SYSROOT"
        export erl_xcomp_isysroot="$SYSROOT"
        export lt_cv_sys_lib_dlsearch_path_spec="$SYSROOT/usr/lib"
        export PKG_CONFIG_SYSROOT_DIR="$SYSROOT"
        export PKG_CONFIG_PATH="$SYSROOT/usr/lib/pkgconfig:$SYSROOT/usr/share/pkgconfig"
        export PKG_CONFIG_LIBDIR="$SYSROOT/usr/lib/pkgconfig"
        export CC_FOR_BUILD="${BUILD_CC}"
        export CFLAGS_FOR_BUILD="${BUILD_CFLAGS}"
        export erl_xcomp_CC_FOR_BUILD="${BUILD_CC}"
        export erl_xcomp_CFLAGS_FOR_BUILD="${BUILD_CFLAGS}"
    fi

    # Export the compiler environment explicitly
    export CC="${CC}"
    export CXX="${CXX}"
    export LD="${LD}"
    export CFLAGS="${CFLAGS}"
    export CXXFLAGS="${CXXFLAGS} -std=c++23"
    export CPPFLAGS="${CPPFLAGS}"
    export LDFLAGS="${LDFLAGS}"

    # Strip debug remapping flags — they contain /usr/src/debug which
    # triggers configure-unsafe QA false positives. Configure doesn't
    # need them; they only matter during actual compilation.
    export CFLAGS="$(echo "${CFLAGS}" | sed 's:-ffile-prefix-map=[^ ]*=/usr/src/debug[^ ]*::g')"
    export CXXFLAGS="$(echo "${CXXFLAGS}" | sed 's:-ffile-prefix-map=[^ ]*=/usr/src/debug[^ ]*::g')"

    ./otp_build update_configure
    ./otp_build configure \
        --host=${HOST_SYS} \
        --build=${BUILD_SYS} \
        --prefix=${prefix} \
        --oldincludedir=${STAGING_DIR_HOST}/usr/include \
        ${EXTRA_OECONF}
}


do_configure:append:class-target() {
    find ${S} -type d -name "*-oe-linux*" | while read dir; do
        base=$(dirname "$dir")
        oetuple=$(basename "$dir")
        buildtuple=$(echo "$oetuple" | sed 's/-oe-linux.*/-pc-linux-gnu/')
        if [ ! -e "${base}/${buildtuple}" ]; then
            ln -s "${dir}" "${base}/${buildtuple}"
        fi
    done

    # Patch hardcoded "ERLC = erlc" — env vars can't override make variable assignments
    find ${S}/make -name "otp.mk" | while read f; do
        sed -i "s|^ERLC = erlc |ERLC = ${STAGING_BINDIR_NATIVE}/erlc |g" "$f"
    done

    # configure generates directories named after the OE host tuple
    # but otp_build boot expects the canonical build tuple (x86_64-pc-linux-gnu).
    # Symlink every occurrence so the build system can find them.
    find ${S} -type d -name "*-oe-linux*" | while read dir; do
        base=$(dirname "$dir")
        oetuple=$(basename "$dir")
        buildtuple=$(echo "$oetuple" | sed 's/-oe-linux.*/-pc-linux-gnu/')
        if [ ! -e "${base}/${buildtuple}" ]; then
            ln -s "${dir}" "${base}/${buildtuple}"
            bbnote "Symlinked ${oetuple} -> ${buildtuple} in ${base}"
        fi
    done
}

do_compile:prepend:class-target() {
    # Fix the JIT header path mismatch where target expects x86_64-oe-linux-gnu
    # but host YCF tool generates under x86_64-pc-linux-gnu
    # Create the JIT target directory ahead of time and link it 
    # so YCF writes to both tuples seamlessly
    mkdir -p ${S}/erts/emulator/x86_64-pc-linux-gnu
    ln -snf x86_64-pc-linux-gnu ${S}/erts/emulator/x86_64-oe-linux-gnu

    # Fix deprecation warning that causes cross_check_erl to fail
    # here is how a mini SDK can test this fix.
    # cross_check_erl.erl needs to be created manually from the EOF in make/cross_check_erl
    #
    # cd ~/Documents/yocto/bitbake-builds/mydistro-wrynose/build/tmp/work/x86-64-v3-oe-linux/erlang/29.0/sources/otp_src_29.0
    # export PATH="$(realpath ../../recipe-sysroot-native/usr/bin):$PATH"
    # erlc cross_check_erl.erl && used_otp=`erl -noshell -noinput -boot start_clean -pa . -run cross_check_erl`
    # echo $?
    sed -i 's/-module(cross_check_erl)./-module(cross_check_erl).\n-compile([nowarn_deprecated_catch])./' \
        ${S}/make/cross_check_erl
    #sed -i "s|^ERL=erl$|ERL=${STAGING_BINDIR_NATIVE}/erl|g" ${S}/make/cross_check_erl
    #sed -i "s|^ERLC=erlc$|ERLC=${STAGING_BINDIR_NATIVE}/erlc|g" ${S}/make/cross_check_erl

    # patching Makefile for cross-compilation on target cross_check_erl:
    sed -i "s|^BOOT_PREFIX\s*=.*|BOOT_PREFIX = ${STAGING_BINDIR_NATIVE}:|g" ${S}/Makefile
    bbnote "BOOT_PREFIX set to: $(grep '^BOOT_PREFIX' ${S}/Makefile)"

    bbnote "Patching yielding_c_fun main_target.mk to force host compilation..."

    # Use actual fallback path evaluation if devtool/externalsrc is playing with ${S}
    TARGET_MK="${S}/erts/lib_src/yielding_c_fun/main_target.mk"

    # We use vertical pipes | as delimiters here so slashes inside BUILD_CFLAGS do not break sed
    sed -i 's|\$(V_CC)|${BUILD_CC}|g' $TARGET_MK
    sed -i 's|\$(V_LD)|${BUILD_CC}|g' $TARGET_MK
    sed -i 's|\$(YCF_CFLAGS)|${BUILD_CFLAGS}|g' $TARGET_MK
    sed -i 's|\$(YCF_LDFLAGS)|${BUILD_LDFLAGS}|g' $TARGET_MK

    # Re-patch ERLC here because otp_build boot -s regenerates otp.mk,
    # overwriting the configure-time patch
    find ${S} -name "otp.mk" | while read f; do
        sed -i "s|^ERLC = erlc |ERLC = ${STAGING_BINDIR_NATIVE}/erlc |g" "$f"
        bbnote "compile-prepend patched ERLC in $f: $(grep '^ERLC ' $f)"
    done

    # copy erlexec from erlang-native
    ERLEXEC_NATIVE=$(find ${STAGING_DIR_NATIVE} -name "erlexec" | head -1)
    if [ -z "$ERLEXEC_NATIVE" ]; then
        bbfatal "Native erlexec not found in ${STAGING_DIR_NATIVE}"
    fi
    mkdir -p ${S}/bin/x86_64-pc-linux-gnu
    install -m 0755 "$ERLEXEC_NATIVE" ${S}/bin/x86_64-pc-linux-gnu/erlexec
    bbnote "Installed native erlexec from $ERLEXEC_NATIVE"

    # Create find_cross_ycf — doesn't exist, make runs from erts/emulator/
    # so utils/ resolves to erts/emulator/utils/
    YCF_BIN="${S}/erts/lib_src/yielding_c_fun/bin/x86_64-pc-linux-gnu/yielding_c_fun"
    mkdir -p ${S}/erts/emulator/utils
    printf '#!/bin/sh\necho "%s"\n' "$YCF_BIN" > ${S}/erts/emulator/utils/find_cross_ycf
    chmod +x ${S}/erts/emulator/utils/find_cross_ycf
    bbnote "Created find_cross_ycf returning: $YCF_BIN"

    # Also patch the already-generated Makefile since backtick runs at parse time
    sed -i "s|YCF_EXECUTABLE_PATH=\`utils/find_cross_ycf\`|YCF_EXECUTABLE_PATH=\"$YCF_BIN\"|g" \
        ${S}/erts/emulator/x86_64-oe-linux-gnu/Makefile
    bbnote "Patched YCF_EXECUTABLE_PATH in generated Makefile"
}

do_compile() {
    cd ${S}
    # the documentation suggests setting LANG if Perl behaves strangely
    export LANG=C
    # Native erlang tools are needed to bootstrap the cross build
    export PATH="${STAGING_DIR_NATIVE}/usr/lib/erlang/bin:${STAGING_BINDIR_NATIVE}:${PATH}"
    # Explicitly export ERLC so Erlang's recursive Makefiles pick it up
    # via $(ERLC) without relying solely on PATH resolution.
    export ERLC="${STAGING_BINDIR_NATIVE}/erlc"
    export YCF_EXECUTABLE_PATH="${S}/erts/lib_src/yielding_c_fun/bin/x86_64-pc-linux-gnu/yielding_c_fun"
    bbnote "ERLC is set to: ${ERLC}"
    export CC_FOR_BUILD="${BUILD_CC}"
    export CFLAGS_FOR_BUILD="${BUILD_CFLAGS}"
    # we build the profile OTP_SMALL_BUILD, the default recommended choice
    ./otp_build boot -a
}

do_install() {
    cd ${S}
    # We pass ${D} as the first argument ($1), which becomes RELEASE_ROOT.
    ./otp_build release -a ${D}
}

do_install:append:class-native() {
    install -d ${D}${bindir}

    # Binaries: live under erts-VERSION/bin/ in the image
    for tool in erlc erlexec escript; do
        src=$(find ${D} -path "*/erts-*/bin/${tool}" | head -1)
        if [ -n "$src" ] && [ -f "$src" ]; then
            install -m 0755 "$src" ${D}${bindir}/${tool}
            bbnote "Staged native ${tool} from $src"
        else
            bbwarn "Native ${tool} not found under ${D}"
        fi
    done

    # erl shell wrapper
    if [ -f ${S}/bin/erl ]; then
        install -m 0755 ${S}/bin/erl ${D}${bindir}/erl
        bbnote "Staged native erl from ${S}/bin/erl"
    else
        bbwarn "Native erl not found at ${S}/bin/erl"
    fi

    # install the full Erlang lib tree so NIFs (crypto.so etc.) are available ──
    install -d ${D}${libdir}/erlang

    # Copy the release lib tree produced by otp_build release
    if [ -d ${S}/release ]; then
        cp -a ${S}/release/lib   ${D}${libdir}/erlang/
        cp -a ${S}/release/erts* ${D}${libdir}/erlang/ || true
    fi

    # Also set ERL_ROOT so erl/erlc find libs without ERL_LIBS being set manually
    # Generate a small env wrapper consumed by the native erl script
    ERTS_VSN=$(ls ${D}${libdir}/erlang/ | grep erts | head -1)
    bbnote "Native ERTS version: ${ERTS_VSN}"

    # Force copy the static files so host mix builds can find them
    # inside erlang-native sysroot components later
    install -d ${D}${libdir}/erlang/lib/erl_interface/lib/

    if [ -f ${S}/lib/erl_interface/obj/x86_64-pc-linux-gnu/libei.a ]; then
        cp ${S}/lib/erl_interface/obj/x86_64-pc-linux-gnu/libei.a ${D}${libdir}/erlang/lib/erl_interface/lib/
        cp ${S}/lib/erl_interface/obj/x86_64-pc-linux-gnu/libei_st.a ${D}${libdir}/erlang/lib/erl_interface/lib/
    fi

    # Install libei dynamic libraries to ${libdir} so -lei resolves correctly
    # when mix/rebar3 compiles NIFs against the native erlang
    for sofile in $(find ${D}${libdir}/erlang -name "libei.so*" -o -name "libei_st.so*" 2>/dev/null); do
        install -m 0755 "$sofile" ${D}${libdir}/
    done

    # Fallback: also search the release tree directly
    for sofile in $(find ${S}/release -name "libei.so*" -o -name "libei_st.so*" 2>/dev/null); do
        install -m 0755 "$sofile" ${D}${libdir}/
    done

    # Install static libei to ${libdir} so host `mix test` / rebar resolves -lei
    # (mirrors the .so install loop above it)
    for afile in $(find ${D}${libdir}/erlang -name "libei.a" -o -name "libei_st.a" 2>/dev/null); do
        install -m 0644 "$afile" ${D}${libdir}/
        bbnote "Staged native static lib $(basename $afile) from $afile"
    done

    # OTP 29 puts libei under an arch-tuple subdir but code:lib_dir(erl_interface, lib)
    # returns the parent dir — symlink up so -L resolves without arch subdir
    EI_LIB="${S}/lib/erl_interface/lib"
    for arch_dir in ${EI_LIB}/*/; do
        for f in "${arch_dir}"libei*; do
            [ -f "$f" ] || continue
            ln -sf "$(basename ${arch_dir})/$(basename $f)" "${EI_LIB}/$(basename $f)"
            bbnote "Symlinked $(basename $f) into ${EI_LIB}/"
        done
    done
}

do_install:append() {
    # Remove test folders
    rm -rf ${D}${libdir}/erlang/lib/*/test

    # Remove src, doc, man — not needed on embedded target
    # This avoids having to enumerate every src subdirectory in FILES
    find ${D} -name "src" -type d | while read d; do
        rm -rf "$d"
    done
    find ${D} -name "doc" -type d | while read d; do
        rm -rf "$d"
    done
    find ${D} -name "man" -type d | while read d; do
        rm -rf "$d"
    done
    # Remove include dirs from lib (keep erts includes for dev package)
    #find ${D}/lib -name "include" -type d | while read d; do
    #    rm -rf "$d"
    #done

    find ${D} -name "src" -type d -exec rm -rf {} + 2>/dev/null || true
    find ${D} -name "doc" -type d -exec rm -rf {} + 2>/dev/null || true
    find ${D} -name "man" -type d -exec rm -rf {} + 2>/dev/null || true
    rm -rf ${D}/usr/include 2>/dev/null || true
    #find ${D}/lib -name "include" -type d -exec rm -rf {} + 2>/dev/null || true

    # yielding_c_fun is a host build tool — wrong RPATH, must not ship to target
    find ${D} -name "yielding_c_fun" -delete
}

do_install:append:class-target() {
    install -d ${D}${bindir}
    install -d ${D}/releases/29
    install -d ${D}/releases/29/bin
    install -d ${D}/releases/29/lib

    # Move the erts directory into your defined FINAL_ROOTDIR
    if [ -d ${D}/erts-17.0 ]; then
        mv ${D}/erts-17.0 ${D}/releases/29/
    fi

    # Move the standard libraries into your defined FINAL_ROOTDIR
    if [ -d ${D}/lib ]; then
        # Move all contents of /lib into /releases/29/lib
        mv ${D}/lib/* ${D}/releases/29/lib/
        rm -rf ${D}/lib
    fi

    # Erlang looks for /releases/29/bin/start.boot
    if [ -d ${D}/releases/29 ]; then
        find ${D}/releases/29 -maxdepth 1 -type f \( -name "*.boot" -o -name "*.script" \) -exec mv {} ${D}/releases/29/bin/ \;
    fi

    # Generate erl from erl.src by substituting the rootdir
    erl_src=$(find ${D} -name "erl.src" | head -1)
    if [ -n "$erl_src" ]; then
        sed "s|%FINAL_ROOTDIR%|/releases/29|g" "$erl_src" > ${D}${bindir}/erl
        chmod 0755 ${D}${bindir}/erl
        bbnote "Generated erl from $erl_src"
    else
        bbwarn "erl.src not found under ${D}, cannot generate erl"
    fi

    # erlang is just an alias for erl
    ln -sf erl ${D}${bindir}/erlang

    # erlc, escript
    for tool in erlc escript; do
        src=$(find ${D} -name "${tool}" ! -path "*/src/*" | head -1)
        if [ -n "$src" ] && [ -f "$src" ]; then
            install -m 0755 "$src" ${D}${bindir}/${tool}
        fi
    done

    if [ -f ${D}/releases/29/erts-17.0/bin/erlexec ]; then
        ln -sf /releases/29/erts-17.0/bin/erlexec ${D}${bindir}/erlexec
    fi

    # libei dynamic libs are only needed for native (NIF compilation),
    # not on the production target — remove them to avoid QA errors
    rm -f ${D}${libdir}/libei.so* ${D}${libdir}/libei_st.so*
}

FILES:${PN} = " \
    ${bindir}/erl \
    ${bindir}/erlang \
    ${bindir}/erlc \
    ${bindir}/escript \
    ${bindir}/erlexec \
    /releases/29 \
    /releases/RELEASES.src \
    /releases/29/lib/*/ebin \
    /releases/29/lib/*/priv \
    /releases/29/lib/common_test-*/proper_ext \
    /releases/29/lib/tools-*/emacs \
    /Install \
    /misc \
"

FILES:${PN}-dev = " \
    /releases/29/erts-17.0/include \
    ${includedir} \
"

FILES:${PN}-staticdev = " \
    ${libdir}/libei.a \
    ${libdir}/libei_st.a \
    ${libdir}/erlang/lib/erl_interface-*/lib/libei.a \
    ${libdir}/erlang/lib/erl_interface-*/lib/libei_st.a \
    /releases/29/lib/erl_interface-*/lib/libei.a \
    /releases/29/lib/erl_interface-*/lib/libei_st.a \
    /releases/29/erts-17.0/lib/internal/*.a \
"

# Skip QA checks that are false positives or inherent to Erlang's install layout
#
# --> buildpaths: compiled beam files embed source paths by design for error reporting
#
# --> usrmerge: otp_build release uses a flat layout not under /usr by design
# otp_build release produces a self-contained OTP release tree rooted at whatever you pass it
#
# --> libdir: Erlang priv/lib .so files are loaded via Erlang, not ldconfig
# Erlang NIFs (.so files under priv/) are loaded via erlang:load_nif/2 at runtime,
# bypassing ldconfig entirely — so the libdir check firing on those is genuinely a false positive
# However, if you have actual shared libraries (like libei.so) that should be in a standard libdir,
# the skip would mask a real problem
# Your recipe installs libei.so files and then removes them for the target with
# rm -f ${D}${libdir}/libei.so* — so skipping libdir on the target package is fine in practice
INSANE_SKIP:${PN} += "buildpaths usrmerge libdir"
INSANE_SKIP:${PN}-dbg += "buildpaths usrmerge libdir"
