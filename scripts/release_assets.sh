#!/usr/bin/env bash
# Helpers shared by draft creation and publication. GitHub computes .digest from the uploaded bytes.

list_releases() {
    gh api --paginate 'repos/{owner}/{repo}/releases?per_page=100' | jq -s 'add // []'
}

public_baselines() {
    jq -r '
      [.[] | select(.draft == false and (.tag_name | test("^v[0-9]+\\.[0-9]+\\.[0-9]+$")))]
      | group_by(.prerelease)[]
      | max_by(.tag_name | ltrimstr("v") | split(".") | map(tonumber))
      | .tag_name'
}

verify_release_assets() {
    local release="$1" tag="$2" checksums="$3"
    local abi name digest expected='[]'
    for abi in arm64-v8a armeabi-v7a x86_64 universal; do
        name="MarbleBr-$tag-$abi.apk"
        if ! digest="$(awk -v name="$name" '$2 == name {print $1}' "$checksums")" ||
            [[ ! "$digest" =~ ^[0-9a-f]{64}$ ]]; then
            echo "Missing or invalid SHA-256 entry for $name" >&2
            return 1
        fi
        expected="$(jq -c --arg name "$name" --arg digest "sha256:$digest" \
            '. + [{name:$name,digest:$digest}]' <<< "$expected")"
    done
    [[ "$(wc -l < "$checksums")" -eq 4 ]] || { echo 'Unexpected SHA256SUMS contents' >&2; return 1; }
    digest="$(sha256sum "$checksums" | cut -d' ' -f1)"
    expected="$(jq -c --arg digest "sha256:$digest" '. + [{name:"SHA256SUMS",digest:$digest}]' <<< "$expected")"
    if ! jq -e --argjson expected "$expected" '
        (.assets | all(.state == "uploaded" and .size > 0)) and
        ((.assets | map({name,digest}) | sort_by(.name)) == ($expected | sort_by(.name)))
    ' <<< "$release" >/dev/null; then
        echo "Draft $tag does not contain exactly the verified APKs and checksum file; refusing to proceed." >&2
        return 1
    fi
}
