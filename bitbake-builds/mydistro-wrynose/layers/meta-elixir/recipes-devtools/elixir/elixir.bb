SUMMARY = "Elixir a wrapper language over erlang"
DESCRIPTION = "Elixir a wrapper language over erlang"
HOMEPAGE = "https://github.com/elixir-lang/elixir"
LICENSE = "Apache-2.0"
# https://github.com/elixir-lang/elixir/blob/main/LICENSE

# Note: Ensure the checksum matches the LICENSE file in the source root
LIC_FILES_CHKSUM = "file://LICENSE;md5=e23fadd6ceef8c618fc1c65191d846fa"

# Source URL based on documentation
SRC_URI += "git://github.com/elixir-lang/elixir;tag=v${PV};protocol=https;nobranch=1"
# Checksum for the Elixir source archive
SRC_URI[sha256sum] = "10750b8bd74b10ac1e25afab6df03e3d86999890fa359b5f02aa81de18a78e36"

PV = "${ELIXIR_VERSION}"

BBCLASSEXTEND = "native"

# Elixir is built on top of Erlang
DEPENDS = "erlang erlang-native"
# erlang must be present at runtime — elixir shells out to erl
RDEPENDS:${PN} = "erlang"

# Elixir's Makefile does not support parallel builds reliably
PARALLEL_MAKE = ""

# The source extracts into a subdirectory named elixir-<version>
S = "${UNPACKDIR}/elixir-${PV}"

do_compile() {
    cd ${S}
    make clean compile
}

do_install() {
    install -d ${D}${libdir}/elixir
    install -d ${D}${bindir}

    # Install compiled beam files and standard library
    cp -r ${S}/lib ${D}${libdir}/elixir/
    cp -r ${S}/bin ${D}${libdir}/elixir/

    # Symlink executables into bindir using a RELATIVE path
    # From /usr/bin → ../lib/elixir/bin/<bin>
    for bin in elixir elixirc iex mix; do
        if [ -f ${D}${libdir}/elixir/bin/${bin} ]; then
            ln -sf ../lib/elixir/bin/${bin} ${D}${bindir}/${bin}
        fi
    done

    rm -rf ${D}${libdir}/elixir/lib/elixir/scripts/windows_installer
}

do_install:append:class-target() {
    # Strip libraries that are build/dev tools only,
    # and that embed TMPDIR paths in all their beam files.
    # ex_unit: test framework
    # iex:     interactive shell
    # mix:     build tool
    rm -rf ${D}${libdir}/elixir/lib/ex_unit
    rm -rf ${D}${libdir}/elixir/lib/iex
    rm -rf ${D}${libdir}/elixir/lib/mix
}

FILES:${PN} = " \
    ${bindir}/elixir \
    ${bindir}/elixirc \
    ${bindir}/iex \
    ${bindir}/mix \
    ${libdir}/elixir \
"

# Skip QA checks that are false positives or inherent to Erlang's install layout
# buildpaths: compiled beam files embed source paths by design for error reporting
INSANE_SKIP:${PN} += "buildpaths"
INSANE_SKIP:${PN}-dbg += "buildpaths"
