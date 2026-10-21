package com.storeya.shop.service.pay;

import com.storeya.shop.utils.StringRandom;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * setCode is the only identifier a caller supplies to retry an order, so it doubles as
 * an authorization token: at "HD" + 4 characters the space was ~1.68M and enumerable.
 */
class PaymentCodeStrengthTest {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final PayService service = new PayService(null, null, null, null, null, null);

    @Test
    void orderCodesAreFourteenCharactersWithAnHDPrefix() {
        String code = service.generateCodeId();

        assertEquals(14, code.length(), () -> "unexpected code length: " + code);
        assertTrue(code.startsWith("HD"), () -> "missing HD prefix: " + code);
        assertEquals(12, randomPart(code).length(), () -> "random part too short: " + code);
    }

    @Test
    void randomPartUsesOnlyTheDeclaredAlphabet() {
        for (int i = 0; i < 500; i++) {
            String part = randomPart(service.generateCodeId());
            for (char c : part.toCharArray()) {
                assertTrue(ALPHABET.indexOf(c) >= 0,
                        () -> "character '" + c + "' is outside the code alphabet");
            }
        }
    }

    /**
     * The generator must be able to reach all 36 characters. Dropping one shrinks the
     * space silently - and this did happen while the alphabet was being edited.
     */
    @Test
    void generatorReachesEveryCharacterOfTheAlphabet() {
        Set<Character> seen = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            for (char c : StringRandom.generateStringRandom(8).toCharArray()) {
                seen.add(c);
            }
        }
        for (char expected : ALPHABET.toCharArray()) {
            assertTrue(seen.contains(expected),
                    () -> "the generator never produced '" + expected + "' - the alphabet lost a character");
        }
    }

    @Test
    void codesDoNotCollide() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            final int draw = i;
            assertTrue(codes.add(service.generateCodeId()), () -> "duplicate order code after " + draw + " draws");
        }
        assertEquals(500, codes.size());
    }

    private static String randomPart(String code) {
        return code.substring(2);
    }
}
