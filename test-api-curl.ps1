# Test script for Weather API using curl
Write-Host "Testing Weather API for Vietnam using curl..."
Write-Host "NOTE: The API requires Redis to be running on port 6379 for caching."
Write-Host "If you get a 500 Internal Server Error, Redis might not be running."
Write-Host "Option 1: Start Redis locally"
Write-Host "Option 2: Run the application with Docker Compose: docker-compose up -d"
Write-Host "Option 3: Modify the application to not require Redis"
Write-Host ""
Write-Host "Proceeding with test..."

# Using curl to call the API
try {
    Write-Host "Executing: curl http://localhost:8080/api/weather/Vietnam"
    curl http://localhost:8080/api/weather/Vietnam
} catch {
    Write-Host "Error executing curl: $_"
}

# Expected response format when successful:
Write-Host ""
Write-Host "Expected response format when successful:"
Write-Host '{
  "result": "SUCCESS",
  "message": "Weather data retrieved successfully",
  "data": {
    "location": "Vietnam",
    "resolvedAddress": "Vietnam",
    "description": "Clear conditions throughout the day.",
    "currentTemp": 83.3,
    "conditions": "Clear",
    "humidity": 65.2,
    "windSpeed": 3.4,
    "forecast": [
      {
        "date": "2023-06-10",
        "tempMax": 90.5,
        "tempMin": 77.4,
        "conditions": "Clear",
        "precipProbability": 0.0
      }
      // More days...
    ],
    "source": "Visual Crossing"
  }
}' 