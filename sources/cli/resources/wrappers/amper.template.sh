#!/bin/sh

#
# Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
#

# TODO gradlew also tries to set ulimit -n (max files), probably we should too
# TODO Script could be run in parallel for the first time, so download/extract code should not fail in that case
# TODO Use slim versions of JetBrains Runtime instead
#  We don't need full JDK here since we don't reuse this runtime for compilation/toolchain

# Possible environment variables:
#   AMPER_DOWNLOAD_ROOT        Maven repository to download Amper dist from.
#                              default: https://packages.jetbrains.team/maven/p/amper/amper
#   AMPER_JRE_DOWNLOAD_ROOT    Url prefix to download Amper JRE from.
#                              default: https:/
#   AMPER_BOOTSTRAP_CACHE_DIR  Cache directory to store extracted JRE and Amper distribution, must end with \
#   AMPER_JAVA_HOME            JRE to run Amper itself (optional, does not affect compilation)

set -e -u

amper_version=@AMPER_VERSION@
# Establish chain of trust from here by specifying exact checksum of Amper distribution to be run
amper_sha256=@AMPER_DIST_SHA256@
amper_url="${AMPER_DOWNLOAD_ROOT:-https://packages.jetbrains.team/maven/p/amper/amper}/org/jetbrains/amper/cli/$amper_version/cli-$amper_version-dist.zip"

amper_jre_download_root="${AMPER_JRE_DOWNLOAD_ROOT:-https:/}"

script_dir="$(dirname -- "$0")"
script_dir="$(cd -- "$script_dir" && pwd)"

die () {
  echo >&2
  echo "$@" >&2
  echo >&2
  exit 1
}

download_and_extract() {
  moniker="$1"
  file_url="$2"
  file_sha256="$3"
  cache_dir="$4"
  extract_dir="$5"

  if [ -e "$extract_dir/.flag" ] && [ -n "$(ls "$extract_dir")" ] && [ "x$(cat "$extract_dir/.flag")" = "x${file_url}" ]; then
    # Everything is up-to-date in $extract_dir, do nothing
    true
  else
    mkdir -p "$cache_dir"
    temp_file="$cache_dir/download-file-$$.bin"

    echo "$moniker will now be provisioned because this is the first run. Subsequent runs will skip this step and be faster."
    echo "Downloading $file_url"

    rm -f "$temp_file"
    if command -v curl >/dev/null 2>&1; then
      if [ -t 1 ]; then CURL_PROGRESS="--progress-bar"; else CURL_PROGRESS="--silent --show-error"; fi
      # shellcheck disable=SC2086
      curl $CURL_PROGRESS -L --fail --output "${temp_file}" "$file_url" 2>&1
    elif command -v wget >/dev/null 2>&1; then
      if [ -t 1 ]; then WGET_PROGRESS=""; else WGET_PROGRESS="-nv"; fi
      wget $WGET_PROGRESS -O "${temp_file}" "$file_url" 2>&1
    else
      die "ERROR: Please install wget or curl"
    fi

    check_sha256 "$file_url" "$temp_file" "$file_sha256"

    echo "Extracting to $extract_dir"
    rm -rf "$extract_dir"
    mkdir -p "$extract_dir"

    case "$file_url" in
      *".zip") unzip -q "$temp_file" -d "$extract_dir" ;;
      *) tar -x -f "$temp_file" -C "$extract_dir" ;;
    esac

    rm -f "$temp_file"

    echo "$file_url" >"$extract_dir/.flag"
    echo
  fi
}

# usage: check_sha256 SOURCE_MONIKER FILE SHA256CHECKSUM
# $1 SOURCE_MONIKER (e.g. url)
# $2 FILE
# $3 SHA256 hex string
check_sha256() {
  if command -v shasum >/dev/null 2>&1; then
    echo "$3 *$2" | shasum -a 256 --status -c || {
      echo "$2 (downloaded from $1):" >&2
      echo "expected checksum $3 but got: $(shasum --binary -a 256 "$2" | awk '{print $1}')" >&2

      die "ERROR: Checksum mismatch for $1"
    }
    return 0
  fi

  if command -v sha256sum >/dev/null 2>&1; then
    echo "$3 *$2" | sha256sum -w -c || {
      echo "$2 (downloaded from $1):" >&2
      echo "expected checksum $3 but got: $(sha256sum "$2" | awk '{print $1}')" >&2

      die "ERROR: Checksum mismatch for $1"
    }
    return 0
  fi

  echo "Both 'shasum' and 'sha256sum' utilities are missing. Please install one of them"
  return 1
}

### System detection
kernelName=$(uname -s)
arch=$(uname -m)
case "$kernelName" in
  Darwin* )
    simpleOs="macos"
    default_amper_cache_dir="$HOME/Library/Caches/Amper"
    ;;
  Linux* )
    simpleOs="linux"
    default_amper_cache_dir="$HOME/.cache/Amper"
    # shellcheck disable=SC2046
    arch=$(linux$(getconf LONG_BIT) uname -m)
    ;;
  CYGWIN* | MSYS* | MINGW* )
    simpleOs="windows"
    if command -v cygpath >/dev/null 2>&1; then
      default_amper_cache_dir=$(cygpath -u "$LOCALAPPDATA\Amper")
    else
      die "The 'cypath' command is not available, but Amper needs it. Use amper.bat instead, or try a Cygwin or MSYS environment."
    fi
    ;;
  *)
    die "Unsupported platform $kernelName"
    ;;
