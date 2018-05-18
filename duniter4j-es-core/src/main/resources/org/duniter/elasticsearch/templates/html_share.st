html_share(type, title, summary, description, image, siteName, locale, url, redirectMessage) ::= <<
<html prefix="og: http://ogp.me/ns#">
    <head>
        <meta charset="UTF-8">

    $if(siteName)$
        <title>$siteName$ | $title$</title>
    $else$
        <title>$title$</title>
    $endif$

    $if(type)$
        <meta property="og:type" content="$type$" />
    $else$
        <meta property="og:type" content="website" />
    $endif$

        <meta property="og:title" content="$title$" />

    $if(summary)$
        <meta property="og:description" content="$summary$" />
    $else$
        <meta property="og:description" content="$description$" />
    $endif$

    $if(siteName)$
        <meta property="og:site_name" content="$siteName$" />
    $endif$

    $if(image)$
        <meta property="og:image" content="$image$" />
    $endif$

    $if(locale)$
        <meta property="og:locale" content="$locale$" />
    $endif$

    $if(url)$
        <meta property="og:url" content="$url$" />
        <script type="text/javascript">
            window.location.href = "$url$"
        </script>
        <META HTTP-EQUIV="Refresh" CONTENT="0; URL=$url$">
    $endif$
    </head>
    <body>
        $if(image)$
        <p>
            <img src="$image$"/>
        </p>
        $endif$

        <h1>$title$</h1>

        <p>$description$</p>

        $if(url)$
        <p>
            $redirectMessage$ <a href='$url$'>$title$</a>.
        </p>
        $endif$
    </body>
</html>
>>
