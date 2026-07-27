#!/bin/sh

set -u

required_java_major=21
repository_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
wrapper_path="$repository_root/mvnw"

fail_environment_check() {
  location=$1
  reason=$2
  fix=$3

  cat >&2 <<EOF
StudyTrack environment check failed.
Location: $location
Invariant: StudyTrack builds require JDK 21 and the Maven Wrapper from this repository.
Reason: $reason
Fix: $fix
Recheck: sh ./scripts/check-environment.sh
Then verify: ./mvnw verify
Authority: docs/environment.md and AGENTS.md
EOF
  exit 1
}

if ! command -v java >/dev/null 2>&1; then
  fail_environment_check \
    "java on PATH" \
    "No java executable was found." \
    "Install or select JDK 21, then set JAVA_HOME and PATH for this shell."
fi

java_output=$(java --version 2>&1)
java_exit_code=$?
if [ "$java_exit_code" -ne 0 ]; then
  fail_environment_check \
    "java --version" \
    "The java command exited with code $java_exit_code. Output: $java_output" \
    "Select a working JDK 21 installation and update JAVA_HOME and PATH for this shell."
fi

detected_java_major=$(
  printf '%s\n' "$java_output" |
    sed -n '1s/^[^0-9]*\([0-9][0-9]*\).*/\1/p'
)
if [ -z "$detected_java_major" ]; then
  fail_environment_check \
    "java --version" \
    "The Java major version could not be parsed. Output: $java_output" \
    "Select a standard JDK 21 distribution and ensure its java command is first on PATH."
fi

if [ "$detected_java_major" -ne "$required_java_major" ]; then
  fail_environment_check \
    "java --version" \
    "Detected Java $detected_java_major, but Java $required_java_major is required." \
    "Select JDK 21 and update JAVA_HOME and PATH for this shell; the script never changes them."
fi

if [ ! -f "$wrapper_path" ]; then
  fail_environment_check \
    "$wrapper_path" \
    "The repository Maven Wrapper launcher is missing." \
    "Restore mvnw from the repository; do not substitute a system Maven installation."
fi

if [ ! -x "$wrapper_path" ]; then
  fail_environment_check \
    "$wrapper_path" \
    "The repository Maven Wrapper launcher is not executable." \
    "Restore the executable bit on mvnw and rerun this check."
fi

wrapper_output=$("$wrapper_path" --version 2>&1)
wrapper_exit_code=$?
if [ "$wrapper_exit_code" -ne 0 ]; then
  fail_environment_check \
    "mvnw --version" \
    "The Maven Wrapper exited with code $wrapper_exit_code. Output: $wrapper_output" \
    "Check the Wrapper files and the documented network, proxy, and certificate boundaries."
fi

maven_version=$(
  printf '%s\n' "$wrapper_output" |
    sed -n 's/^Apache Maven[[:space:]][[:space:]]*\([^[:space:]][^[:space:]]*\).*/\1/p'
)
if [ -z "$maven_version" ]; then
  fail_environment_check \
    "mvnw --version" \
    "The Wrapper ran, but its Maven version could not be parsed. Output: $wrapper_output" \
    "Restore the repository Wrapper configuration and rerun this check."
fi

wrapper_java_major=$(
  printf '%s\n' "$wrapper_output" |
    sed -n 's/^Java version:[[:space:]][[:space:]]*\([0-9][0-9]*\).*/\1/p'
)
if [ -z "$wrapper_java_major" ]; then
  fail_environment_check \
    "mvnw --version" \
    "The Java version used by Maven could not be parsed. Output: $wrapper_output" \
    "Set JAVA_HOME to JDK 21 and ensure the Wrapper starts with that runtime."
fi

if [ "$wrapper_java_major" -ne "$required_java_major" ]; then
  fail_environment_check \
    "mvnw --version" \
    "The Wrapper uses Java $wrapper_java_major, but Java $required_java_major is required." \
    "Set JAVA_HOME to JDK 21 for this shell, then rerun the environment check."
fi

printf '%s\n' \
  "StudyTrack environment check passed." \
  "Java: $required_java_major" \
  "Maven Wrapper: Apache Maven $maven_version" \
  "Next: ./mvnw verify"
