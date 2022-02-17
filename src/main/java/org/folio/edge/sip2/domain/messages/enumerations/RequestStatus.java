package org.folio.edge.sip2.domain.messages.enumerations;

import java.util.Arrays;

/**
 * Defined request statuses.
 *
 * @author mreno-EBSCO
 *
 */
public enum RequestStatus {
  NONE(""),
  OPEN_NOT_YET_FILLED("Open - Not yet filled"),
  OPEN_AWAITING_PICKUP("Open - Awaiting pickup"),
  OPEN_IN_TRANSIT("Open - In transit"),
  OPEN_AWAITING_DELIVERY("Open - Awaiting delivery"),
  CLOSED_FILLED("Closed - Filled"),
  CLOSED_CANCELLED("Closed - Cancelled"),
  CLOSED_UNFILLED("Closed - Unfilled"),
  CLOSED_PICKUP_EXPIRED("Closed - Pickup expired");

  /**
   * Lookup SIP CirculationStatus by FOLIO Item Status.
   *
   * @param value the itemStatus string from the FOLIO JSON respons
   * @return the CirculationStatus enum item
   */
  public static RequestStatus from(String value) {
    return Arrays.stream(values())
      .filter(status -> status.valueMatches(value))
      .findFirst()
      .orElse(NONE);
  }

  private final String value;

  RequestStatus(String value) {
    this.value = value;
  }

  /**
   * Return enum value.
   *
   * @return the ItemStatus String value
   */
  public String getValue() {
    return value;
  }

  /**
   * Compare values of a String and Enum contents.
   *
   * @param value the string 
   * @return the answer
   */
  private boolean valueMatches(String value) {
    return getValue().equalsIgnoreCase(value);
  }
}
