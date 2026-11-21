package com.example.auth_project.messaging;

import com.example.auth_project.config.RabbitConfig;
import com.example.auth_project.model.event.TimesheetSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimesheetEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishSubmitted(TimesheetSubmittedEvent event){
        rabbitTemplate.convertAndSend(
                RabbitConfig.TIMESHEET_EXCHANGE,
                RabbitConfig.TIMESHEET_SUBMITTED_KEY,
                event
        );
        log.info("Published timesheet submitted event: {}", event);
    }
}