esac

# TODO should we respect --shared-caches-root instead of (or in addition to) this env var?
amper_cache_dir="${AMPER_BOOTSTRAP_CACHE_DIR:-$default_amper_cache_dir}"

### JVM
# links from https://github.com/corretto/corretto-17/releases
if [ "x${AMPER_JAVA_HOME:-}" = "x" ]; then
  corretto_version=17.0.9.8.1
  microsoft_jdk_version=17.0.6
  platform="$simpleOs $arch"
  case $platform in
    "macos x86_64")
      jvm_url="$amper_jre_download_root/corretto.aws/downloads/resources/$corretto_version/amazon-corretto-$corretto_version-macosx-x64.tar.gz"
      jvm_target_dir="$amper_cache_dir/amazon-corretto-$corretto_version-macosx-x64"
      jvm_sha256=7eed832eb25b6bb9fed5172a02931804ed0bf65dc86a2ddc751aa7648bb35c43
      ;;
    "macos arm64")
      jvm_url="$amper_jre_download_root/corretto.aws/downloads/resources/$corretto_version/amazon-corretto-$corretto_version-macosx-aarch64.tar.gz"
      jvm_target_dir="$amper_cache_dir/amazon-corretto-$corretto_version-macosx-aarch64"
      jvm_sha256=8a0c542e78e47cb5de1db40763692d55b977f1d0b31c5f0ebf2dd426fa33a2f4
      ;;
    "linux x86_64")
      jvm_url="$amper_jre_download_root/corretto.aws/downloads/resources/$corretto_version/amazon-corretto-$corretto_version-linux-x64.tar.gz"
      jvm_target_dir="$amper_cache_dir/amazon-corretto-$corretto_version-linux-x64"
      jvm_sha256=0cf11d8e41d7b28a3dbb95cbdd90c398c310a9ea870e5a06dac65a004612aa62
      ;;
    "linux aarch64")
      jvm_url="$amper_jre_download_root/corretto.aws/downloads/resources/$corretto_version/amazon-corretto-$corretto_version-linux-aarch64.tar.gz"
      jvm_target_dir="$amper_cache_dir/amazon-corretto-$corretto_version-linux-aarch64"
      jvm_sha256=8141bc6ea84ce103a040128040c2f527418a6aa3849353dcfa3cf77488524499
      ;;
    "windows x86_64")
      jvm_url="$amper_jre_download_root/corretto.aws/downloads/resources/$corretto_version/amazon-corretto-$corretto_version-windows-x64-jdk.zip"
      jvm_target_dir="$amper_cache_dir/amazon-corretto-$corretto_version-windows-x64"
      jvm_sha256=bef1845cbfc5dfc39240d794a31770b0f3f4b7aa179b49536f7b37a4f09985ae
      ;;
    "windows arm64")
      jvm_url="$amper_jre_download_root/aka.ms/download-jdk/microsoft-jdk-$microsoft_jdk_version-windows-aarch64.zip"
      jvm_target_dir="$amper_cache_dir/microsoft-jdk-$microsoft_jdk_version-windows-aarch64"
      jvm_sha256=0a24e2382841387bad274ff70f0c3537e3eb3ceb47bc8bc5dc22626b2cb6a87c
      ;;
    *)
      die "Unsupported platform $platform"
      ;;
  esac

  download_and_extract "A runtime for Amper" "$jvm_url" "$jvm_sha256" "$amper_cache_dir" "$jvm_target_dir"

  AMPER_JAVA_HOME=
  for d in "$jvm_target_dir" "$jvm_target_dir"/* "$jvm_target_dir"/Contents/Home "$jvm_target_dir"/*/Contents/Home; do
    if [ -e "$d/bin/java" ]; then
      AMPER_JAVA_HOME="$d"
    fi
  done

  if [ "x${AMPER_JAVA_HOME:-}" = "x" ]; then
    die "Unable to find bin/java under $jvm_target_dir"
  fi
fi

java_exe="$AMPER_JAVA_HOME/bin/java"
if [ '!' -x "$java_exe" ]; then
  die "Unable to find bin/java executable at $java_exe"
fi

### AMPER
amper_target_dir="$amper_cache_dir/amper-cli-$amper_version"
download_and_extract "The Amper $amper_version distribution" "$amper_url" "$amper_sha256" "$amper_cache_dir" "$amper_target_dir"

if [ "$simpleOs" = "windows" ]; then
  # Can't cygpath the '*' so it has to be outside
  classpath="$(cygpath -w "$amper_target_dir")\lib\*"
else
  classpath="$amper_target_dir/lib/*"
fi
exec "$java_exe" -ea "-Damper.wrapper.dist.sha256=$amper_sha256" "-Damper.wrapper.process.name=$0" -cp "$classpath" org.jetbrains.amper.cli.MainKt "$@"
