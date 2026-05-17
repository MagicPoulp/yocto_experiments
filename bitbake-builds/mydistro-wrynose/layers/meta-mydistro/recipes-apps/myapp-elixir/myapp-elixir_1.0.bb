#Before the Yocto build this should be run locally.
#Networking to get dependencies is not allowed for security in a Yocto build.
#mix deps.get --only prod
#tar -czvf supervision-platform-deps.tar.gz deps/

SUMMARY = "A supervision platform that restarts a process if it crashes"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=8fe15be9c1355b986d0901a0fc1bf691"

DEPENDS = "erlang elixir erlang-native elixir-native libcap"
RDEPENDS:${PN} += "erlang elixir"
WORKER_APPS = " \
myapp-cpp \
"
RDEPENDS:${PN} += "${WORKER_APPS}"

# Inherit chrpath to natively clear host RPATHs from compiled binaries safely
inherit chrpath

FILESEXTRAPATHS:prepend := "${THISDIR}/..:"
SRC_URI = " \
    file://supervision-platform-repo \
    file://supervision-platform-repo/supervision-platform-deps.tar.gz \
"

S = "${UNPACKDIR}/supervision-platform-repo"

do_configure() {
    if [ -d "${UNPACKDIR}/deps" ]; then
        cp -r ${UNPACKDIR}/deps ${S}/
    else
        bbfatal "The unpacked 'deps' folder was not found. Ensure you archived it correctly."
    fi
}

do_compile() {
    cd ${S}

    export MIX_ENV="prod"
    export HEX_OFFLINE=1
    export MIX_REBAR3="${STAGING_BINDIR_NATIVE}/rebar3"
    export ERL_EI_LIBDIR="${STAGING_LIBDIR_NATIVE}/erlang/lib/erl_interface/lib"
    export ERL_EI_INCLUDE_DIR="${STAGING_LIBDIR_NATIVE}/erlang/lib/erl_interface/include"
    export ERL_LDFLAGS="-lcap"

    bbnote "Surgically purging runtime_tools references with syntax safety guards..."

    # 1. Clean Elixir mix.exs files using precise syntax matching
    find . -type f -name "mix.exs" | while read -r file; do
        sed -E -i 's/runtime_tools\s*:\s*:[a-zA-Z0-9_]+\s*,\s*//g' "$file"
        sed -E -i 's/,\s*runtime_tools\s*:\s*:[a-zA-Z0-9_]+//g' "$file"
        sed -E -i 's/\[\s*runtime_tools\s*:\s*:[a-zA-Z0-9_]+\s*\]/\[\]/g' "$file"

        sed -E -i 's/:runtime_tools\s*,\s*//g' "$file"
        sed -E -i 's/,\s*:runtime_tools//g' "$file"
        sed -E -i 's/\[\s*:runtime_tools\s*\]/\[\]/g' "$file"
    done

    # 2. Clean Erlang app configurations safely (standard comma-separated terms)
    find . -type f \( -name "*.app" -o -name "*.app.src" \) | while read -r file; do
        sed -E -i 's/runtime_tools\s*,\s*//g' "$file"
        sed -E -i 's/,\s*runtime_tools//g' "$file"
        sed -E -i 's/\[\s*runtime_tools\s*\]/\[\]/g' "$file"
    done

    # Build steps
    mix deps.compile --no-deps-check
    mix compile --no-deps-check
    mix release --overwrite --no-deps-check
}

do_install() {
    install -d ${D}/opt/supervision_platform

    if [ -d ${S}/_build/prod/rel/ems ]; then
        cp -r ${S}/_build/prod/rel/ems/* ${D}/opt/supervision_platform/
        LN_TARGET="ems"
    else
        cp -r ${S}/_build/prod/rel/supervision_platform/* ${D}/opt/supervision_platform/
        LN_TARGET="supervision_platform"
    fi

    install -d ${D}${bindir}
    ln -rs ${D}/opt/supervision_platform/bin/${LN_TARGET} ${D}${bindir}/supervision_platform

    # Clean native fix: Clear out hostile build paths from the target directory before packaging QA runs
    chrpath --delete ${D}/opt/supervision_platform/lib/*/priv/lib/*/*.so || true
}

FILES:${PN} += " \
    /opt/supervision_platform \
    ${bindir} \
"

INSANE_SKIP:${PN} += "buildpaths"
INSANE_SKIP:${PN}-dbg += "buildpaths"
