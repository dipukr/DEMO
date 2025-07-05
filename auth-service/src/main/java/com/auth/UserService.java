package com.auth;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	@Autowired
	private UserDAO userDAO;

	public boolean register(String username, String password) {
		if (userDAO.findByUsername(username).isPresent())
			return false;
		User user = new User();
		user.setUsername(username);
		user.setPassword(password); // In production, hash this!
		userDAO.save(user);
		return true;
	}

	public boolean authenticate(String username, String password) {
		Optional<User> user = userDAO.findByUsername(username);
		return user.map(u -> u.getPassword().equals(password)).orElse(false);
	}
}
