package com.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

	@Autowired
	private UserService userService;

	@Autowired
	private JWTUtils jwtUtils;

	@PostMapping("/register")
	public ResponseEntity<String> register(@RequestBody RequestBO req) {
		boolean created = userService.register(req.getUsername(), req.getPassword());
		return created ? ResponseEntity.ok("User registered")
				: ResponseEntity.badRequest().body("User already exists.");
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody RequestBO req) {
		if (userService.authenticate(req.getUsername(), req.getPassword())) {
			String token = jwtUtils.generateToken(req.getUsername());
			return ResponseEntity.ok(new ResponseBO(token));
		} else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body("Invalid credentials.");
		}
	}

	@GetMapping("/verify")
	public ResponseEntity<?> checkLogin(@RequestHeader("Authorization") String authHeader) {
		if (!authHeader.startsWith("Bearer "))
			return ResponseEntity.badRequest().body("Invalid token");
		String token = authHeader.substring(7);
		String username = jwtUtils.validateTokenAndGetUsername(token);
		if (username != null) return ResponseEntity.ok("Logged in as " + username);
		else return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
	}
}
