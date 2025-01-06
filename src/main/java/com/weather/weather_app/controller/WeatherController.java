package com.weather.weather_app.controller;


import com.weather.weather_app.model.ForecastResponse;
import com.weather.weather_app.model.WeatherResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import javax.servlet.http.HttpSession;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class WeatherController {

    @Value("${api.key}")
    private String apiKey;

    @GetMapping("/tomorrow-weather")
    public String tomorrowWeather() {
        return "tomorrow-weather";
    }

    @GetMapping("/index")
    public String index(Model model) {
        return "index";
    }
    @GetMapping("/")
    public String getIndex() {
        return "login";
    }

    @GetMapping("/home")
    public String index(@CookieValue(value = "city", defaultValue = "") String city, Model model) {
        if (!city.isEmpty()) {
            model.addAttribute("city", city);
        }
        return "weather";
    }
    @GetMapping("/weather")
    public String getWeather(@RequestParam(value = "city", required = false) String city,
                             @CookieValue(value = "city", defaultValue = "") String cityFromCookie,
                             Model model, HttpServletResponse response) {
        try {
            if (city == null || city.isEmpty()) {
                city = cityFromCookie;
            }

            if (city == null || city.isEmpty()) {
                model.addAttribute("error", "Podaj Miasto.");
                return "error";
            }

            String decodedCity = URLDecoder.decode(city, "UTF-8");

            model.addAttribute("city", city);

            if (cityFromCookie.isEmpty()) {
                Cookie cityCookie = new Cookie("city", URLEncoder.encode(city, "UTF-8"));
                cityCookie.setMaxAge(60 * 60 * 24 * 7);
                cityCookie.setPath("/");
                response.addCookie(cityCookie);
            }

            String weatherUrl = "http://api.openweathermap.org/data/2.5/weather?q=" + decodedCity + "&appid=" + apiKey + "&units=metric";
            String forecastUrl = "http://api.openweathermap.org/data/2.5/forecast?q=" + decodedCity + "&appid=" + apiKey + "&units=metric";

            RestTemplate restTemplate = new RestTemplate();

            WeatherResponse weatherResponse = restTemplate.getForObject(weatherUrl, WeatherResponse.class);
            ForecastResponse forecastResponse = restTemplate.getForObject(forecastUrl, ForecastResponse.class);

            if (weatherResponse != null) {
                model.addAttribute("city", weatherResponse.getName());
                model.addAttribute("country", weatherResponse.getSys().getCountry());
                model.addAttribute("weatherDescription", weatherResponse.getWeather().get(0).getDescription());
                model.addAttribute("temperature", weatherResponse.getMain().getTemp());
                model.addAttribute("humidity", weatherResponse.getMain().getHumidity());
                model.addAttribute("windSpeed", weatherResponse.getWind().getSpeed());
                String weatherIcon = "wi wi-owm-" + weatherResponse.getWeather().get(0).getId();
                model.addAttribute("weatherIcon", weatherIcon);

                StringBuilder alerts = new StringBuilder();
                if (weatherResponse.getWeather().stream().anyMatch(w -> w.getId() >= 200 && w.getId() < 300)) {
                    alerts.append("⚡ Spodziewane są burze. ");
                }
                if (weatherResponse.getWeather().stream().anyMatch(w -> w.getId() == 600 || w.getId() == 601)) {
                    alerts.append("❄️ Prawdopodobne są opady śniegu. ");
                }
                if (weatherResponse.getMain().getTemp() > 35) {
                    alerts.append("🔥 Spodziewane są wysokie temperatury. ");
                }
                if (weatherResponse.getWind().getSpeed() > 15) {
                    alerts.append("🌬️ Spodziewany jest silny wiatr. ");
                }
                if (weatherResponse.getWeather().stream().anyMatch(w -> w.getId() >= 500 && w.getId() < 600)) {
                    alerts.append("🌧️ Prawdopodobne są intensywne opady deszczu. ");
                }

                model.addAttribute("alerts", alerts.toString().isEmpty() ? "Nie wykryto żadnych ekstremalnych warunków pogodowych." : alerts.toString());
            } else {
                model.addAttribute("error", "Nie znaleziono aktualnych danych o pogodzie.");
            }

            if (forecastResponse != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String tomorrowDate = dateFormat.format(calendar.getTime());

                Map<String, List<ForecastResponse.ForecastItem>> groupedByDay = forecastResponse.getList().stream()
                        .collect(Collectors.groupingBy(item -> item.getDtTxt().split(" ")[0]));

                List<Map<String, String>> forecastList = groupedByDay.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .filter(entry -> entry.getKey().compareTo(tomorrowDate) >= 0)
                        .limit(5)
                        .map(entry -> {
                            String date = entry.getKey();
                            ForecastResponse.ForecastItem middayForecast = entry.getValue().stream()
                                    .filter(item -> item.getDtTxt().contains("12:00:00"))
                                    .findFirst()
                                    .orElse(entry.getValue().get(0));

                            Map<String, String> forecastData = new HashMap<>();
                            forecastData.put("date", date);
                            forecastData.put("temperature", String.valueOf(middayForecast.getMain().getTemp()));
                            forecastData.put("description", middayForecast.getWeather().get(0).getDescription());
                            forecastData.put("icon", "wi wi-owm-" + middayForecast.getWeather().get(0).getId());
                            return forecastData;
                        }).collect(Collectors.toList());

                model.addAttribute("forecastList", forecastList);
            } else {
                model.addAttribute("error", "Nie znaleziono prognozy pogody.");
            }

            return "weather";
        } catch (UnsupportedEncodingException e) {
            model.addAttribute("error", "Błąd Kodowania Miasta.");
            return "error";
        } catch (Exception e) {
            model.addAttribute("error", "Wystąpił niespodziewany błąd: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/weather/tomorrow")
    public String getTomorrowWeather(@RequestParam("city") String city, Model model) {
        try {
            String decodedCity = URLDecoder.decode(city, "UTF-8");

            model.addAttribute("city", city); // nie ruszaj bo się rozjebie całość xd

            String forecastUrl = "http://api.openweathermap.org/data/2.5/forecast?q=" + decodedCity + "&appid=" + apiKey + "&units=metric";

            RestTemplate restTemplate = new RestTemplate();
            ForecastResponse forecastResponse = restTemplate.getForObject(forecastUrl, ForecastResponse.class);

            if (forecastResponse != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String tomorrowDate = dateFormat.format(calendar.getTime());

                List<ForecastResponse.ForecastItem> tomorrowForecast = forecastResponse.getList().stream()
                        .filter(item -> item.getDtTxt().startsWith(tomorrowDate))
                        .collect(Collectors.toList());

                if (!tomorrowForecast.isEmpty()) {
                    StringBuilder tomorrowAlerts = new StringBuilder();
                    boolean thunderstorm = tomorrowForecast.stream().anyMatch(item -> item.getWeather().stream()
                            .anyMatch(w -> w.getId() >= 200 && w.getId() < 300));
                    boolean heavyRain = tomorrowForecast.stream().anyMatch(item -> item.getWeather().stream()
                            .anyMatch(w -> w.getId() >= 500 && w.getId() < 600));
                    boolean snow = tomorrowForecast.stream().anyMatch(item -> item.getWeather().stream()
                            .anyMatch(w -> w.getId() == 600 || w.getId() == 601));
                    boolean strongWind = tomorrowForecast.stream().anyMatch(item -> item.getWind().getSpeed() > 15);
                    boolean highTemp = tomorrowForecast.stream().anyMatch(item -> item.getMain().getTemp() > 35);

                    if (thunderstorm) tomorrowAlerts.append("⚡ Spodziewane są burze. ");
                    if (heavyRain) tomorrowAlerts.append("🌧️ Prawdopodobne są intensywne opady deszczu. ");
                    if (snow) tomorrowAlerts.append("❄️ Prawdopodobne są opady śniegu. ");
                    if (strongWind) tomorrowAlerts.append("🌬️ Spodziewany jest silny wiatr. ");
                    if (highTemp) tomorrowAlerts.append("🔥 Spodziewane są wysokie temperatury. ");

                    model.addAttribute("tomorrowDetails", tomorrowForecast);
                    model.addAttribute("tomorrowAlerts", tomorrowAlerts.toString().isEmpty() ? "Nie wykryto żadnych ekstremalnych warunków pogodowych." : tomorrowAlerts.toString());
                } else {
                    model.addAttribute("error", "Nie znaleziono aktualnych danych o pogodzie.");
                }
            } else {
                model.addAttribute("error", "Nie znaleziono aktualnych danych o pogodzie.");
            }

            return "tomorrow-weather";
        } catch (UnsupportedEncodingException e) {
            model.addAttribute("error", "Błąd Kodowania Miasta.");
            return "error";
        } catch (Exception e) {
            model.addAttribute("error", "Wystąpił niespodziewany błąd xd: " + e.getMessage());
            return "error";
        }
    }

}

