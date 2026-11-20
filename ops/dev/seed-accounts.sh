#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../lib/env.sh
source "$SCRIPT_DIR/../lib/env.sh"

usage()
{
    cat <<EOF
Usage: $(basename "$0") [admin-password] [professor-password]

Seeds one local test account per role in $DESS_ENV_FILE:

  admin      ADMIN
  professor  PROFESSOR

Passwords are generated when an argument is omitted. Nothing is committed: the
script writes the passwords to the untracked .env file only, which the
application reads at startup. Restart WildFly after running it.

Examples:
  ./ops/dev/seed-accounts.sh                 # generate both passwords
  ./ops/dev/seed-accounts.sh admin123 prof123
EOF
}

if (( $# > 2 ))
then
    usage >&2
    exit 64
fi

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]
then
    usage
    exit 0
fi

if [[ ! -f "$DESS_ENV_FILE" ]]
then
    echo "error: $DESS_ENV_FILE does not exist." >&2
    echo "       Copy .env.example to .env and set the database values first." >&2
    exit 1
fi

generate_password()
{
    if command -v openssl >/dev/null 2>&1
    then
        openssl rand -hex 12
        return
    fi

    echo "error: openssl is required to generate passwords; pass both passwords as arguments." >&2
    exit 1
}

admin_password="${1:-}"
professor_password="${2:-}"

if [[ -z "$admin_password" ]]
then
    admin_password="$(generate_password)"
fi

if [[ -z "$professor_password" ]]
then
    professor_password="$(generate_password)"
fi

quote_for_env()
{
    # Single-quote the value so .env keeps it as one shell word. Embedded single
    # quotes become '\'' as in normal shell quoting.
    printf "'%s'" "${1//\'/\'\\\'\'}"
}

temp_file="$(mktemp "${DESS_ENV_FILE}.XXXXXX")"
trap 'rm -f "$temp_file"' EXIT

# Drop a previous block, then append the new one. Appending is safe because the
# last assignment wins when .env is sourced.
awk '
    /^# BEGIN DESS test accounts/ { skip = 1; next }
    /^# END DESS test accounts/   { skip = 0; next }
    !skip                         { print }
' "$DESS_ENV_FILE" > "$temp_file"

cat >> "$temp_file" <<EOF
# BEGIN DESS test accounts (managed by ops/dev/seed-accounts.sh)
DESS_ADMIN_PASSWORD=$(quote_for_env "$admin_password")
DESS_PROFESSOR_PASSWORD=$(quote_for_env "$professor_password")
# END DESS test accounts
EOF

chmod 600 "$temp_file"
mv "$temp_file" "$DESS_ENV_FILE"
trap - EXIT

echo "Seeded test accounts in $DESS_ENV_FILE:"
echo "  admin     / $admin_password  (ADMIN)"
echo "  professor / $professor_password  (PROFESSOR)"

if [[ -f "$DESS_REPO_ROOT/.run/wildfly.pid" ]]
then
    echo
    echo "WildFly is running. Restart it to recreate the in-memory accounts:"
    echo "  ./ops/wildfly/stop.sh"
    echo "  ./ops/wildfly/run.sh"
fi
