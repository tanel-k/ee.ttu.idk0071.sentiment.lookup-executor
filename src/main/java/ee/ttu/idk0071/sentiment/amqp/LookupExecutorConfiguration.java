package ee.ttu.idk0071.sentiment.amqp;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LookupExecutorConfiguration extends MessageConfiguration {
	protected final String lookupQueue = "lookup-request-queue";

//	@Autowired
//	private LookupExecutor lookupExecutor;

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setRoutingKey(this.lookupQueue);
		template.setQueue(this.lookupQueue);
		template.setMessageConverter(jsonMessageConverter());
		return template;
	}

	@Bean
	Queue lookupQueue() {
		return new Queue(this.lookupQueue, true);
	}

	@Bean
	public SimpleMessageListenerContainer listenerContainer(ConnectionFactory connectionFactory) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setQueueNames(this.lookupQueue);
		container.setMessageListener(messageListenerAdapter());
		return container;
	}

	@Bean
	public MessageListenerAdapter messageListenerAdapter() {
		// testing
		return new MessageListenerAdapter(LookupExecutor.class, jsonMessageConverter());
	}
}
