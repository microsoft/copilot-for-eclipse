# Copilot Shell Integration for POSIX sh
# This script is sourced via ENV when starting a terminal.

__copilot_sh_integration_main() {
    # Avoid multiple initialization
    [ -n "$COPILOT_SHELL_INTEGRATION" ] && return
    COPILOT_SHELL_INTEGRATION=1

    # OSC escape sequence: ESC ] 7775 ; C BEL
    # This is invisible in terminal but detectable programmatically
    __COPILOT_MARKER=$(printf '\033]7775;C\007')

    # The function that prints the marker
    __copilot_precmd() {
        printf '%s' "$__COPILOT_MARKER"
    }

    # Save original PS1 only if PS1 is already set and not empty
    if [ -z "$__copilot_original_ps1" ] && [ -n "$PS1" ]; then
        __copilot_original_ps1=$PS1
    fi

    # Ensure PS1 has a value (POSIX shells may start without PS1)
    : "${__copilot_original_ps1:=$ }"

    # Assemble PS1:
    #   <marker output> (invisible OSC sequence)
    #   <original prompt>
    PS1="\$(__copilot_precmd)${__copilot_original_ps1}"
}

__copilot_sh_integration_main