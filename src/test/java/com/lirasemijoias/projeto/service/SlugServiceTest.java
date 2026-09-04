package com.lirasemijoias.projeto.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlugServiceTest {

    private final SlugService service = new SlugService();

    @Test
    void slugifyRemovesAccentsNormalizesCaseAndSeparators() {
        assertEquals("anel-coracao-d-agua", service.slugify("  Anel Coração d'Água!  "));
    }

    @Test
    void slugifyReturnsEmptyForTextWithoutLettersOrNumbers() {
        assertEquals("", service.slugify(" --- ♥ --- "));
    }
}
