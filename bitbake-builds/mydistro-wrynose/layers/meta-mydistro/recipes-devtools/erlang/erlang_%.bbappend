do_install:append() {
    rm -rf ${D}${libdir}/erlang/lib/odbc-*
}
