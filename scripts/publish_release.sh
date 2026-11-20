#!/usr/bin/env bash
set -euo pipefail

tag="${1:-}"
channel="${2:-}"
if [[ ! "$tag" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || [[ "$channel" != "latest" && "$channel" != "prerelease" ]]; then
    echo "Expected a vX.Y.Z tag and a channel of latest or prerelease." >&2
    exit 1
fi

source "$(dirname "$0")/release_assets.sh"
releases="$(list_releases)"
release="$(jq -ce --arg tag "$tag" '.[] | select(.tag_name == $tag and .draft)' <<< "$releases")" || {
    echo "Release $tag is not a draft; refusing to publish." >&2
    exit 1
}
newest="$( { printf '%s\n' "$tag"; public_baselines <<< "$releases"; } | sort -V | tail -1)"
if [[ "$newest" != "$tag" ]]; then
    echo "Draft $tag is older than published $newest; publishing it could offer users a downgrade." >&2
    exit 1
fi
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
gh release download "$tag" --pattern SHA256SUMS --dir "$tmp"
verify_release_assets "$release" "$tag" "$tmp/SHA256SUMS"

if [[ "$channel" == "latest" ]]; then
    gh release edit "$tag" --draft=false --prerelease=false --latest=true
else
    gh release edit "$tag" --draft=false --prerelease=true --latest=false
fi

result="$(gh release view "$tag" --json isDraft,isPrerelease)"
[[ "$(jq -r '.isDraft' <<< "$result")" == "false" ]]
if [[ "$channel" == "latest" ]]; then
    [[ "$(jq -r '.isPrerelease' <<< "$result")" == "false" ]]
    [[ "$(gh api 'repos/{owner}/{repo}/releases/latest' --jq .tag_name)" == "$tag" ]]
else
    [[ "$(jq -r '.isPrerelease' <<< "$result")" == "true" ]]
fi
echo "Published $tag as $channel and verified its status."
