#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
export MOCK_DIR="$tmp"
cat > "$tmp/gh" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
if [[ "$1 $2" == "release view" ]]; then
    if [[ ! -f "$MOCK_DIR/edited" ]]; then
        cat "$MOCK_DIR/draft.json"
    elif [[ "$(cat "$MOCK_DIR/edited")" == "latest" ]]; then
        echo '{"isDraft":false,"isPrerelease":false}'
    else
        echo '{"isDraft":false,"isPrerelease":true}'
    fi
elif [[ "$1 $2" == "release edit" ]]; then
    printf '%s\n' "$*" >> "$MOCK_DIR/calls"
    if [[ "$*" == *'--latest=true'* ]]; then echo latest > "$MOCK_DIR/edited"; else echo prerelease > "$MOCK_DIR/edited"; fi
elif [[ "$1" == "api" ]]; then
    echo v2.0.0
else
    exit 1
fi
MOCK
chmod +x "$tmp/gh"
export PATH="$tmp:$PATH"
cat > "$tmp/draft.json" <<'JSON'
{"isDraft":true,"isPrerelease":false,"assets":[{"name":"MarbleBr-v2.0.0-arm64-v8a.apk"},{"name":"MarbleBr-v2.0.0-armeabi-v7a.apk"},{"name":"MarbleBr-v2.0.0-x86.apk"},{"name":"MarbleBr-v2.0.0-x86_64.apk"},{"name":"MarbleBr-v2.0.0-universal.apk"}]}
JSON
"$root/scripts/publish_release.sh" v2.0.0 latest
grep -q -- '--draft=false --prerelease=false --latest=true' "$tmp/calls"
rm "$tmp/edited"
"$root/scripts/publish_release.sh" v2.0.0 prerelease
grep -q -- '--draft=false --prerelease=true --latest=false' "$tmp/calls"
rm "$tmp/edited"
jq 'del(.assets[0])' "$tmp/draft.json" > "$tmp/incomplete.json"
mv "$tmp/incomplete.json" "$tmp/draft.json"
if "$root/scripts/publish_release.sh" v2.0.0 latest; then
    echo 'Incomplete releases must not be published' >&2; exit 1
fi
if "$root/scripts/publish_release.sh" 'v2.0.0;bad' latest; then
    echo 'Invalid tags must be rejected' >&2; exit 1
fi
echo 'Release publishing tests passed.'
