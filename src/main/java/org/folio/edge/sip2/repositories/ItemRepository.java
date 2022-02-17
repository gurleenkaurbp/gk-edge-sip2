package org.folio.edge.sip2.repositories;


import static org.folio.edge.sip2.domain.messages.enumerations.CirculationStatus.OTHER;
import static org.folio.edge.sip2.domain.messages.enumerations.SecurityMarker.OTHER;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.lang.String;
import java.time.Clock;
import java.time.OffsetDateTime;
// import java.time.ZoneOffset;
// import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.sip2.domain.messages.enumerations.CirculationStatus;
import org.folio.edge.sip2.domain.messages.enumerations.ItemStatus;
import org.folio.edge.sip2.domain.messages.enumerations.SecurityMarker;
import org.folio.edge.sip2.domain.messages.requests.ItemInformation;
import org.folio.edge.sip2.domain.messages.responses.ItemInformationResponse;
import org.folio.edge.sip2.domain.messages.responses.ItemInformationResponse.ItemInformationResponseBuilder;
import org.folio.edge.sip2.session.SessionData;
//import org.folio.edge.sip2.utils.Utils.buildQueryString;


/**
 * Provides interaction with the Items service.
 *
 * @author mreno-EBSCO
 *
 */
public class ItemRepository {
  private static final Logger log = LogManager.getLogger();
  private final IResourceProvider<IRequestData> resourceProvider;
  private Clock clock;

  @Inject
  ItemRepository(IResourceProvider<IRequestData> resourceProvider,
      Clock clock) {
    this.resourceProvider = Objects.requireNonNull(resourceProvider,
        "Resource provider cannot be null");
    this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
  }

  private Map<String, String> getBaseHeaders() {
    final Map<String, String> headers = new HashMap<>();
    headers.put("accept", "application/json");
    return headers;
  }

  private class ItemInformationRequestData implements IRequestData {

    private final String itemIdentifier;
    private final Map<String, String> headers;
    private final SessionData sessionData;

    private ItemInformationRequestData(
        String itemIdentifier,
        Map<String, String> headers,
        SessionData sessionData) {
      this.itemIdentifier = itemIdentifier;
      this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
      this.sessionData = sessionData;
    }
    
    public String getPath() {
      // Map<String,String> qsMap = new HashMap<String, String>();
      // qsMap.put("limit", "1");
      // qsMap.put("key2", "value2");
      // qsMap.put("key3", "value3");
      String uri = "/inventory/items?limit=1&query=barcode==" + itemIdentifier;
      log.info("URI: {}", () -> uri);
      return uri;
    }

    public Map<String, String> getHeaders() {
      return headers;
    }

    @Override
    public SessionData getSessionData() {
      return sessionData;
    }
  }

  private class NextHoldRequestData implements IRequestData {

    private final String itemUuid;
    private final Map<String, String> headers;
    private final SessionData sessionData;

    private NextHoldRequestData(
        String itemUuid,
        Map<String, String> headers,
        SessionData sessionData) {
      this.itemUuid = itemUuid;
      this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
      this.sessionData = sessionData;
    }
    
    public String getPath() {
      // Map<String,String> qsMap = new HashMap<String, String>();
      // qsMap.put("limit", "1");
      // qsMap.put("key2", "value2");
      // qsMap.put("key3", "value3");
      String uri = "/circulation/requests?limit=1&query=status==Open - Awaiting pickup and itemId=="
          + itemUuid;
      log.info("URI: {}", () -> uri);
      return uri;
    }

    public Map<String, String> getHeaders() {
      return headers;
    }

    @Override
    public SessionData getSessionData() {
      return sessionData;
    }
  }

  private class LoanRequestData implements IRequestData {

    private final String itemUuid;
    private final Map<String, String> headers;
    private final SessionData sessionData;

    private LoanRequestData(
        String itemUuid,
        Map<String, String> headers,
        SessionData sessionData) {
      this.itemUuid = itemUuid;
      this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
      this.sessionData = sessionData;
    }
    
    public String getPath() {
      // Map<String,String> qsMap = new HashMap<String, String>();
      // qsMap.put("limit", "1");
      // qsMap.put("key2", "value2");
      // qsMap.put("key3", "value3");
      String uri = "/circulation/loans?query=(itemId==" + itemUuid
          + " and status.name==Open) sortby status&";
      log.info("URI: {}", () -> uri);
      return uri;
    }

    public Map<String, String> getHeaders() {
      return headers;
    }

    @Override
    public SessionData getSessionData() {
      return sessionData;
    }
  }

