package ee.ttu.idk0071.sentiment.services;

import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import ee.ttu.idk0071.sentiment.services.objects.Mail;
import it.ozimov.springboot.mail.model.Email;
import it.ozimov.springboot.mail.model.defaultimpl.DefaultEmail;
import it.ozimov.springboot.mail.service.EmailService;
import it.ozimov.springboot.mail.utils.StringUtils;

@Service
public class MailService {
	private static final String DEFAULT_MAIL_ENCODING = "UTF-8";

	@Autowired
	private EmailService emailService;

	public void sendEmailTemplate(Mail mail, String templateFile, Map<String, Object> context) {
		try {
			InternetAddress senderAddress = new InternetAddress(mail.getSender().getAddress(), mail.getSender().getName());
			InternetAddress recipientAddress = new InternetAddress(mail.getRecipient().getAddress(), null);
			
			Email email = DefaultEmail.builder()
				.from(senderAddress)
				.to(Lists.newArrayList(recipientAddress))
				.subject(mail.getSubject())
				.body(StringUtils.EMPTY)
				.encoding(DEFAULT_MAIL_ENCODING)
				.build();
			
			emailService.send(email, templateFile, context);
		} catch (Throwable t) {
			// consume failure
		}
	}
}
