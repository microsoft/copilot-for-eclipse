# Copilot Shell Integration for Bash
# This script is loaded with bash --init-file when starting a terminal.

__copilot_bash_integration_main() {
    [ -n "${COPILOT_BASH_INTEGRATION:-}" ] && return
    COPILOT_BASH_INTEGRATION=1

    bind 'set enable-bracketed-paste on' 2>/dev/null || true
    __copilot_prompt_initialized=0

    __copilot_precmd() {
        __copilot_status=$?
        if [ "${__copilot_prompt_initialized:-0}" = "1" ]; then
            printf '\033]7775;C;%s\007' "$__copilot_status"
        else
            __copilot_prompt_initialized=1
        fi
        printf '\033]7775;A\007'
        return "$__copilot_status"
    }

    __copilot_prompt_end() {
        printf '\033]7775;B\007'
    }

    if [ -z "${__copilot_original_ps1:-}" ]; then
        __copilot_original_ps1=${PS1:-'\$ '}
    fi

    case "$(declare -p PROMPT_COMMAND 2>/dev/null)" in
        declare\ -a*|declare\ -A*)
            PROMPT_COMMAND=(__copilot_precmd "${PROMPT_COMMAND[@]}")
            ;;
        *)
            if [ -n "${PROMPT_COMMAND:-}" ]; then
                PROMPT_COMMAND="__copilot_precmd; ${PROMPT_COMMAND}"
            else
                PROMPT_COMMAND=__copilot_precmd
            fi
            ;;
    esac
    PS1="${__copilot_original_ps1}"'\[$(__copilot_prompt_end)\]'
}

__copilot_bash_integration_main