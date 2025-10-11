package mg.rivolink;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void appHasAGreeting() {
        assertNotNull(new Main().getClass(), "NO class error.");
    }

}
