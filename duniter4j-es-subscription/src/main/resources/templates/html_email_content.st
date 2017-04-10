html_email_content(issuer, senderPubkey, senderName, events, url) ::= <<
    <div class="row no-padding">
        <div class="col col-20 hidden-xs hidden-sm text-center" id="home">
            <div class="logo"></div>
        </div>
        <div class="col">
            <div class="padding padding-bottom row responsive-sm">
                <div class="col">
                    $length(events):{count | $i18n_args("duniter4j.es.subscription.email.header", [issuer, count])$}$
                </div>
                <div class="col">
                    <a class="button button-positive pull-right"
                       href="$url$">$i18n("duniter4j.es.subscription.email.openCesium")$ &gt;&gt;</a>
                </div>
            </div>
            <div class="list item-border-large">
                <div class="item item-divider stable-bg">
                    $i18n("duniter4j.es.subscription.email.notificationsDivider")$
                </div>
                $events:{e|$event_item(e)$}$
            </div>
            <div class="center padding text-center">
                <i class="ion-es-user-api"></i>
                $i18n_args("duniter4j.es.subscription.email.footer.sendBy", [{$[url, "/#/app/wot/", senderPubkey, "/"]; separator=""$}, senderName])$
                <br/>
                <small>
                    $i18n_args("duniter4j.es.subscription.email.footer.disableHelp", {$[url, "/#/app/wallet/subscriptions"]; separator=""$})$
                </small>
            </div>
        </div>
        <div class="col col-20 hidden-xs hidden-sm">&nbsp;</div>
    </div>
>>