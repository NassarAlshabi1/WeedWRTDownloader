# WeedWRT Downloader
<img src="https://raw.githubusercontent.com/NassarAlshabi1/WeedWRTDownloader/master/Weedwrt_large.png" width="50%">

Tiny utility to automatically generate and build an **offline LuCI install package** for OpenWrt.

Point it at an OpenWrt version and architecture — it downloads LuCI **plus the full dependency closure** from the official feeds and packages everything into a single ZIP with a ready-to-run install script for your router.

- 📱 **Android app** — build offline packages right on your phone
- 💻 **Desktop CLI** — cross-platform Java JAR (Java 8+)
- 🤖 **CI** — GitHub Actions builds a signed release APK and a ready-made offline bundle
- 🔎 **Smart resolver** — no hardcoded package lists; works with renamed packages (e.g. `libubus20230605`, `libjson-c5`) and the ucode-based LuCI of 23.05+

## Downloads (GitHub Releases)

| File | Description |
|------|-------------|
| `WeedWRT-Downloader_v*_signed.apk` | Android app (signed) |
| `weedwrt_23.05.5_mipsel_24kc.zip` | Ready-made offline LuCI bundle for OpenWrt **23.05.5 / mipsel_24kc** |
| `WeedWRT-Downloader-cli_*.jar` | Desktop CLI tool |
| `SHA256SUMS.txt` | Checksums |

## Android app

1. Install the APK.
2. Defaults are pre-set for **OpenWrt 23.05.5 / ramips/mt7621 / mipsel_24kc** — change them if your device differs.
3. Optionally add extra packages (comma separated), e.g. `luci-app-aria2,luci-app-adblock`.
4. Tap **Build offline package** — the ZIP is saved to your Downloads and can be shared directly to your PC.

## CLI usage (Java 8+)

```bash
java -jar WeedWRT-fat.jar <version> <target> <arch> [extra packages]

# OpenWrt 23.05.5 on mipsel_24kc (e.g. MT7621 devices)
java -jar WeedWRT-fat.jar 23.05.5 ramips/mt7621 mipsel_24kc

# with extra LuCI apps
java -jar WeedWRT-fat.jar 23.05.5 ramips/mt7621 mipsel_24kc luci-app-aria2,luci-app-adblock

# nightly snapshot on x86
java -jar WeedWRT-fat.jar snapshot x86/64 x86_64
```

Common target/arch combinations:

| Device family | Target | Arch |
|---|---|---|
| MT7621 (Xiaomi, Netgear, Newifi...) | `ramips/mt7621` | `mipsel_24kc` |
| MT7628/MT7688 | `ramips/mt76x8` | `mipsel_24kc` |
| Atheros/QCA ar71xx | `ath79/generic` | `mips_24kc` |
| Filogic (WiFi 6) | `mediatek/filogic` | `aarch64_generic` |
| Raspberry Pi 4 | `bcm27xx/bcm2711` | `aarch64_cortex-a72` |
| x86 PCs | `x86/64` | `x86_64` |

Build it yourself: `./gradlew :cli:fatJar` → `cli/build/libs/WeedWRT-fat.jar`

## Installing on the router

```bash
scp weedwrt_23.05.5_mipsel_24kc.zip root@192.168.1.1:/tmp/
ssh root@192.168.1.1
cd /tmp && unzip weedwrt_23.05.5_mipsel_24kc.zip && sh weedwrt.sh
```

Then open `http://192.168.1.1` in your browser.

The bundle installs LuCI (`luci-base`, status/system/network modules, bootstrap theme, firewall and package-manager apps) plus **every dependency** — kmods matched to the release kernel included — so it works on a bare OpenWrt image with no internet access.

## CI / GitHub Actions

`.github/workflows/build.yml`:

- On **push** to master: builds the signed APK + CLI JAR + offline bundle (artifacts).
- On **tag `v*`** or manual dispatch with *Publish a GitHub Release*: creates a Release with all files.
- Manual **workflow_dispatch** inputs let you build a bundle for any version/target/arch and any extra packages.

### APK signing

Release APKs are signed with the keystore in `android-app/keystore/weedwrt-release.jks` (a self-signed key committed to this repo so that everyone gets working signed builds out of the box).

⚠️ **Security note:** since that key is public, anyone could sign apps with it. It is fine for a personal utility, but for anything serious generate your own key:

```bash
./scripts/generate-keystore.sh          # creates a private keystore + keystore.properties
```

Then add these repository **secrets** and CI will use your key instead of the committed one:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 weedwrt-release.jks` |
| `KEYSTORE_PASSWORD` | store password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

## Project layout

```
core/         shared Kotlin library: feed parsing, dependency resolver, downloader, zip packager
cli/          desktop command line tool (fat JAR)
android-app/  the Android app
```

## Credits

Original tool by [NickP0is0n](https://github.com/NickP0is0n) — maintained by [NassarAlshabi1](https://github.com/NassarAlshabi1).

---

## الاستخدام بالعربية

أداة لتوليد حزمة تثبيت LuCI **أوفلاين** لأجهزة OpenWrt:

1. حمّل ملف `weedwrt_23.05.5_mipsel_24kc.zip` من صفحة Releases (جاهز لراوتر mipsel_24kc مثل أجهزة MT7621).
2. انسخه للراوتر: `scp weedwrt_23.05.5_mipsel_24kc.zip root@192.168.1.1:/tmp/`
3. على الراوتر: `cd /tmp && unzip weedwrt_*.zip && sh weedwrt.sh`
4. افتح `http://192.168.1.1` من المتصفح.

لتضمين حزم إضافية تختارها (مثل luci-app-aria2): استخدم تطبيق الأندرويد أو أداة سطر الأوامر ومرّر أسماء الحزم مفصولة بفواصل. الأداة تحلّ جميع التبعيات تلقائياً وتنزّل الإصدارات المطابقة لإصدار OpenWrt والمعمارية اللذين تحددهما.
