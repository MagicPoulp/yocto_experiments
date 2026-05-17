SUMMARY = "Worker C++ application that crashes intentionally"
LICENSE = "CLOSED"

SRC_URI = "file://crashing_worker.cpp"

S = "${UNPACKDIR}"

do_compile() {
    ${CXX} ${CXXFLAGS} ${LDFLAGS} ${S}/crashing_worker.cpp -o crashing_worker
}

do_install() {
    # 1. Create the destination directory inside the image destination (${D})
    install -d ${D}/opt/supervision_platform

    # 2. Install the compiled binary to that directory
    install -m 0755 crashing_worker ${D}/opt/supervision_platform/
}

FILES:${PN} += "/opt/supervision_platform/crashing_worker"
