SUMMARY = "A supervision platform that restarts a process if it crashes"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=8fe15be9c1355b986d0901a0fc1bf691"

DEPENDS = "erlang elixir"
RDEPENDS:${PN} += "erlang elixir"
# nodejs is just to run a test server
RDEPENDS:${PN} += "nodejs"

# using a local folder for testing purpose, but we could user a real repo
FILESEXTRAPATHS:prepend := "${THISDIR}/..:"
SRC_URI = "file://supervision-platform-repo"

S = "${UNPACKDIR}/supervision-platform-repo"

do_install() {
    # Create the parent directory structure using install
    install -d ${D}/opt/supervision-platform

    # Switch to the source directory to keep paths relative
    cd ${UNPACKDIR}/supervision-platform-repo

    # Mirror the directory structure first (Directories must be 0755 to be accessible)
    find . -type d -exec install -d -m 0755 {} ${D}/opt/supervision-platform/{} \;

    # Securely install regular files with 0644 (Read-only for general system/users)
    # This covers your .beam files and config files safely.
    find . -type f ! -path "./bin/*" -exec install -m 0644 {} ${D}/opt/supervision-platform/{} \;

    # Securely install ONLY the execution scripts with 0755
    # This ensures your Elixir boot runners can actually start.
    if [ -d ./bin ]; then
        find ./bin -type f -exec install -m 0755 {} ${D}/opt/supervision-platform/bin/{} \;
    fi

    # Expose the app to the system via /usr/bin
    install -d ${D}${bindir}
    #ln -rs ${D}/opt/supervision-platform/bin/supervision ${D}${bindir}/supervision
}

FILES:${PN} += " \
    /opt/supervision-platform \
    ${bindir} \
"

INSANE_SKIP:${PN} += "buildpaths"

#inherit mix   # provided by meta-erlang

# Hex dependencies must be pre-fetched
# Run this locally first to generate the lockfile:
# mix deps.get && mix deps.compile
#MIX_ENV = "prod"

#do_compile() {
#    cd ${S}
#    mix local.hex --force
#    mix local.rebar --force
#    mix deps.get --only prod
#    mix release --overwrite
#}

#do_install() {
#    install -d ${D}${bindir}
#    install -d ${D}/opt/myapp

# Install the release
#    cp -r ${S}/_build/prod/rel/myapp/* ${D}/opt/myapp/

#    # Symlink the start script to PATH
#    ln -sf /opt/myapp/bin/myapp ${D}${bindir}/myapp
#}

#FILES:${PN} += "/opt/myapp"

#do_compile() {
# Establish the build environment variables so Hex/Mix don't reach out to the web
#    export MIX_ENV="prod"
#    export HEX_OFFLINE=1

# Explicitly point Mix to the Yocto cross-compiler Erlang Interface paths
#   export ERL_EI_LIBDIR="${STAGING_LIBDIR_NATIVE}/erlang/lib/erl_interface/lib"
#   export ERL_EI_INCLUDE_DIR="${STAGING_LIBDIR_NATIVE}/erlang/lib/erl_interface/include"

# Compile the pre-fetched dependencies and the app without fetching
#   mix deps.compile --no-deps-check
#   mix compile --no-deps-check
#}
