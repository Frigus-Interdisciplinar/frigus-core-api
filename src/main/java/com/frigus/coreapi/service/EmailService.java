package com.frigus.coreapi.service;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class EmailService {
 private final JavaMailSender sender;
 @Value("${app.mail.from}") private String from;
 @Value("${app.frontend-url}") private String frontendUrl;
 public void sendLink(String to,String subject,String path,String token){
  var mail=new SimpleMailMessage();mail.setFrom(from);mail.setTo(to);mail.setSubject(subject);
  mail.setText("Acesse o link para continuar: "+frontendUrl.replaceAll("/$","")+path+"?token="+token+"\nSe você não solicitou esta ação, ignore este e-mail.");sender.send(mail);
 }
}
