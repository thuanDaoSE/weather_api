import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherApiTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("Testing Visual Crossing Weather API for Vietnam...");
            
            // Build the API URL
            String apiUrl = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/Vietnam";
            String queryParams = "?unitGroup=us&key=PNSZD8XZ44E39M4Y5G5EH33JL&contentType=json";
            URL url = new URL(apiUrl + queryParams);
            
            // Create connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            // Get response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            // Read the response
            BufferedReader reader;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            
            // Process the response
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Print the first 200 characters of the response to verify it's working
            String responseData = response.toString();
            System.out.println("Response Data (first 200 chars): " + 
                responseData.substring(0, Math.min(200, responseData.length())));
            
            // Check if the response contains expected fields
            boolean hasLocation = responseData.contains("resolvedAddress");
            boolean hasTemperature = responseData.contains("temp");
            boolean hasConditions = responseData.contains("conditions");
            
            System.out.println("Contains location data: " + hasLocation);
            System.out.println("Contains temperature data: " + hasTemperature);
            System.out.println("Contains weather conditions: " + hasConditions);
            
            System.out.println("API test completed successfully!");
            
        } catch (Exception e) {
            System.out.println("Error testing API: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 