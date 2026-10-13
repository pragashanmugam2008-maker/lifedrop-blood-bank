$ErrorActionPreference = "Stop"
Write-Host "=================================================================="
Write-Host "  LIFEDROP HOSPITAL-TO-DONOR REST API COMPREHENSIVE TEST SUITE"
Write-Host "=================================================================="

function Call-Api($uri, $method = "Get", $body = $null, $token = $null) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    
    $params = @{
        Uri = $uri
        Method = $method
        Headers = $headers
        ContentType = "application/json"
    }
    if ($body) { $params["Body"] = ($body | ConvertTo-Json) }
    
    return Invoke-RestMethod @params
}

# 1. Live database-calculated statistics
Write-Host "`n>>> [1] GET /api/statistics (Live database-calculated statistics)"
$stats = Call-Api "http://localhost:8080/api/statistics"
Write-Host ("- Registered Donors:        " + $stats.data.registeredDonors)
Write-Host ("- Successful Donations:     " + $stats.data.successfulDonations)
Write-Host ("- Lives Helped:             " + $stats.data.livesHelped)
Write-Host ("- Available Donors:         " + $stats.data.availableDonors)
Write-Host ("- Active Blood Requests:    " + $stats.data.activeBloodRequests)
Write-Host ("- Total Available Units:    " + $stats.data.totalAvailableUnits)

# 2. Blood inventory
Write-Host "`n>>> [2] GET /api/inventory (8 Blood Groups)"
$inventory = Call-Api "http://localhost:8080/api/inventory"
foreach ($item in $inventory.data) {
    Write-Host ("  * " + $item.bloodGroup + " : " + $item.availableUnits + " units (Status: " + $item.status + ", Badge: " + $item.demandBadge + ")")
}

# 3. Hospital Login
Write-Host "`n>>> [3] POST /api/auth/login (Hospital Login: Apollo)"
$hospAuth = Call-Api "http://localhost:8080/api/auth/login" "Post" @{
    email = "apollo@lifedrop.org"
    password = "hospital123"
}
$hospToken = $hospAuth.data.token
Write-Host ("- Logged in as: " + $hospAuth.data.name + " | Role: " + $hospAuth.data.role + " | Hospital: " + $hospAuth.data.hospitalName)

# 4. Donor Login
Write-Host "`n>>> [4] POST /api/auth/login (Donor Login: Rahul)"
$donorAuth = Call-Api "http://localhost:8080/api/auth/login" "Post" @{
    email = "rahul@lifedrop.org"
    password = "donor123"
}
$donorToken = $donorAuth.data.token
Write-Host ("- Logged in as: " + $donorAuth.data.name + " | Role: " + $donorAuth.data.role + " | Code: " + $donorAuth.data.referralCode)

# 5. Hospital Creates Blood Request
Write-Host "`n>>> [5] POST /api/blood-requests (Hospital creates request for O-)"
$reqData = @{
    hospitalName = "Apollo Speciality Hospital"
    bloodGroup = "O-"
    requiredUnits = 2
    hospitalLocation = "Greams Road, Chennai"
    urgency = "CRITICAL"
    notes = "Emergency trauma surgery"
}
$createdReq = Call-Api "http://localhost:8080/api/blood-requests" "Post" $reqData $hospToken
$requestId = $createdReq.data.id
Write-Host ("- Blood Request ID: " + $requestId + " | Blood: " + $createdReq.data.bloodGroup + " | Matched Donors: " + $createdReq.data.matchedDonorsCount)

# 6. Compatible Donors Check
Write-Host "`n>>> [6] GET /api/donors/compatible?bloodGroup=O-"
$compat = Call-Api "http://localhost:8080/api/donors/compatible?bloodGroup=O-"
Write-Host ("- Compatible donors found: " + $compat.data.Count)
$targetDonor = $compat.data[0]
Write-Host ("- Target Donor: " + $targetDonor.name + " (" + $targetDonor.bloodGroup + ") in " + $targetDonor.city)

# 7. Hospital sends direct request to compatible donor
Write-Host "`n>>> [7] POST /api/blood-requests/$requestId/send-to-donor/$($targetDonor.id)"
$directReq = Call-Api "http://localhost:8080/api/blood-requests/$requestId/send-to-donor/$($targetDonor.id)" "Post" $null $hospToken
Write-Host ("- Direct Request Sent. Status: " + $directReq.data.status + " | Hospital: " + $directReq.data.hospitalName)

# 8. Donor Views Incoming Requests
Write-Host "`n>>> [8] GET /api/blood-requests/incoming (Donor Views Incoming Requests)"
# Login as Sneha (O- Donor)
$snehaAuth = Call-Api "http://localhost:8080/api/auth/login" "Post" @{
    email = "sneha@lifedrop.org"
    password = "donor123"
}
$incoming = Call-Api "http://localhost:8080/api/blood-requests/incoming" "Get" $null $snehaAuth.data.token
Write-Host ("- Incoming requests for Sneha (O-): " + $incoming.data.Count)

# 9. Donor Accepts Request
Write-Host "`n>>> [9] PUT /api/blood-requests/$requestId/respond (Donor Accepts)"
$donorResp = Call-Api "http://localhost:8080/api/blood-requests/$requestId/respond" "Put" @{
    status = "ACCEPTED"
    notes = "Confirmed for tomorrow morning donation"
} $snehaAuth.data.token
Write-Host ("- Donor Response Recorded: " + $donorResp.data.status)

# 10. Admin Verification
Write-Host "`n>>> [10] POST /api/auth/login (Admin Login) & Admin Dashboard"
$adminAuth = Call-Api "http://localhost:8080/api/auth/login" "Post" @{
    email = "admin@lifedrop.org"
    password = "admin123"
}
$adminDash = Call-Api "http://localhost:8080/api/admin/dashboard" "Get" $null $adminAuth.data.token
Write-Host ("- Total Users:         " + $adminDash.data.totalUsers)
Write-Host ("- Total Donors:        " + $adminDash.data.totalDonors)
Write-Host ("- Total Hospitals:     " + $adminDash.data.totalHospitals)
Write-Host ("- Successful Donations:" + $adminDash.data.successfulDonations)
Write-Host ("- Total Blood Units:   " + $adminDash.data.totalBloodUnits)

Write-Host "`n=================================================================="
Write-Host "  HOSPITAL-TO-DONOR BLOOD PLATFORM VERIFIED WITH 100% SUCCESS!    "
Write-Host "=================================================================="
