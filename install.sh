#!/usr/bin/env bash

set -e

. ./config.sh

print_help ()
{
  program_name=$(basename "$0")
  text=$(cat << EOF
usage: $program_name [OPTIONS]

options:

--help                     print this help message
--prefix     [INSTALL_DIR] install project
--uninstall  [INSTALL_DIR] uninstall project
EOF
  )

  echo "$text"
}

__handle_install ()
{
  if [[ -z "$1" ]]; then
    prefix=$INSTALL_DIR
  else
    prefix=$1
  fi

  class=$(echo "$PACKAGE_NAME" | tr / .).Main
  script=$(cat << EOF
#!/usr/bin/env bash

set -e

export PATH=/system/bin/
export CLASSPATH="$prefix/share/afetch/$OUTPUT_APK"

exec app_process -Xnoimage-dex2oat / "$class" "\$@" 2>/dev/null
EOF
)

  mkdir -pv "$prefix/bin"
  echo "Creating executable script..."
  echo "$script" > "$prefix/bin/afetch"
  chmod +x "$prefix/bin/afetch"
  mkdir -pv "$prefix/share/afetch"
  cp -v "$OUTPUT_APK" "$prefix/share/afetch"
  cp -v LICENSE "$prefix/share/afetch"
}

__handle_uninstall ()
{
  if [[ -z "$1" ]]; then
    prefix=$INSTALL_DIR
  else
    prefix=$1
  fi

  if [[ -z "$prefix" ]]; then
    echo "Error: no install prefix specified and INSTALL_DIR is not set in config.sh" >&2
    exit 1
  fi

  if [[ ! -f "$prefix/bin/afetch" ]]; then
    echo "afetch not found at $prefix/bin/afetch — is it installed?"
    exit 1
  fi

  echo "Removing executable script..."
  rm -fv "$prefix/bin/afetch"

  echo "Removing installed files..."
  rm -rfv "$prefix/share/afetch"
}

__handle_prefix ()
{
  if [[ -z "$1" ]]; then
    print_help
    exit 1
  fi

  __handle_install "$1"
}

main ()
{
  if [[ $# -eq 0 ]]; then
    __handle_install
    exit 0
  fi

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --help)
        print_help
        exit 0
        ;;
      --prefix)
        __handle_prefix "$2"
        exit 0
        ;;
      --uninstall)
        __handle_uninstall "$2"
        exit 0
        ;;
      *)
        print_help
        exit 1
        ;;
    esac
    shift
  done
}

main "$@"
