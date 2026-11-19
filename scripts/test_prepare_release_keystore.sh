#!/usr/bin/env bash
set -euo pipefail

# Unit tests for scripts/prepare_release_keystore.sh using real keytool keystores.
# Run after actions/setup-java has installed a JDK.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_SCRIPT="$SCRIPT_DIR/prepare_release_keystore.sh"

if ! command -v keytool >/dev/null 2>&1; then
    echo "❌ keytool is required to test the release keystore preparation script."
    exit 1
fi

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

PASS_COUNT=0
pass() {
    PASS_COUNT=$((PASS_COUNT + 1))
    echo "✅ $1"
}

assert_line() {
    local file="$1"
    local expected="$2"
    if ! grep -qxF "$expected" "$file"; then
        echo "❌ Expected line missing from $file: $expected" >&2
        cat "$file" >&2
        exit 1
    fi
}

assert_contains() {
    local file="$1"
    local expected="$2"
    if ! grep -qF "$expected" "$file"; then
        echo "❌ Expected text missing from $file: $expected" >&2
        cat "$file" >&2
        exit 1
    fi
}

generate_keystore() {
    local path="$1"
    local alias="$2"
    local storepass="$3"
    local keypass="$4"
    keytool -genkeypair \
        -alias "$alias" -keyalg RSA -keysize 2048 -validity 1 \
        -keystore "$path" -storetype JKS \
        -storepass "$storepass" -keypass "$keypass" \
        -dname "CN=MarbleBr CI Test, OU=Test, O=MarbleBr, L=Tehran, S=Tehran, C=IR" \
        >/dev/null 2>&1
}

add_key() {
    local path="$1"
    local alias="$2"
    local storepass="$3"
    local keypass="$4"
    keytool -genkeypair \
        -alias "$alias" -keyalg RSA -keysize 2048 -validity 1 \
        -keystore "$path" -storetype JKS \
        -storepass "$storepass" -keypass "$keypass" \
        -dname "CN=MarbleBr CI Test 2, OU=Test, O=MarbleBr, L=Tehran, S=Tehran, C=IR" \
        >/dev/null 2>&1
}

run_prepare() {
    local alias="$1"
    local storepass="$2"
    local keypass="$3"
    local base64_data="$4"
    local keyfile="$5"
    local envfile="$6"
    set +e
    KEYSTORE_BASE64="$base64_data" KEYSTORE_PASSWORD="$storepass" \
        KEY_ALIAS="$alias" KEY_PASSWORD="$keypass" \
        KEYSTORE_FILE="$keyfile" GITHUB_ENV="$envfile" \
        bash "$TEST_SCRIPT" >"$WORK_DIR/output.log" 2>&1
    local status=$?
    set -e
    return "$status"
}

KS_ONE="$WORK_DIR/one.jks"
KS_MULTI="$WORK_DIR/multi.jks"
ENV_FILE="$WORK_DIR/github_env"
OUTPUT="$WORK_DIR/output.log"
STORE_PASS='storepass123'
KEY_PASS='keypass123'
ALIAS_ONE='release-key'
ALIAS_TWO='secondary-key'

generate_keystore "$KS_ONE" "$ALIAS_ONE" "$STORE_PASS" "$KEY_PASS"
B64_ONE="$(base64 -w0 "$KS_ONE")"

# 1. A correct alias with whitespace copied from a secret is trimmed and used.
ENV_FILE="$WORK_DIR/env_trim"
if run_prepare $'  release-key \n' $'storepass123\n' $'keypass123\n' \
    "$B64_ONE" "$WORK_DIR/out_trim.jks" "$ENV_FILE"; then
    assert_line "$ENV_FILE" "MARBLE_KEY_ALIAS=release-key"
    assert_line "$ENV_FILE" "MARBLE_KEYSTORE_PASSWORD=storepass123"
    assert_line "$ENV_FILE" "MARBLE_KEY_PASSWORD=keypass123"
    assert_line "$ENV_FILE" "MARBLE_KEYSTORE_PATH=$WORK_DIR/out_trim.jks"
    pass "whitespace in secrets is trimmed and the configured alias is used"
else
    cat "$OUTPUT" >&2
    echo "❌ Correct alias with surrounding whitespace was rejected." >&2
    exit 1
fi

