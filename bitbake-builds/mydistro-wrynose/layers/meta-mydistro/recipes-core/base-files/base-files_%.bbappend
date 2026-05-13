FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " file://sane-path.sh"

do_install:append() {
    install -d ${D}${sysconfdir}/profile.d
    install -m 0755 ${UNPACKDIR}/sane-path.sh \
        ${D}${sysconfdir}/profile.d/sane-path.sh
    echo 'export LANG=en_US.UTF-8' >> ${D}${sysconfdir}/profile
    echo 'export LC_ALL=en_US.UTF-8' >> ${D}${sysconfdir}/profile
}
