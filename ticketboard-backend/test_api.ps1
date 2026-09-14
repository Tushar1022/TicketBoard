$loginPayload = '{"email":"admin@ticketboard.com","password":"Admin@123"}'
$loginResp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method POST -Body $loginPayload -ContentType "application/json"
Write-Host "Success:" $loginResp.success
Write-Host "Logged In User:" $loginResp.data.user.email "("$loginResp.data.user.firstName $loginResp.data.user.lastName")"
$token = $loginResp.data.accessToken
Write-Host "JWT Access Token Length:" $token.Length
$headers = @{ "Authorization" = ("Bearer " + $token) }
$dashResp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/dashboards/executive" -Method GET -Headers $headers
Write-Host "Executive Dashboard Total Projects in MySQL DB:" $dashResp.data.totalProjects
Write-Host "Executive Dashboard Delayed Projects in MySQL DB:" $dashResp.data.delayedProjects
Write-Host "Executive Dashboard Total Requirements in MySQL DB:" $dashResp.data.totalRequirements
Write-Host "Executive Dashboard Total Work Items in MySQL DB:" $dashResp.data.totalWorkItems
