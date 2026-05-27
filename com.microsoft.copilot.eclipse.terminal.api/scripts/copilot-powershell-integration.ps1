# Copilot Shell Integration for Windows PowerShell
# This script is loaded when starting a Copilot terminal.

try {
    $global:OutputEncoding = New-Object System.Text.UTF8Encoding $false
    [Console]::InputEncoding = $global:OutputEncoding
    [Console]::OutputEncoding = $global:OutputEncoding
} catch {
    # Some hosts do not expose console encodings during startup.
}

if (-not $global:COPILOT_SHELL_INTEGRATION) {
    $global:COPILOT_SHELL_INTEGRATION = $true
    $global:__copilot_original_prompt = (Get-Command prompt -CommandType Function).ScriptBlock
    $global:__copilot_last_history_id = -1

    function global:prompt {
        $lastSuccess = $?
        $lastExitCode = $LASTEXITCODE
        $esc = [char]27
        $bel = [char]7
        $lastHistoryEntry = Get-History -Count 1
        $result = ""

        if ($global:__copilot_last_history_id -ne -1) {
            if ($null -ne $lastHistoryEntry -and $lastHistoryEntry.Id -eq $global:__copilot_last_history_id) {
                $result += "$esc]7775;C$bel"
            } else {
                if ($lastSuccess) {
                    $exitCode = 0
                } elseif ($null -ne $lastExitCode -and $lastExitCode -ne 0) {
                    $exitCode = $lastExitCode
                } else {
                    $exitCode = 1
                }
                $result += "$esc]7775;C;$exitCode$bel"
            }
        }

        $result += "$esc]7775;A$bel"

        if ($global:__copilot_original_prompt) {
            $result += $global:__copilot_original_prompt.Invoke()
        } else {
            $result += "PS $($executionContext.SessionState.Path.CurrentLocation)> "
        }

        $result += "$esc]7775;B$bel"
        if ($null -ne $lastHistoryEntry) {
            $global:__copilot_last_history_id = $lastHistoryEntry.Id
        } else {
            $global:__copilot_last_history_id = 0
        }
        $result
    }
}