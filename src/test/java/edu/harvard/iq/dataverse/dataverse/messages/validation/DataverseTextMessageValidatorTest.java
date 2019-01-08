package edu.harvard.iq.dataverse.dataverse.messages.validation;

import edu.harvard.iq.dataverse.dataverse.messages.dto.DataverseTextMessageDto;
import edu.harvard.iq.dataverse.util.DataverseClock;
import edu.harvard.iq.dataverse.util.DateUtil;
import edu.harvard.iq.dataverse.validation.ValidationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;

import static edu.harvard.iq.dataverse.dataverse.messages.validation.DataverseTextMessageValidator.validateEndDate;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DataverseTextMessageValidatorTest {

    LocalDateTime now = LocalDateTime.of(2018, 10, 12, 13, 15, 0);

    @Before
    public void setUp() {
        DataverseClock.fixedAt(now);
    }

    @After
    public void clear() {
        DataverseClock.reset();
    }

    @Test
    public void shouldValidateEndDateMustBeAFutureDateAndFailedForCurrentDate() {
        // given
        DataverseTextMessageDto dto = new DataverseTextMessageDto();
        dto.setFromTime(DateUtil.convertToDate(now.minusDays(1)));
        dto.setToTime(DateUtil.convertToDate(now));

        // when
        try {
            validateEndDate(dto);
            fail("EndDateMustBeAFutureDate validation exception should occured!");
        } catch (ValidationException ex) {
            // then
            assertTrue(ex instanceof EndDateMustBeAFutureDate);
        }
    }

    @Test
    public void shouldValidateEndDateMustBeAFutureDateAndFailedForPastDate() {
        // given
        DataverseTextMessageDto dto = new DataverseTextMessageDto();
        dto.setFromTime(DateUtil.convertToDate(now.minusDays(2)));
        dto.setToTime(DateUtil.convertToDate(now.minusDays(1)));

        // when
        try {
            validateEndDate(dto);
            fail("EndDateMustBeAFutureDate validation exception should occured!");
        } catch (ValidationException ex) {
            // then
            assertTrue(ex instanceof EndDateMustBeAFutureDate);
        }
    }

    @Test
    public void shouldValidateEndDateMustBeAFutureDateAndNotFailed() {
        // given
        DataverseTextMessageDto dto = new DataverseTextMessageDto();
        dto.setToTime(DateUtil.convertToDate(now.plusDays(1)));

        // expect
        validateEndDate(dto);
    }

    @Test
    public void shouldValidateEndDateMustNotBeEarlierThanStartingDateAndFailedForEarlierEndDate() {
        // given
        DataverseTextMessageDto dto = new DataverseTextMessageDto();
        dto.setFromTime(DateUtil.convertToDate(now.plusDays(5)));
        dto.setToTime(DateUtil.convertToDate(now.plusDays(4)));

        // when
        try {
            validateEndDate(dto);
            fail("EndDateMustNotBeEarlierThanStartingDate validation exception should occured!");
        } catch (ValidationException ex) {
            // then
            assertTrue(ex instanceof EndDateMustNotBeEarlierThanStartingDate);
        }
    }

    @Test
    public void shouldValidateEndDateMustNotBeEarlierThanStartingDateAndNotFailed() {
        // given
        DataverseTextMessageDto dto = new DataverseTextMessageDto();
        dto.setFromTime(DateUtil.convertToDate(now.minusDays(1)));
        dto.setToTime(DateUtil.convertToDate(now.plusDays(1)));

        // when
        validateEndDate(dto);
    }

    @Test
    public void shouldValidateEndDateMustNotBeEarlierThanStartingDateAndNotFailedForEqualDates() {
        // given
        DataverseTextMessageDto dto = new DataverseTextMessageDto();
        dto.setFromTime(DateUtil.convertToDate(now.plusDays(1)));
        dto.setToTime(DateUtil.convertToDate(now.plusDays(1)));

        // when
        validateEndDate(dto);
    }

}