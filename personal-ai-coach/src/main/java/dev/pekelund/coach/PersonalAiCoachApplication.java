package dev.pekelund.coach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Personal AI Coach — Din personliga AI-coach.
 *
 * <p>En AI-baserad personlig assistent med agenter för kalender, e-post,
 * ekonomi, menyplanering, semesterplanering, todo-hantering och personlig coaching.
 */
@SpringBootApplication
public class PersonalAiCoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalAiCoachApplication.class, args);
    }
}
