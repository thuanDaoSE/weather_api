# Test script for Weather API
Write-Host "Testing Weather API for Vietnam..."
Write-Host "NOTE: The API requires Redis to be running on port 6379 for caching."
Write-Host "If you get a 500 Internal Server Error, Redis might not be running."
Write-Host "Option 1: Start Redis locally"
Write-Host "Option 2: Run the application with Docker Compose: docker-compose up -d"
Write-Host "Option 3: Modify the application to not require Redis"
Write-Host ""
Write-Host "Proceeding with test..."

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/weather/Vietnam" -Method Get
    Write-Host "API Response:"
    $response | ConvertTo-Json -Depth 5
} catch {
    Write-Host "Error calling API: $_"
    Write-Host "The error is likely due to Redis not being available."
} 