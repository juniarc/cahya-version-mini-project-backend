package com.example.auth_project.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String TIMESHEET_EXCHANGE = "timesheet.exchange";
    public static final String TIMESHEET_SUBMITTED_QUEUE = "timesheet.submitted.queue";
    public static final String TIMESHEET_SUBMITTED_KEY = "timesheet.submitted";

    @Bean
    public TopicExchange timesheetExchange(){
        return  new TopicExchange(TIMESHEET_EXCHANGE, true, false);
    }

    @Bean
    public Queue timesheetSubmittedQueue(){
        return new Queue(TIMESHEET_SUBMITTED_QUEUE, true);
    }

    @Bean
    public Binding timesheetSubmittedBinding(Queue timesheetSubmittedQueue, TopicExchange timesheetExchange){
        return BindingBuilder.bind(timesheetSubmittedQueue)
                .to(timesheetExchange)
                .with(TIMESHEET_SUBMITTED_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}
