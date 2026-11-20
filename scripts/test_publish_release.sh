#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
export MOCK_DIR="$tmp"
cat > "$tmp/gh" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
case "$1 $2" in
    'api --paginate') cat "$MOCK_DIR/releases.json" ;;
    'release download') cp "$MOCK_DIR/SHA256SUMS" "${@: -1}/SHA256SUMS" ;;
    'release view')
        if [[ "$(cat "$MOCK_DIR/edited")" == "latest" ]]; then
            echo '{"isDraft":false,"isPrerelease":false}'
        else
            echo '{"isDraft":false,"isPrerelease":true}'
        fi ;;
    'release edit')
        printf '%s\n' "$*" >> "$MOCK_DIR/calls"
        if [[ "$*" == *'--latest=true'* ]]; then echo latest > "$MOCK_DIR/edited"; else echo prerelease > "$MOCK_DIR/edited"; fi ;;
    *) [[ "$1" == api ]] && echo v2.0.0 ;;
esac
MOCK
chmod +x "$tmp/gh"
export PATH="$tmp:$PATH"
for abi in arm64-v8a armeabi-v7a x86_64 universal; do
    digest="$(printf '%s' "$abi" | sha256sum | cut -d' ' -f1)"
    printf '%s  MarbleBr-v2.0.0-%s.apk\n' "$digest" "$abi"
done > "$tmp/SHA256SUMS"
sha="$(sha256sum "$tmp/SHA256SUMS" | cut -d' ' -f1)"
jq -Rn --arg sha "$sha" '
    [{draft:true,prerelease:false,tag_name:"v2.0.0",assets:
      ([inputs | split("  ") | {name:.[1],digest:("sha256:" + .[0]),state:"uploaded",size:1}]
       + [{name:"SHA256SUMS",digest:("sha256:" + $sha),state:"uploaded",size:1}])}]
' < "$tmp/SHA256SUMS" > "$tmp/good.json"
cp "$tmp/good.json" "$tmp/releases.json"
"$root/scripts/publish_release.sh" v2.0.0 latest
grep -q -- '--draft=false --prerelease=false --latest=true' "$tmp/calls"
"$root/scripts/publish_release.sh" v2.0.0 prerelease
grep -q -- '--draft=false --prerelease=true --latest=false' "$tmp/calls"

reject() {
    jq "$1" "$tmp/good.json" > "$tmp/releases.json"
    rm -f "$tmp/calls"
    if "$root/scripts/publish_release.sh" v2.0.0 latest || [[ -e "$tmp/calls" ]]; then
        echo "Unsafe draft was not rejected: $1" >&2; exit 1
    fi
}
reject 'del(.[0].assets[0])'
reject '.[0].assets[0].digest = "sha256:bad"'
reject '.[0].assets[0].state = "starter"'
reject '.[0].assets += [{name:"MarbleBr-v2.0.0-x86.apk",digest:"sha256:bad",state:"uploaded",size:1}]'
reject '. += [{draft:false,prerelease:true,tag_name:"v2.10.0",assets:[]}]'
reject '.[0].draft = false'
if "$root/scripts/publish_release.sh" 'v2.0.0;bad' latest; then
    echo 'Invalid tags must be rejected' >&2; exit 1
fi
echo 'Release publishing tests passed.'
