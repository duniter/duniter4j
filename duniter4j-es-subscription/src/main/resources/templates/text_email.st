text_email(issuer, senderPubkey, senderName, events, url) ::= <<
$length(events):{count | $i18n_args("duniter4j.es.subscription.email.header.text", [issuer, count])$}$

$i18n("duniter4j.es.subscription.email.notificationsDivider")$
$events:{e|$text_event_item(e)$}$

$i18n("duniter4j.es.subscription.email.openCesium")$ : $url$

-----------------------------------------------
$i18n_args("duniter4j.es.subscription.email.footer.sendBy.text", [{$[url, "/#/app/wot/", senderPubkey, "/"]; separator=""$}, senderName])$
$i18n_args("duniter4j.es.subscription.email.footer.disableHelp.text", {$[url, "/#/app/wallet/subscriptions"]; separator=""$})$

>>