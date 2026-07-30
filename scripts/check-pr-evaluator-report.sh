#!/bin/sh

set -u

authority="WORKFLOW.md and
docs/decisions/031-pr-evaluator-report-lifecycle.md"

fail_report_guard() {
  location=$1
  reason=$2
  fix=$3
  recheck=$4

  cat >&2 <<EOF
StudyTrack pull request evaluator report check failed.
Location: $location
Invariant: The PR body must contain one complete v1 evaluator PASS report whose Subject SHA
matches the pull_request head SHA exactly.
Reason: $reason
Fix: $fix
Recheck: $recheck
Authority: $authority
EOF
  exit 1
}

if [ "$#" -ne 2 ]; then
  fail_report_guard \
    "scripts/check-pr-evaluator-report.sh arguments" \
    "Expected exactly two arguments: a PR body file and the expected head SHA; received $#." \
    "Pass the safely materialized pull_request body file and event head SHA." \
    'sh ./scripts/check-pr-evaluator-report.sh "<body-file>" "<expected-head-sha>"'
fi

body_file=$1
expected_head=$2

if [ ! -f "$body_file" ] || [ ! -r "$body_file" ]; then
  fail_report_guard \
    "PR body file: $body_file" \
    "The PR body file is missing or unreadable." \
    "Materialize pull_request.body from GITHUB_EVENT_PATH into a readable runner temp file." \
    "sh ./scripts/check-pr-evaluator-report.sh \"$body_file\" \"$expected_head\""
fi

case "$expected_head" in
  '' | *[!0-9A-Fa-f]*)
    fail_report_guard \
      "expected head SHA: $expected_head" \
      "The expected head SHA is not a complete hexadecimal commit identifier." \
      "Pass the exact pull_request.head.sha from GITHUB_EVENT_PATH." \
      "sh ./scripts/check-pr-evaluator-report.sh \"$body_file\" \"$expected_head\""
    ;;
esac

if [ "${#expected_head}" -ne 40 ]; then
  fail_report_guard \
    "expected head SHA: $expected_head" \
    "The expected head SHA must contain exactly 40 hexadecimal characters." \
    "Pass the complete pull_request.head.sha without abbreviation." \
    "sh ./scripts/check-pr-evaluator-report.sh \"$body_file\" \"$expected_head\""
fi

begin_marker='<!-- studytrack-evaluator-report:v1:start -->'
end_marker='<!-- studytrack-evaluator-report:v1:end -->'

if parsed_report=$(
  awk -v begin_marker="$begin_marker" -v end_marker="$end_marker" '
    function trim(value) {
      sub(/^[[:space:]]+/, "", value)
      sub(/[[:space:]]+$/, "", value)
      return value
    }

    function remember_error(message) {
      if (error == "") {
        error = message
      }
    }

    function field_index(line, candidate) {
      for (candidate = 1; candidate <= 8; candidate++) {
        if (candidate >= 4 && candidate <= 7) {
          if (line == labels[candidate]) {
            return candidate
          }
        } else {
          if (line == labels[candidate] || index(line, labels[candidate] " ") == 1) {
            return candidate
          }
        }
      }
      return 0
    }

    function require_section_content(section) {
      if (section >= 4 && section <= 7 && !has_content[section]) {
        remember_error("Required report field is empty: " labels[section])
      }
    }

    BEGIN {
      labels[1] = "Subject SHA:"
      labels[2] = "Generator:"
      labels[3] = "Evaluator:"
      labels[4] = "Commands executed:"
      labels[5] = "Independent scenarios:"
      labels[6] = "Findings:"
      labels[7] = "Residual gaps:"
      labels[8] = "Verdict:"
    }

    {
      sub(/\r$/, "")

      if ($0 == begin_marker) {
        begin_count++
        if (begin_count == 1 && !inside && !closed) {
          inside = 1
        } else {
          remember_error("The v1 begin marker is duplicated or out of order.")
        }
        next
      }

      if ($0 == end_marker) {
        end_count++
        if (!inside) {
          remember_error("The v1 end marker is duplicated or appears before the begin marker.")
          next
        }
        require_section_content(current)
        if (current != 8) {
          remember_error("Required report fields are missing or out of fixed order.")
        }
        inside = 0
        closed = 1
        next
      }

      if (!inside) {
        next
      }

      detected = field_index($0)
      if (detected > 0) {
        require_section_content(current)
        if (detected != current + 1) {
          message = "Report fields are duplicated, missing, or out of fixed order at "
          remember_error(message labels[detected])
          next
        }

        current = detected
        if (current <= 3 || current == 8) {
          value = trim(substr($0, length(labels[current]) + 1))
          if (value == "") {
            remember_error("Required report field is empty: " labels[current])
          }
          values[current] = value
        }
        next
      }

      if (current == 0) {
        if (trim($0) != "") {
          remember_error("Unexpected content appears before Subject SHA.")
        }
      } else if (current >= 4 && current <= 7) {
        if (trim($0) != "") {
          has_content[current] = 1
        }
      } else if (current == 8 && trim($0) != "") {
        remember_error("Unexpected content appears after Verdict.")
      }
    }

    END {
      if (begin_count != 1) {
        remember_error("Expected exactly one v1 begin marker; found " begin_count ".")
      }
      if (end_count != 1) {
        remember_error("Expected exactly one v1 end marker; found " end_count ".")
      }
      if (inside || !closed) {
        remember_error("The v1 marker region is not closed in the correct order.")
      }
      require_section_content(current)
      if (current != 8) {
        remember_error("Required report fields are missing or out of fixed order.")
      }

      if (error != "") {
        print error > "/dev/stderr"
        exit 1
      }

      print values[1]
      print values[8]
    }
  ' "$body_file" 2>&1
); then
  :
else
  fail_report_guard \
    "PR body v1 evaluator report: $body_file" \
    "$parsed_report" \
    "Replace the marker region with one complete report using the fixed v1 field order." \
    "sh ./scripts/check-pr-evaluator-report.sh \"$body_file\" \"$expected_head\""
fi

reported_subject=$(printf '%s\n' "$parsed_report" | sed -n '1p')
reported_verdict=$(printf '%s\n' "$parsed_report" | sed -n '2p')

if [ "$reported_subject" != "$expected_head" ]; then
  fail_report_guard \
    "Subject SHA in PR body: $reported_subject" \
    "The report Subject SHA does not match the pull_request head SHA $expected_head." \
    "Re-evaluate the exact current head and replace the body report with that PASS result." \
    "sh ./scripts/check-pr-evaluator-report.sh \"$body_file\" \"$expected_head\""
fi

if [ "$reported_verdict" != "PASS" ]; then
  fail_report_guard \
    "Verdict in PR body: $reported_verdict" \
    "The current evaluator report verdict is not exactly PASS." \
    "Resolve FAIL findings or inconclusive evidence, then obtain a new evaluator PASS." \
    "sh ./scripts/check-pr-evaluator-report.sh \"$body_file\" \"$expected_head\""
fi
