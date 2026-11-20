#!/usr/bin/env bash
set -euo pipefail

tag="${1:-}"
channel="${2:-}"
if [[ ! "$tag" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || [[ "$channel" != "latest" && "$channel" != "prerelease" ]]; then
    echo "Expected a vX.Y.Z tag and a channel of latest or prerelease." >&2
    exit 1
fi

release="$(gh release view "$tag" --json isDraft,isPrerelease,assets)"
if [[ "$(jq -r '.isDraft' <<< "$release")" != "true" ]]; then
    echo "Release $tag is not a draft; refusing to change a published release." >&2
    exit 1
fi
for abi in arm64-v8a armeabi-v7a x86 x86_64 universal; do
    if ! jq -e --arg name "MarbleBr-${tag}-${abi}.apk" '.assets | any(.name == $name)' <<< "$release" >/dev/null; then
        echo "Draft $tag is missing the $abi APK; refusing to publish." >&2
        exit 1
    fi
done

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
