package com.weather.weather_app.service;

import com.weather.weather_app.model.User;
import com.weather.weather_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void registerUser(String imie, String nazwisko, String email, String miasto, String haslo, Date bday) {
        User user = new User();
        user.setImie(imie);
        user.setNazwisko(nazwisko);
        user.setEmail(email);
        user.setMiasto(miasto);
        user.setHaslo(haslo);
        user.setBday(bday);
        userRepository.save(user);
    }

    public void updateCity(User user, String newCity) {
        user.setMiasto(newCity);
        userRepository.save(user);
    }

}
