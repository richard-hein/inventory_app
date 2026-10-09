package com.example.inventory;

import org.springframework.boot.SpringApplication;

public class TestInventoryAiApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(InventoryAiApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
