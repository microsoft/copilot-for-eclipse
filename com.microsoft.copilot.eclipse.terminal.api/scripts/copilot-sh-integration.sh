# Copilot Shell Integration for POSIX sh
# This script is sourced via ENV when starting a terminal.

__copilot_sh_integration_main() {
    # Avoid multiple initialization
    [ -n "$COPILOT_SHELL_INTEGRATION" ] && return
    COPILOT_SHELL_INTEGRATION=1

    __COPILOT_MARKER="__COPILOT_CMD_COMPLETE__"

    # The function that prints the marker
    __copilot_precmd() {
        printf '%s\n' "$__COPILOT_MARKER"
    }

    # Save original PS1 only if PS1 is already set and not empty
    if [ -z "$__copilot_original_ps1" ] && [ -n "$PS1" ]; then
        __copilot_original_ps1=$PS1
    fi

    # Ensure PS1 has a value (POSIX shells may start without PS1)
    : "${__copilot_original_ps1:=$ }"

    # newline in POSIX sh
    __NL='
'

    # Assemble PS1:
    #   <newline>
    #   <marker output>
    #   <newline>
    #   <original prompt>
    PS1="${__NL}\$(__copilot_precmd)${__NL}${__copilot_original_ps1}"
}

__copilot_sh_integration_main