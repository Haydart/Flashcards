#!/usr/bin/env bash
# Flags violations of the argument-order convention in AGENTS.md / ADR-0020.
# Heuristic, regex-based — not an AST check. Exits non-zero if any violation found.
set -uo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
violations=0

check_file() {
    local file="$1"
    awk -v file="$file" '
        function report(msg) {
            print file ":" sig_start ": " msg
            violations_found = 1
        }
        function reset() { in_sig = 0; fn_name = ""; delete params; nparams = 0 }
        BEGIN { in_sig = 0; violations_found = 0 }
        /^(private |internal )?fun [A-Za-z0-9_]+\(/ {
            in_sig = 1
            sig_start = NR
            fn_name = $0
            sub(/^(private |internal )?fun /, "", fn_name)
            sub(/\(.*/, "", fn_name)
            delete params
            nparams = 0
            next
        }
        in_sig && /^\)/ {
            # closing paren: run checks over collected params
            modifier_idx = 0; vm_idx = 0; svh_idx = 0; first_navcb_idx = 0
            for (i = 1; i <= nparams; i++) {
                p = params[i]
                if (p ~ /^modifier[ :]/) modifier_idx = i
                else if (p ~ /^viewModel[ :]/) vm_idx = i
                else if (p ~ /SavedStateHandle/) svh_idx = i
                else if (p ~ /^onNavigate/ && first_navcb_idx == 0) first_navcb_idx = i
            }
            if (fn_name ~ /Screen$/) {
                if (modifier_idx > 0 && vm_idx > 0 && vm_idx < modifier_idx)
                    report(fn_name ": viewModel must come after modifier (ADR-0020)")
                if (modifier_idx > 0 && first_navcb_idx > 0 && first_navcb_idx < modifier_idx)
                    report(fn_name ": nav callback must come after modifier (ADR-0020)")
                if (vm_idx > 0 && first_navcb_idx > 0 && first_navcb_idx < vm_idx)
                    report(fn_name ": nav callback must come after viewModel (ADR-0020)")
            }
            if (fn_name ~ /Content$/) {
                if (modifier_idx > 0 && modifier_idx != nparams)
                    report(fn_name ": modifier must be the last parameter (ADR-0020)")
            }
            reset()
            next
        }
        in_sig {
            line = $0
            gsub(/^[ \t]+/, "", line)
            if (line != "") { nparams++; params[nparams] = line }
        }
        END { exit violations_found }
    ' "$file"
    if [ $? -ne 0 ]; then
        violations=1
    fi
}

check_ctor() {
    local file="$1"
    awk -v file="$file" '
        function report(msg) { print file ":" sig_start ": " msg; violations_found = 1 }
        function reset() { in_sig = 0; delete params; nparams = 0 }
        BEGIN { in_sig = 0; violations_found = 0 }
        /@Inject constructor\(/ {
            in_sig = 1
            sig_start = NR
            delete params
            nparams = 0
            next
        }
        in_sig && /^\)[ \t]*:/ {
            svh_idx = 0
            for (i = 1; i <= nparams; i++) {
                if (params[i] ~ /SavedStateHandle/) { svh_idx = i; break }
            }
            if (svh_idx > 1)
                report("SavedStateHandle must be the first constructor param (ADR-0020)")
            reset()
            next
        }
        in_sig {
            line = $0
            gsub(/^[ \t]+/, "", line)
            if (line != "") { nparams++; params[nparams] = line }
        }
        END { exit violations_found }
    ' "$file"
    if [ $? -ne 0 ]; then
        violations=1
    fi
}

while IFS= read -r -d '' file; do
    check_file "$file"
done < <(find "$root" \( -name "*Screen.kt" -o -name "*Content.kt" \) -print0 2>/dev/null)

while IFS= read -r -d '' file; do
    check_ctor "$file"
done < <(find "$root" -name "*ViewModel.kt" -print0 2>/dev/null)

if [ "$violations" -ne 0 ]; then
    echo ""
    echo "Argument-order violations found — see AGENTS.md §Argument Order and ADR-0020."
    exit 1
fi

echo "check-arg-order: no violations found."
exit 0
