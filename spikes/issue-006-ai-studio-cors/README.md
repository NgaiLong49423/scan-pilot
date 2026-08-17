# AI Studio to Cloud Run CORS Spike

Temporary, credential-free code for Scan Pilot Eligibility Spike Issue #6.

## Contract

- Endpoint: `GET /spike/ping`
- Allowed browser origin: `https://aistudio.google.com`
- Credentials, cookies, repository data, Gemini calls, and application state: not used
- Scaling target: `min instances = 0`, `max instances = 1`

The code is not a Scan Pilot production API and must not be promoted into the product backend.

## Local verification

```powershell
$env:PORT = 8080
py main.py
```

In another PowerShell window:

```powershell
Invoke-WebRequest http://localhost:8080/spike/ping -Headers @{ Origin = 'https://aistudio.google.com' }
```

## Expected browser test

From the actual AI Studio page, a browser request to the deployed URL must return the JSON payload and an `Access-Control-Allow-Origin: https://aistudio.google.com` response header. A request from any other browser origin must not receive that CORS allow header.
