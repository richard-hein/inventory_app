package com.example.inventory.security;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class JwtService {

    private static final SecureRandom random = new SecureRandom();


}
