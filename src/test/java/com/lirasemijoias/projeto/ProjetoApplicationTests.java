package com.lirasemijoias.projeto;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

class ProjetoApplicationTests {

	@Test
	void shouldBeInstantiable() {
		assertNotNull(new ProjetoApplication());
	}

	@Test
	void shouldDelegateMainToSpringApplication() {
		String[] args = {"--spring.main.web-application-type=none"};

		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			ProjetoApplication.main(args);

			springApplication.verify(() -> SpringApplication.run(ProjetoApplication.class, args));
		}
	}

}
