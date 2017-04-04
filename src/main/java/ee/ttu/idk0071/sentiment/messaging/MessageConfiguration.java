package ee.ttu.idk0071.sentiment.messaging;

import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ee.ttu.idk0071.sentiment.lib.messages.DomainLookupRequestMessage;

@Configuration
public class MessageConfiguration {
	@Bean
	public MessageConverter jsonMessageConverter() {
		final Jackson2JsonMessageConverter converter =
				new Jackson2JsonMessageConverter();
		converter.setClassMapper(classMapper());
		return converter;
	}

	@Bean
	public DefaultClassMapper classMapper() {
		DefaultClassMapper typeMapper = new DefaultClassMapper();
		typeMapper.setDefaultType(DomainLookupRequestMessage.class);
		return typeMapper;
	}
}
