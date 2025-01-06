package com.weather.weather_app.controller;

import com.weather.weather_app.model.User;
import com.weather.weather_app.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Date;

@Controller
public class UserController {

    @Autowired
    private UserService userService;
    private HttpServletResponse response;

    @GetMapping("/login")
    public String getLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String haslo, Model model, HttpServletResponse response) {
        User user = userService.findByEmail(email);
        if (user != null && user.getHaslo().equals(haslo)) {
            String city = user.getMiasto();
            try {
                Cookie cityCookie = new Cookie("city", URLEncoder.encode(city, "UTF-8"));
                cityCookie.setMaxAge(60 * 60 * 24 * 7);
                cityCookie.setPath("/");
                response.addCookie(cityCookie);

                Cookie emailCookie = new Cookie("email", URLEncoder.encode(email, "UTF-8"));
                emailCookie.setMaxAge(60 * 60 * 24 * 7);
                emailCookie.setHttpOnly(true);
                emailCookie.setPath("/");
                response.addCookie(emailCookie);

                String encodedCity = URLEncoder.encode(city, "UTF-8");
                return "redirect:/weather?city=" + encodedCity;
            } catch (UnsupportedEncodingException e) {
                model.addAttribute("error", "Błąd Kodowania Miasta");
                return "login";
            }
        } else {
            model.addAttribute("error", "Nieprawidłowy Email lub Hasło");
            return "login";
        }
    }



    @GetMapping("/register")
    public String getRegisterPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String imie, @RequestParam String nazwisko, @RequestParam String email,
                               @RequestParam String miasto, @RequestParam String haslo, @RequestParam String bday, Model model) {
        userService.registerUser(imie, nazwisko, email, miasto, haslo, Date.valueOf(bday));
        model.addAttribute("message", "Zajerestrowano Pomyślnie!");
        return "login";
    }

    @GetMapping("/change-city")
    public String getChangeCityPage() {
        return "change-city"; // Wyświetla formularz zmiany miasta
    }

    @PostMapping("/change-city")
    public String changeCity(@RequestParam String newCity, @CookieValue(value = "email", required = false) String email, Model model) {
        if (email == null) {
            model.addAttribute("error", "Nie jesteś zalogowany. Brak informacji o użytkowniku.");
            return "login";
        }

        User user = userService.findByEmail(email);
        if (user != null) {
            userService.updateCity(user, newCity);
            model.addAttribute("message", "Miasto zostało zaktualizowane pomyślnie.");
            return "login";
        } else {
            model.addAttribute("error", "Nie znaleziono użytkownika.");
            return "error";
        }
    }





}
