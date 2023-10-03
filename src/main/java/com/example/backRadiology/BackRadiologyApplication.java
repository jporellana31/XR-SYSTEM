package com.example.backRadiology;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import com.example.backRadiology.infraestructure.services.login.EmailSenderService;

@SpringBootApplication
public class BackRadiologyApplication {

	// @Autowired
	// private EmailSenderService senderService;

	public static void main(String[] args) {
		SpringApplication.run(BackRadiologyApplication.class, args);
	}

	//>>>>>>>>>>>PRUEBA DE ENVIO DE EMAIL<<<<<<<<<<<<<<<<
	// @EventListener(ApplicationReadyEvent.class)
	// public void triggerMail() throws MessagingException {
	// 	senderService.sendSimpleEmail("pepewee0@gmail.com",
	// 			"This is email body",
	// 			"This is email subject");

	// }

}
