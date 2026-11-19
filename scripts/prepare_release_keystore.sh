#!/usr/bin/env bash
set -euo pipefail

# Decode, validate and configure the production signing keystore for CI.
#
# The four GitHub Secrets are passed through the environment:
#   KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
#
# KEY_ALIAS is used when it matches a private key entry in the keystore. Secrets
# are often copied with surrounding whitespace, and aliases can change when a
# keystore is regenerated, so when the configured alias does not match we fall
# back to the keystore's only private key entry instead of failing the release
# after a full 4-minute build. With multiple key entries and no match, the
# script fails early with a clear message rather than guessing.
#
# Optional inputs:
#   KEYSTORE_FILE - destination for the decoded keystore
#   KEYTOOL       - keytool binary override (used by the unit tests)
#   GITHUB_ENV    - when set, MARBLE_* signing variables are exported there.

KEYSTORE_FILE="${KEYSTORE_FILE:-${RUNNER_TEMP:-/tmp}/marblebr-release.jks}"
KEYTOOL="${KEYTOOL:-keytool}"

trim_whitespace() {
    local value="${1-}"
    value="${value#"${value%%[![:space:]]*}"}"
    value="${value%"${value##*[![:space:]]}"}"
    printf '%s' "$value"
}

require_secret() {
    local name="$1"
    local value="$2"
    if [[ -z "$value" ]]; then
        echo "::error title=MissingReleaseSecret::GitHub Secret ${name} is required for signed releases."
        exit 1
    fi
}

BASE64_DATA="${KEYSTORE_BASE64:-}"
STORE_PASSWORD="$(trim_whitespace "${KEYSTORE_PASSWORD:-}")"
CONFIGURED_ALIAS="$(trim_whitespace "${KEY_ALIAS:-}")"
KEY_PASSWORD="$(trim_whitespace "${KEY_PASSWORD:-}")"

require_secret KEYSTORE_BASE64 "$BASE64_DATA"
require_secret KEYSTORE_PASSWORD "$STORE_PASSWORD"
require_secret KEY_PASSWORD "$KEY_PASSWORD"

mkdir -p "$(dirname "$KEYSTORE_FILE")"
umask 077
if ! printf '%s' "$BASE64_DATA" | tr -d '[:space:]' | base64 --decode > "$KEYSTORE_FILE" 2>/dev/null; then
    echo "::error title=InvalidReleaseKeystore::KEYSTORE_BASE64 is not a valid base64 keystore."
    exit 1
fi

keytool_list() {
    LC_ALL=C "$KEYTOOL" -J-Duser.language=en -J-Duser.country=US "$@"
}

if ! keytool_list -list -keystore "$KEYSTORE_FILE" -storepass "$STORE_PASSWORD" >/dev/null 2>&1; then
    echo "::error title=InvalidReleaseKeystore::KEYSTORE_PASSWORD cannot open the supplied keystore."
    exit 1
fi

extract_private_key_aliases() {
    keytool_list -list -v -keystore "$KEYSTORE_FILE" -storepass "$STORE_PASSWORD" 2>/dev/null |
        awk '
            /^Alias name: / {
                alias = $0
                sub(/^Alias name:[[:space:]]*/, "", alias)
                sub(/[[:space:]]*$/, "", alias)
            }
            /^Entry type: / && /PrivateKeyEntry/ {
                if (alias != "") print alias
                alias = ""
            }
        '
}

mapfile -t PRIVATE_KEY_ALIASES < <(extract_private_key_aliases)

RESOLVED_ALIAS=""
for alias in "${PRIVATE_KEY_ALIASES[@]}"; do
    if [[ "$alias" == "$CONFIGURED_ALIAS" ]]; then
        RESOLVED_ALIAS="$alias"
        break
    fi
done

if [[ -z "$RESOLVED_ALIAS" ]]; then
    if [[ -n "$CONFIGURED_ALIAS" ]]; then
        echo "::warning title=ReleaseKeyAliasMismatch::KEY_ALIAS does not match any private key in KEYSTORE_BASE64; falling back to the keystore's key entry."
    fi
    if (( ${#PRIVATE_KEY_ALIASES[@]} != 1 )); then
        echo "::error title=AmbiguousReleaseKey::The keystore has ${#PRIVATE_KEY_ALIASES[@]} private key entries and KEY_ALIAS matches none of them. Set KEY_ALIAS to one of the entries."
        exit 1
    fi
    RESOLVED_ALIAS="${PRIVATE_KEY_ALIASES[0]}"
    echo "::notice::Using the single private key entry found in the release keystore."
fi

# -certreq signs the request with the private key, so it actually verifies the
# key password (-exportcert only reads the public certificate and ignores it).
if ! keytool_list -certreq -alias "$RESOLVED_ALIAS" \
    -keystore "$KEYSTORE_FILE" -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" >/dev/null 2>&1; then
    echo "::error title=InvalidReleaseKeyPassword::KEY_PASSWORD cannot unlock the release key, or the resolved alias is unusable."
    exit 1
fi

if [[ -n "${GITHUB_ENV:-}" ]]; then
    {
        echo "MARBLE_KEYSTORE_PATH=${KEYSTORE_FILE}"
        echo "MARBLE_KEYSTORE_PASSWORD=${STORE_PASSWORD}"
        echo "MARBLE_KEY_ALIAS=${RESOLVED_ALIAS}"
        echo "MARBLE_KEY_PASSWORD=${KEY_PASSWORD}"
    } >> "$GITHUB_ENV"
fi

echo "✅ Release keystore validated and signing alias resolved."
