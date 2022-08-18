<#import "lib.ftl" as lib>
<#macro supportedMessages>
 ${PackagedSupportedMessages.patronStatusRequest}<#t>
 ${PackagedSupportedMessages.checkOut}<#t>
 ${PackagedSupportedMessages.checkIn}<#t>
 ${PackagedSupportedMessages.blockPatron}<#t>
 ${PackagedSupportedMessages.scAcsStatus}<#t>
 ${PackagedSupportedMessages.requestScAcsResend}<#t>
 ${PackagedSupportedMessages.login}<#t>
 ${PackagedSupportedMessages.patronInformation}<#t>
 ${PackagedSupportedMessages.endPatronSession}<#t>
 ${PackagedSupportedMessages.feePaid}<#t>
 ${PackagedSupportedMessages.itemInformation}<#t>
 ${PackagedSupportedMessages.itemStatusUpdate}<#t>
 ${PackagedSupportedMessages.patronEnable}<#t>
 ${PackagedSupportedMessages.hold}<#t>
 ${PackagedSupportedMessages.renew}<#t>
 ${PackagedSupportedMessages.renewAll}<#t>
</#macro>
98<#rt>
${ACSStatus.onLineStatus}<#rt>
${ACSStatus.checkinOk}<#rt>
${ACSStatus.checkoutOk}<#rt>
${ACSStatus.acsRenewalPolicy}<#rt>
${ACSStatus.statusUpdateOk}<#rt>
${ACSStatus.offLineOk}<#rt>
${ACSStatus.timeoutPeriod?string("000")}<#rt>
${ACSStatus.retriesAllowed?string("000")}<#rt>
${formatDateTime(ACSStatus.dateTimeSync, "yyyyMMdd    HHmmss", timezone)}<#t>
${ACSStatus.protocolVersion}<#rt>
AO${ACSStatus.institutionId}|<#rt>
<@lib.acsLibraryName value=ACSStatus.libraryName!""/>
BX<@supportedMessages />|<#rt>
<@lib.acsTerminalLocation value=ACSStatus.terminalLocation!""/>
<#-- screen message: variable-length optional field -->
<@lib.screenMessage value=ACSStatus.screenMessage!""/>
<#-- screen message: variable-length optional field -->
<@lib.printLine value=ACSStatus.printLine!""/>
