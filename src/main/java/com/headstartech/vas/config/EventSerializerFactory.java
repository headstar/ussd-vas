package com.headstartech.vas.config;

import org.mobicents.ussdgateway.EventsSerializeFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Per Johansson
 */
@Configuration
public class EventSerializerFactory {

    @Bean
    public EventsSerializeFactory eventsSerializeFactory() {
        return new EventsSerializeFactory();
    }
}
