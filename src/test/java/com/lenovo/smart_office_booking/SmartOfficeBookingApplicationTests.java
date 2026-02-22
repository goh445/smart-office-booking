package com.lenovo.smart_office_booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration"
})
class SmartOfficeBookingApplicationTests {

	@Test
	void contextLoads() {
	}

}
