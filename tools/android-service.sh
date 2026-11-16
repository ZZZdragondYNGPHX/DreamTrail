#!/usr/bin/env bash
set -euo pipefail

project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
build_unit=dreamtrail-build.service
emulator_unit=dreamtrail-emulator.service

usage() {
    cat <<'TEXT'
Usage: tools/android-service.sh build [Gradle tasks/options...]
       tools/android-service.sh emulator <AVD name> [emulator options...]
       tools/android-service.sh status | logs <build|emulator> | stop

Set JAVA_HOME to JDK 21 and ANDROID_HOME to your SDK before starting a build.
For an emulator, set ANDROID_HOME and optionally ANDROID_AVD_HOME.
Jobs run asynchronously in separate systemd user services. Build and emulator
share a nonblocking lock so only one can run at a time. Logs go to the journal.
TEXT
}

# Runs inside the service, never in the calling terminal's cgroup.
if [[ ${1:-} == __run ]]; then
    shift
    exec 9>"${XDG_RUNTIME_DIR:?}/dreamtrail-android.lock"
    if ! flock -n 9; then
        echo 'Another DreamTrail Android job is running. Stop it before starting this job.' >&2
        exit 1
    fi
    cd -- "$project_dir"
    exec "$@"
fi

command=${1:-help}
if [[ $# -gt 0 ]]; then shift; fi
case "$command" in
    build)
        : "${JAVA_HOME:?Set JAVA_HOME to JDK 21}"
        : "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK}"
        [[ -x "$JAVA_HOME/bin/java" ]] || { echo 'JAVA_HOME does not contain Java.' >&2; exit 1; }
        [[ $# -gt 0 ]] || set -- :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
        unit=$build_unit
        job=("$project_dir/gradlew" --no-daemon --max-workers=1
             -Pkotlin.compiler.execution.strategy=in-process --console=plain "$@")
        duration=20min
        memory_high=3G
        memory_max=4G
        cpu_quota=150%
        ;;
    emulator)
        : "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK}"
        [[ $# -gt 0 ]] || { usage; exit 1; }
        avd_name=$1; shift
        unit=$emulator_unit
        job=("$ANDROID_HOME/emulator/emulator" -avd "$avd_name" -read-only
             -no-snapshot -no-window -no-audio -memory 1536 -cores 2 "$@")
        duration=30min
        memory_high=4G
        memory_max=5G
        cpu_quota=250%
        ;;
    status)
        systemctl --user show "$build_unit" "$emulator_unit" \
            -p Id -p ActiveState -p Result -p ExecMainStatus -p ControlGroup \
            -p MemoryCurrent -p MemoryPeak -p MemoryMax
        exit
        ;;
    logs)
        case ${1:-} in
            build) unit=$build_unit ;;
            emulator) unit=$emulator_unit ;;
            *) usage; exit 1 ;;
        esac
        journalctl --user -u "$unit" -n 80 --no-pager
        exit
        ;;
    stop)
        for task_unit in "$build_unit" "$emulator_unit"; do
            if [[ $(systemctl --user show "$task_unit" -p LoadState --value) != not-found ]]; then
                systemctl --user stop "$task_unit"
            fi
        done
        exit
        ;;
    help|--help|-h) usage; exit ;;
    *) usage; exit 1 ;;
esac

for other_unit in "$build_unit" "$emulator_unit"; do
    if systemctl --user is-active --quiet "$other_unit"; then
        echo "$other_unit is running; stop it before starting another Android job." >&2
        exit 1
    fi
done

env_options=()
for variable in JAVA_HOME ANDROID_HOME ANDROID_AVD_HOME ANDROID_EMULATOR_HOME ANDROID_ADB_SERVER_PORT DREAMTRAIL_SIGNING_PROPERTIES; do
    if [[ -n ${!variable:-} ]]; then env_options+=("--setenv=$variable=${!variable}"); fi
done
if systemctl --user is-failed --quiet "$unit"; then
    systemctl --user reset-failed "$unit"
fi
systemd-run --user --unit="$unit" --slice=background.slice \
    --property="MemoryHigh=$memory_high" --property="MemoryMax=$memory_max" --property=MemorySwapMax=512M \
    --property="CPUQuota=$cpu_quota" --property=TasksMax=512 --property=KillMode=control-group \
    --property=OOMPolicy=stop --property="RuntimeMaxSec=$duration" \
    "${env_options[@]}" /usr/bin/bash "$project_dir/tools/android-service.sh" __run "${job[@]}"
printf 'Started %s. Use tools/android-service.sh status or logs %s.\n' "$unit" "$command"
