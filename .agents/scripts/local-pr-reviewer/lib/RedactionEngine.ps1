# RedactionEngine.ps1
function Redact-Secrets {
    param (
        [string]$Content
    )

    if ([string]::IsNullOrEmpty($Content)) {
        return $Content
    }

    $sanitized = $Content

    # 1. AWS Access Key
    $sanitized = [regex]::Replace($sanitized, 'AKIA[0-9A-Z]{16}', '[REDACTED_SECRET]')

    # 2. GitHub Personal Access Tokens (ghp_, gho_, ghu_, ghs_, ghr_)
    $sanitized = [regex]::Replace($sanitized, 'gh[pousr]_[0-9a-zA-Z]{36,}', '[REDACTED_SECRET]')

    # 3. Private Keys (RSA, EC, OPENSSH, DSA)
    $sanitized = [regex]::Replace($sanitized, '-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----[\s\S]*?-----END (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----', '[REDACTED_SECRET]')

    # 4. JSON Web Tokens (JWT) / Bearer Tokens
    $sanitized = [regex]::Replace($sanitized, 'eyJ[A-Za-z0-9-_=]+\.eyJ[A-Za-z0-9-_=]+\.[A-Za-z0-9-_.+/=]*', '[REDACTED_SECRET]')

    # 5. Database Connection String Passwords
    $sanitized = [regex]::Replace($sanitized, '(?i)(?:password|pwd)=([^;\s&"'']+)', 'password=[REDACTED_SECRET]')

    # 6. Generic Secret Assignments
    $sanitized = [regex]::Replace($sanitized, '(?i)(?:api[_-]?key|secret[_-]?key|auth[_-]?token|access[_-]?token|private[_-]?key)\s*[:=]\s*["'']?([a-zA-Z0-9_\-]{16,})["'']?', 'api_key=[REDACTED_SECRET]')

    return $sanitized
}
