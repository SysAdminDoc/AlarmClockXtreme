#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

secret_path_regex='(^|/)(keystore\.properties|[^/]+\.(jks|keystore|p12|pfx))$'
required_ignored_paths=(
  "keystore.properties"
  "release.jks"
  "release.keystore"
  "release.p12"
  "release.pfx"
)

fail=0

emit_error() {
  echo "::error::$1" >&2
  fail=1
}

check_secret_list() {
  local label="$1"
  shift
  local path
  while IFS= read -r -d '' path; do
    if [[ "$path" =~ $secret_path_regex ]]; then
      emit_error "Signing secret is $label: $path"
    fi
  done < <("$@" -z)
}

check_secret_list "tracked" git ls-files
check_secret_list "staged" git diff --cached --name-only

for path in "${required_ignored_paths[@]}"; do
  if ! git check-ignore --no-index -q -- "$path"; then
    emit_error "Signing secret path is not ignored by .gitignore: $path"
  fi
done

if [[ "${GITHUB_ACTIONS:-}" == "true" && -e keystore.properties ]]; then
  emit_error "keystore.properties exists before CI secret materialization"
fi

if [[ "$fail" -ne 0 ]]; then
  cat >&2 <<'EOF'
Release signing hygiene check failed.

Keep signing material out of git:
  - do not stage keystore.properties or keystore files
  - keep local keystores in ignored paths only
  - let CI create keystore.properties from repository secrets at release time
EOF
  exit 1
fi

echo "Signing hygiene check passed"
