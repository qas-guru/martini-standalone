package guru.qas.martini.standalone.harness.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import guru.qas.martini.runtime.event.DefaultEventManager;
import guru.qas.martini.runtime.event.EventManager;

@Configuration
class EventManagerConfiguration {

	@Bean
	EventManager getEventManger(
		AutowireCapableBeanFactory beanFactory,
		@Value("${martini.event.manager.impl:#{null}}") Class<? extends EventManager> implementation
	) {
		return null == implementation ?
			beanFactory.createBean(DefaultEventManager.class) : beanFactory.createBean(implementation);
	}
}
