
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:class-native = " file://0001-remove-openssl.patch"
SRC_URI:append:class-nativesdk = " file://0001-remove-openssl.patch"

# Force CMake's internal curl utility (cmcurl) to disable OpenSSL
CMAKE_EXTRACONF:append:class-native = " -DCMAKE_USE_OPENSSL=OFF -DHTTP_ONLY=ON -D_CMAKE_USE_OPENSSL_DEFAULT=OFF"

# Disable it globally across all setup scopes
EXTRA_OECMAKE:append:class-native = " -DCMAKE_USE_OPENSSL=OFF"
BOOTSTRAP_ARGS:append:class-native = " -- -DCMAKE_USE_OPENSSL=OFF"

# Drop OpenSSL from package configs entirely
#PACKAGECONFIG:class-native = ""
PACKAGECONFIG:remove:class-native = "openssl"

# disable networking since we removed openssl
do_configure[network] = "0"
do_compile[network] = "0"
