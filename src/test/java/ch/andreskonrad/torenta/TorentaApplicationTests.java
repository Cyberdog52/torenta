package ch.andreskonrad.torenta;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "ch.andreskonrad.torenta.tmdb.service.key=test-key")
class TorentaApplicationTests {

    @Test
    void contextLoads() {
    }

}
