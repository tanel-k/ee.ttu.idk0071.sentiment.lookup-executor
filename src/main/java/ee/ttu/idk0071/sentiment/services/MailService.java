package ee.ttu.idk0071.sentiment.services;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import ee.ttu.idk0071.sentiment.services.objects.MailModel;
import it.ozimov.springboot.mail.model.Email;
import it.ozimov.springboot.mail.model.defaultimpl.DefaultEmail;
import it.ozimov.springboot.mail.service.EmailService;
import it.ozimov.springboot.mail.service.exception.CannotSendEmailException;

@Service
public class MailService {
	@Autowired
	private EmailService emailService;

	public void sendEmailTemplate(MailModel mail, String templateFile, Map<String, Object> templateContext) {
		Email email = null;
		try {
			email = DefaultEmail.builder()
				.from(new InternetAddress(mail.getSender().getAddress(), mail.getSender().getName()))
				.to(Lists.newArrayList(new InternetAddress(mail.getSender().getAddress(), null)))
				.subject(mail.getTopic()).body("").encoding("UTF-8").build();
			emailService.send(email, templateFile, templateContext);
		} catch (UnsupportedEncodingException|CannotSendEmailException e) {
			// consume failure
		}
	}
}
