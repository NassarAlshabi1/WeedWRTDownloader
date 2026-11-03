package weedwrt.core

/**
 * Generates the on-router installation script (weedwrt.sh, busybox-ash compatible).
 *
 * Every literal shell `$` is written as `${dollar}` so Kotlin does not treat it
 * as a string template.
 *
 * The script is placed next to the `luci-offline` folder inside the ZIP.
 * It works from any directory the user unzips it to, with a fallback to /luci-offline.
 */
object ScriptGenerator {

    fun generate(): String {
        val d = "$"
        return """
#!/bin/sh
# WeedWRT LuCI Offline Installer v${BuildInfo.version}
# Repo: ${BuildInfo.repository}
#
# Usage: copy the whole unzipped folder to your router (e.g. with scp),
# then run:  sh weedwrt.sh

echo "WeedWRT v${BuildInfo.version} - LuCI offline installer"
echo "Original tool by NickP0is0n (github.com/NickP0is0n)"
echo "Maintained by NassarAlshabi1 (${BuildInfo.repository})"
echo " "

SCRIPT_DIR=${d}(cd "${d}(dirname "${d}0")" && pwd)
PKG_DIR="${d}SCRIPT_DIR/luci-offline"

# fallback to the legacy location
if [ ! -d "${d}PKG_DIR" ]; then
    PKG_DIR="/luci-offline"
fi

if [ ! -d "${d}PKG_DIR" ]; then
    echo "[!] Could not find the 'luci-offline' folder next to this script."
    echo "    Unzip the full archive first, then run this script from inside it."
    exit 1
fi

if [ "${d}(id -u)" != "0" ]; then
    echo "[!] This script must run as root. Try: sudo sh weedwrt.sh"
    exit 1
fi

echo "[*] Copying packages to a temp directory"
TMP_DIR="/tmp/luci-offline-packages"
rm -rf "${d}TMP_DIR"
mkdir -p "${d}TMP_DIR"
cp "${d}PKG_DIR"/*.ipk "${d}TMP_DIR"/ || { echo "[!] No .ipk files found"; exit 1; }

echo "[*] Installing LuCI and dependencies (this may take a minute)"
cd "${d}TMP_DIR" || exit 1
if ! opkg install ./*.ipk; then
    echo "[!] First install pass reported errors, retrying with --force-depends"
    opkg install --force-depends ./*.ipk
fi

echo "[*] Removing temp files"
cd /
rm -rf "${d}TMP_DIR"

echo "[*] Restarting services"
/etc/init.d/rpcd restart 2>/dev/null
/etc/init.d/uhttpd enable
/etc/init.d/uhttpd restart

echo " "
echo "Done! LuCI should now be available at http://192.168.1.1"
echo "(open your router's IP address in a browser)"
""".trimIndent() + "\n"
    }
}
