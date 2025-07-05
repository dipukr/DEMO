package com.auth;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ResponseBO {
	
	private String token;

	public ResponseBO(String token) {
		this.token = token;
	}
}
