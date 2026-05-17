SUMMARY = "My C++ application"
LICENSE = "CLOSED"

#SRC_URI = "file://myapp.cpp"

#inherit cmake   # or autotools, or meson

#do_compile() {
#    ${CXX} ${CXXFLAGS} ${WORKDIR}/myapp.cpp -o myapp
#}

#do_install() {
#    install -d ${D}${bindir}
#    install -m 0755 myapp ${D}${bindir}/myapp
#}
