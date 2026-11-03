$baseUrl = "https://lifedrop-blood-bank-3ypa.onrender.com"
$corsOrigin = "https://lifedrop-blood-bank.vercel.app"

Write-Host "=================================================================="
Write-Host "  LIFEDROP PRODUCTION AUTHENTICATION & LOGIN PERFORMANCE SUITE"
Write-Host "=================================================================="
Write-Host "Target: $baseUrl"
Write-Host "Simulated Vercel Frontend Origin: $corsOrigin"
Write-Host "Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')`n"

function Run-AuthTest {
    param(
        [string]$Name,
        [string]$Endpoint,
        [hashtable]$Body,
        [int]$ExpectedStatus = 200,
        [string]$ExpectedMessage = ""
    )
    
    $corrId = "test-corr-" + [Guid]::NewGuid().ToString().Substring(0, 8)
    $url = "$baseUrl$Endpoint"
    $json = $Body | ConvertTo-Json
    $headers = @{
        "Origin" = $corsOrigin
        "X-Correlation-ID" = $corrId
    }

    Write-Host ">>> [$Name]"
    Write-Host "    POST $Endpoint | CorrelationID: $corrId"
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    
    $status = 0
    $respBody = $null
    $respHeaders = $null
    
    try {
        $req = [System.Net.HttpWebRequest]::Create($url)
        $req.Method = "POST"
        $req.ContentType = "application/json"
        $req.Headers.Add("Origin", $corsOrigin)
        $req.Headers.Add("X-Correlation-ID", $corrId)
        $req.Timeout = 30000
        
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
        $req.ContentLength = $bytes.Length
        $reqStream = $req.GetRequestStream()
        $reqStream.Write($bytes, 0, $bytes.Length)
        $reqStream.Close()
        
        $resp = $req.GetResponse()
        $sw.Stop()
        $status = [int]$resp.StatusCode
        $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
        $respBody = $reader.ReadToEnd()
        $respHeaders = $resp.Headers
    } catch {
        $sw.Stop()
        $webEx = $_.Exception
        if ($webEx.InnerException -and $webEx.InnerException -is [System.Net.WebException]) {
            $webEx = $webEx.InnerException
        }
        if ($webEx -is [System.Net.WebException] -and $webEx.Response) {
            $httpResp = [System.Net.HttpWebResponse]$webEx.Response
            $status = [int]$httpResp.StatusCode
            $reader = New-Object System.IO.StreamReader($httpResp.GetResponseStream())
            $respBody = $reader.ReadToEnd()
            $respHeaders = $httpResp.Headers
        } elseif ($_.Exception.Response) {
            $httpResp = [System.Net.HttpWebResponse]$_.Exception.Response
            $status = [int]$httpResp.StatusCode
            $reader = New-Object System.IO.StreamReader($httpResp.GetResponseStream())
            $respBody = $reader.ReadToEnd()
            $respHeaders = $httpResp.Headers
        } else {
            $status = 500
            $respBody = $_.Exception.Message
        }
    }

    $elapsed = $sw.ElapsedMilliseconds
    $corsHeader = if ($respHeaders) { $respHeaders.Get("access-control-allow-origin") } else { "none" }
    $echoCorrId = if ($respHeaders) { $respHeaders.Get("X-Correlation-ID") } else { "none" }
    
    $passed = ($status -eq $ExpectedStatus)
    $badge = if ($passed) { "PASS" } else { "FAIL" }
    
    Write-Host "    Result: [$badge] Status: $status (Expected: $ExpectedStatus) | Time: ${elapsed} ms"
    Write-Host "    CORS Origin Allowed: $corsHeader | Echoed CorrelationID: $echoCorrId"
    
    if ($respBody -and $respBody.Length -gt 0) {
        $summary = if ($respBody.Length -gt 160) { $respBody.Substring(0, 160) + "..." } else { $respBody }
        Write-Host "    Body: $summary"
    }
    Write-Host ""
    
    return [PSCustomObject]@{
        Name = $Name
        Status = $status
        Expected = $ExpectedStatus
        TimeMs = $elapsed
        Passed = $passed
        Cors = $corsHeader
    }
}

$results = @()

# 1. Correct Hospital Credentials
$results += Run-AuthTest "1. Hospital Login (Apollo)" "/api/auth/login" @{
    email = "apollo@lifedrop.org"
    password = "hospital123"
} 200

# 2. Correct Donor Credentials
$results += Run-AuthTest "2. Donor Login (Rahul)" "/api/auth/login" @{
    email = "rahul@lifedrop.org"
    password = "donor123"
} 200

# 3. Correct Admin Credentials
$results += Run-AuthTest "3. Admin Login (Administrator)" "/api/auth/login" @{
    email = "admin@lifedrop.org"
    password = "admin123"
} 200

# 4. Incorrect Password
$results += Run-AuthTest "4. Incorrect Password" "/api/auth/login" @{
    email = "rahul@lifedrop.org"
    password = "WrongPassword999!"
} 401

# 5. Invalid / Unregistered Email
$results += Run-AuthTest "5. Non-Existent Email" "/api/auth/login" @{
    email = "nobody_exists_12345@lifedrop.org"
    password = "anypassword"
} 401

# 6. Registration Flow: New Donor
$randId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$results += Run-AuthTest "6. Registration Flow (New Donor)" "/api/auth/register" @{
    name = "Test New Donor $randId"
    email = "newdonor_$randId@lifedrop.org"
    password = "SecurePassword123!"
    phone = "+91 9840001122"
    role = "DONOR"
    bloodGroup = "O+"
    age = 27
    gender = "Male"
    city = "Chennai"
    address = "100 Anna Nagar, Chennai"
} 201

# 7. Duplicate Registration (Should Reject Fast)
$results += Run-AuthTest "7. Duplicate Email Registration" "/api/auth/register" @{
    name = "Duplicate Rahul"
    email = "rahul@lifedrop.org"
    password = "SecurePassword123!"
    phone = "+91 9840001122"
    role = "DONOR"
    bloodGroup = "O+"
} 409

# 8. Fast Second Login (Warm pool verification)
$results += Run-AuthTest "8. Warm Cache Login (Apollo)" "/api/auth/login" @{
    email = "apollo@lifedrop.org"
    password = "hospital123"
} 200

# Summary Report
Write-Host "=================================================================="
Write-Host "  TEST RESULTS SUMMARY"
Write-Host "=================================================================="
$passCount = ($results | Where-Object { $_.Passed }).Count
$totalCount = $results.Count
Write-Host "Score: $passCount / $totalCount Passed"
foreach ($r in $results) {
    $mark = if ($r.Passed) { "[PASS]" } else { "[FAIL]" }
    Write-Host ("  {0,6} {1,-35} : {2,5} ms (Status: {3})" -f $mark, $r.Name, $r.TimeMs, $r.Status)
}
Write-Host "=================================================================="
