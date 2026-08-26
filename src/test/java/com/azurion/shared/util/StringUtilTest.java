package com.azurion.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilTest {

    @Test
    @DisplayName("trimToMax: returns null when input is null")
    void trimToMax_nullInput_returnsNull() {
        assertThat(StringUtil.trimToMax(null, 10)).isNull();
    }

    @Test
    @DisplayName("trimToMax: returns trimmed text if shorter than max length")
    void trimToMax_shorter_returnsTrimmed() {
        assertThat(StringUtil.trimToMax("  hello  ", 10)).isEqualTo("hello");
    }

    @Test
    @DisplayName("trimToMax: truncates to max length when text exceeds limit")
    void trimToMax_exceedsLength_truncatesCorrectly() {
        assertThat(StringUtil.trimToMax("1234567890EXTRA", 10)).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("trimHeaderValue: removes carriage return and line breaks and trims")
    void trimHeaderValue_sanitizesNewlinesAndTruncates() {
        String input = " Header\r\nValue\nWithNewLines ";
        String result = StringUtil.trimHeaderValue(input, 15);
        assertThat(result).doesNotContain("\r").doesNotContain("\n");
        assertThat(result.length()).isLessThanOrEqualTo(15);
    }

    @Test
    @DisplayName("firstNonBlank: returns first non-blank candidate")
    void firstNonBlank_returnsFirstValidString() {
        String result = StringUtil.firstNonBlank(null, "", "   ", "validValue", "secondValue");
        assertThat(result).isEqualTo("validValue");
    }

    @Test
    @DisplayName("firstNonBlank: returns null if all candidates are null or blank")
    void firstNonBlank_allBlank_returnsNull() {
        String result = StringUtil.firstNonBlank(null, "", "   ");
        assertThat(result).isNull();
    }
}