  /**
   * Perform a itemInformation.
   *
   * @param itemInformation the itemInformation domain object
   * @return the itemInformation response domain object
   */
  public Future<ItemInformationResponse> performItemInformationCommand(
      ItemInformation itemInformation, SessionData sessionData) {
    // We'll need to convert this date properly. It is likely that it will not include timezone
    // information, so we'll need to use the tenant/SC timezone as the basis and convert to UTC.
    // final String scLocation = sessionData.getScLocation();
    final String itemIdentifier = itemInformation.getItemIdentifier();

    final Map<String, String> headers = getBaseHeaders();
    ItemInformationRequestData itemInformationRequestData =
        new ItemInformationRequestData(itemIdentifier, headers, sessionData);

    Future<IResource> itemsResult;
    itemsResult = resourceProvider
      .retrieveResource(itemInformationRequestData);

    return itemsResult
      .otherwiseEmpty()
      .compose(itemsResource -> {
        JsonObject itemsResponse = itemsResource.getResource();
        JsonObject item = itemsResponse.getJsonArray("items").getJsonObject(0);
        NextHoldRequestData nextHoldRequestData =
            new NextHoldRequestData(item.getString("id"), headers, sessionData);

        Future<IResource> nextHoldResult;
        nextHoldResult = resourceProvider.retrieveResource(nextHoldRequestData);
        
        return nextHoldResult
            .otherwiseEmpty()
            .compose(holdResource -> {
        
              final ItemInformationResponseBuilder builder = ItemInformationResponse.builder();
              log.debug("circStatus: {}", item.getJsonObject("status").getString("name"));
              log.debug("circStatusName: {}", lookupCirculationStatus(item.getJsonObject("status")
                  .getString("name")));
              builder
                  .circulationStatus(
                      lookupCirculationStatus(item.getJsonObject("status").getString("name")))
                  .securityMarker(SecurityMarker.NONE)
                  .transactionDate(OffsetDateTime.now(clock))
                  .dueDate(OffsetDateTime.now(clock))
                  .itemIdentifier(itemIdentifier)
                  .titleIdentifier(item.getString("title"))
                  .permanentLocation(item.getJsonObject("effectiveLocation").getString("name"));
              JsonObject holdResponse = holdResource.getResource();

              if (holdResponse.getJsonArray("requests").size() > 0) {
                JsonObject nextHold = holdResponse.getJsonArray("requests").getJsonObject(0);
                JsonObject holdPatron = nextHold.getJsonObject("requester");
                builder
                    .holdPatronId(holdPatron.getString("barcode"))
                    .holdPatronName(holdPatron.getString("lastName") + ", "
                      + holdPatron.getString("firstName"));
              }
              if (item.getJsonObject("status")
                  .getString("name").equals(ItemStatus.CHECKED_OUT.getValue())) {
                    LoanRequestData loanRequestData =
                        new LoanRequestData(item.getString("id"), headers, sessionData);
                    Future<IResource> loansResult = resourceProvider
                        .retrieveResource(loanRequestData);
                    //   loansResult
                    //       .otherwiseEmpty()
                    //       .map(IResource::getResource)
                    //       .compose(loansResource -> {
                    //         JsonArray loans = loansResource.getJsonArray("loans");
                    //       }); //end compose
              }
      
              return Future.succeededFuture(builder.build());
            }
          );
      }); // end compose
  }


  
  /**
   * Lookup SIP CirculationStatus by FOLIO Item Status.
   *
   * @param folioString the itemStatus string from the FOLIO JSON respons
   * @return the CirculationStatus enum item
   */
  public CirculationStatus lookupCirculationStatus(String folioString) {
    
    if (ItemStatus.AVAILABLE.getValue().equals(folioString)) {
      return CirculationStatus.AVAILABLE;
    }
    if (ItemStatus.AWAITING_PICKUP.getValue().equals(folioString)) {
      return CirculationStatus.WAITING_ON_HOLD_SHELF;
    }
    if (ItemStatus.AWAITING_DELIVERY.getValue().equals(folioString)) {
      return CirculationStatus.IN_TRANSIT_BETWEEN_LIBRARY_LOCATIONS;
    }
    if (ItemStatus.CHECKED_OUT.getValue().equals(folioString)) {
      return CirculationStatus.CHARGED;
    }
    if (ItemStatus.IN_TRANSIT.getValue().equals(folioString)) {
      return CirculationStatus.IN_TRANSIT_BETWEEN_LIBRARY_LOCATIONS;
    }
    if (ItemStatus.PAGED.getValue().equals(folioString)) {
      return CirculationStatus.RECALLED;
    }
    if (ItemStatus.ON_ORDER.getValue().equals(folioString)) {
      return CirculationStatus.ON_ORDER;
    }
    if (ItemStatus.IN_PROCESS.getValue().equals(folioString)) {
      return CirculationStatus.IN_PROCESS;
    }
    if (ItemStatus.DECLARED_LOST.getValue().equals(folioString)) {
      return CirculationStatus.LOST;
    }
    if (ItemStatus.CLAIMED_RETURNED.getValue().equals(folioString)) {
      return CirculationStatus.CLAIMED_RETURNED;
    }
    if (ItemStatus.LOST_AND_PAID.getValue().equals(folioString)) {
      return CirculationStatus.LOST;
    }
    if (ItemStatus.INTELLECTUAL_ITEM.getValue().equals(folioString)) {
      return CirculationStatus.OTHER;
    }
    if (ItemStatus.IN_PROCESS_NON_REQUESTABLE.getValue().equals(folioString)) {
      return CirculationStatus.IN_PROCESS;
    }
    if (ItemStatus.LONG_MISSING.getValue().equals(folioString)) {
      return CirculationStatus.MISSING;
    }
    if (ItemStatus.UNAVAILABLE.getValue().equals(folioString)) {
      return CirculationStatus.OTHER;
    }
    if (ItemStatus.RESTRICTED.getValue().equals(folioString)) {
      return CirculationStatus.OTHER;
    }
    if (ItemStatus.AGED_TO_LOST.getValue().equals(folioString)) {
      return CirculationStatus.LOST;
    }
    return CirculationStatus.OTHER;
  }
}
