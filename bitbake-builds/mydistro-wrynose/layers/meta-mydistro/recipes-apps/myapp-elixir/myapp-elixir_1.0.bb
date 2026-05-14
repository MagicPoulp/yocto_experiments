SUMMARY = "My Elixir application"
LICENSE = "CLOSED"

DEPENDS = "erlang elixir"
RDEPENDS:${PN} += "erlang elixir"

SRC_URI = "file://hello.ex"

S = "${UNPACKDIR}"

do_install() {
    # Create /usr/bin on the target filesystem
    install -d ${D}${bindir}

    # Copy the file to /usr/bin/hello.ex
    install -m 0644 ${UNPACKDIR}/hello.ex ${D}${bindir}/hello.ex
}

FILES:${PN} += "${bindir}/hello.ex"

# Point to your app source
#SRC_URI = "git://github.com/yourname/myapp.git;branch=main;protocol=https"
#SRCREV = "abc123yourgitcommithash"

#S = "${UNPACKDIR}/git"

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
