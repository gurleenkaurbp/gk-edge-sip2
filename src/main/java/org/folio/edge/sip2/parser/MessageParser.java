package org.folio.edge.sip2.parser;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.folio.edge.sip2.parser.Field.UNKNOWN;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.sip2.parser.exceptions.MissingDelimiterException;

/**
 * Base class for message parsing. Contains some common parsing methods.
 *
 * @author mreno-EBSCO
 *
 */
public abstract class MessageParser {
  private static final Logger log = LogManager.getLogger();

  protected int position;
  protected final Character delimiter;
  protected final String timezone;

  protected MessageParser(Character delimiter, String timezone) {
    this.delimiter = Objects.requireNonNull(delimiter,
      "delimiter cannot be null");
    this.timezone = Objects.requireNonNull(timezone,
      "timezone cannot be null");
  }

  protected Field parseFieldIdentifier(char [] messageChars) {
    final String fieldIdentifier = new String(new char [] {
        messageChars[position++], messageChars[position++]
    });

    final Field field = Field.find(fieldIdentifier);

    if (field == UNKNOWN) {
      log.warn("Unknown field {}", fieldIdentifier);
    }

    return field;
  }

  protected String parseVariableLengthField(char [] messageChars, Field field) {
    final int startPosition = position;
    while (position < messageChars.length
        && messageChars[position] != delimiter.charValue()) {
      position++;
    }

    if (position == messageChars.length
        && messageChars[position - 1] != delimiter.charValue()) {
      throw new MissingDelimiterException(
          "Field does not contain a valid delimiter: " + field);
    }

    return new String(messageChars, startPosition, position - startPosition);
  }

  protected OffsetDateTime parseDateTime(char [] messageChars) {
    final String dateTimeString = new String(messageChars, position, 18);
    position += 18;
    
    // if no dateTimeString in this position assume epoch date as dummy entry
    // This allows the module to work with Envisionware selfchecks
    if (dateTimeString.trim().equals("")) {
      return convertFieldToDateTime("19700101    000000");
    }
    return convertFieldToDateTime(dateTimeString);
  }

  protected Boolean parseBoolean(char [] messageChars) {
    final Boolean result;

    if (messageChars[position] == 'Y' || messageChars[position] == 'y') {
      result = TRUE;
    } else {
      result = FALSE;
    }

    position++; // increment position

    return result;
  }

  protected OffsetDateTime convertFieldToDateTime(String dateTimeString) {
    OffsetDateTime now = OffsetDateTime.now(ZoneId.of(this.timezone));
    DateTimeMapper dtMapper = new DateTimeMapper(now.getOffset());
    return dtMapper.mapDateTime(dateTimeString).withOffsetSameInstant(ZoneOffset.UTC);
  }

  protected Boolean convertFieldToBoolean(String value) {
    return Boolean.valueOf("Y".equalsIgnoreCase(value));
  }

  protected Integer convertFieldToInteger(Field field, String value) {
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException e) {
      log.error("Field {} not an number: {}, ignoring", field, value);
      return null;
    }
  }
}
