package com.DM.dairyManagement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

@SpringBootApplication
public class DairyManagementApplication {
	public static void main(String[] args) {
		// Load environment variables from .env file
		loadEnvFile();
		
		SpringApplication.run(DairyManagementApplication.class, args);
		System.out.println("Ready to run..");
	}
	
	private static void loadEnvFile() {
		try {
			File envFile = new File(".env");
			if (envFile.exists()) {
				Properties props = new Properties();
				FileInputStream fis = new FileInputStream(envFile);
				props.load(fis);
				fis.close();
				
				// Set system properties from .env file
				for (String key : props.stringPropertyNames()) {
					System.setProperty(key, props.getProperty(key));
				}
				
				System.out.println("Environment variables loaded from .env file");
			} else {
				System.out.println("No .env file found, using default values");
			}
		} catch (Exception e) {
			System.out.println("Error loading .env file: " + e.getMessage());
		}
	}
}