# 2. A stale/wrong KEY_ALIAS falls back to the only key in the keystore.
ENV_FILE="$WORK_DIR/env_fallback"
if run_prepare 'stale-alias' "$STORE_PASS" "$KEY_PASS" \
    "$B64_ONE" "$WORK_DIR/out_fallback.jks" "$ENV_FILE"; then
    assert_contains "$OUTPUT" 'ReleaseKeyAliasMismatch'
    assert_line "$ENV_FILE" "MARBLE_KEY_ALIAS=$ALIAS_ONE"
    pass "a mismatched KEY_ALIAS falls back to the single private key entry"
else
    cat "$OUTPUT" >&2
    echo "❌ Mismatched alias did not fall back to the single key entry." >&2
    exit 1
fi

# 3. An empty KEY_ALIAS is also resolved from a single-key keystore.
ENV_FILE="$WORK_DIR/env_empty_alias"
if run_prepare '' "$STORE_PASS" "$KEY_PASS" \
    "$B64_ONE" "$WORK_DIR/out_empty_alias.jks" "$ENV_FILE"; then
    assert_line "$ENV_FILE" "MARBLE_KEY_ALIAS=$ALIAS_ONE"
    pass "an empty KEY_ALIAS is resolved from the keystore"
else
    cat "$OUTPUT" >&2
    echo "❌ Empty alias was not resolved." >&2
    exit 1
fi

cp "$KS_ONE" "$KS_MULTI"
add_key "$KS_MULTI" "$ALIAS_TWO" "$STORE_PASS" "$KEY_PASS"
B64_MULTI="$(base64 -w0 "$KS_MULTI")"

# 4. With multiple keys, a wrong alias must fail early instead of guessing.
if run_prepare 'stale-alias' "$STORE_PASS" "$KEY_PASS" \
    "$B64_MULTI" "$WORK_DIR/out_ambiguous.jks" "$WORK_DIR/env_ambiguous"; then
    cat "$OUTPUT" >&2
    echo "❌ Ambiguous keystore unexpectedly accepted a wrong alias." >&2
    exit 1
else
    assert_contains "$OUTPUT" 'AmbiguousReleaseKey'
    pass "an ambiguous keystore with a wrong alias fails early"
fi

# 5. With multiple keys, a matching alias is selected.
ENV_FILE="$WORK_DIR/env_multi"
if run_prepare "$ALIAS_TWO" "$STORE_PASS" "$KEY_PASS" \
    "$B64_MULTI" "$WORK_DIR/out_multi.jks" "$ENV_FILE"; then
    assert_line "$ENV_FILE" "MARBLE_KEY_ALIAS=$ALIAS_TWO"
    pass "the configured alias is selected from a multi-key keystore"
else
    cat "$OUTPUT" >&2
    echo "❌ Matching alias in a multi-key keystore was rejected." >&2
    exit 1
fi

# 6. Malformed base64 fails before any build work.
if run_prepare "$ALIAS_ONE" "$STORE_PASS" "$KEY_PASS" \
    '%%%not-base64%%%' "$WORK_DIR/out_bad_b64.jks" "$WORK_DIR/env_bad_b64"; then
    cat "$OUTPUT" >&2
    echo "❌ Invalid base64 keystore was accepted." >&2
    exit 1
else
    assert_contains "$OUTPUT" 'InvalidReleaseKeystore'
    pass "a malformed base64 keystore is rejected"
fi

# 7. A wrong store password fails early.
if run_prepare "$ALIAS_ONE" 'wrong-store-pass' "$KEY_PASS" \
    "$B64_ONE" "$WORK_DIR/out_bad_store.jks" "$WORK_DIR/env_bad_store"; then
    cat "$OUTPUT" >&2
    echo "❌ Wrong store password was accepted." >&2
    exit 1
else
    assert_contains "$OUTPUT" 'InvalidReleaseKeystore'
    pass "a wrong store password is rejected"
fi

# 8. A wrong key password fails early.
if run_prepare "$ALIAS_ONE" "$STORE_PASS" 'wrong-key-pass' \
    "$B64_ONE" "$WORK_DIR/out_bad_key.jks" "$WORK_DIR/env_bad_key"; then
    cat "$OUTPUT" >&2
    echo "❌ Wrong key password was accepted." >&2
    exit 1
else
    assert_contains "$OUTPUT" 'InvalidReleaseKeyPassword'
    pass "a wrong key password is rejected"
fi

echo "🎉 All $PASS_COUNT release keystore tests passed."
