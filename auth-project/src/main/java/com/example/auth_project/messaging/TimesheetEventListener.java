package com.example.auth_project.messaging;


import com.example.auth_project.config.RabbitConfig;
import com.example.auth_project.model.event.TimesheetSubmittedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TimesheetEventListener {
    @RabbitListener(queues = RabbitConfig.TIMESHEET_SUBMITTED_QUEUE)
    public void onTimesheetSubmitted(TimesheetSubmittedEvent event){
        log.info("Received timesheet submitted event: {}", event);
    }
}
