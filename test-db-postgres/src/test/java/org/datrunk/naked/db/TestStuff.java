package org.datrunk.naked.db;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = { TestStuff.Config.class })
@ExtendWith({ SpringExtension.class })
@ActiveProfiles("test")
public class TestStuff {
	static class Config {
		
	}
	
    @Test
    @Transactional
    public void test() {
    }
}