# Test script for Weather API
Write-Host "Testing Weather API..."

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/weather/London" -Method Get
    Write-Host "API Response:"
    $response | ConvertTo-Json -Depth 5
} catch {
    Write-Host "Error calling API: $_"
} 