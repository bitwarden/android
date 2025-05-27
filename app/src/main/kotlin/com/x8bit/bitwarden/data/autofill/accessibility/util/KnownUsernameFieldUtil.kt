package com.x8bit.bitwarden.data.autofill.accessibility.util

import com.x8bit.bitwarden.data.autofill.accessibility.model.AccessOptions
import com.x8bit.bitwarden.data.autofill.accessibility.model.KnownUsernameField

/**
 * Determines if the [String] receiver is a uri authority for a known username field and returns
 * that [KnownUsernameField] if it is a match.
 */
fun String.getKnownUsernameFieldNull(): KnownUsernameField? =
    LEGACY_KNOWN_USERNAME_FIELDS.find { it.uriAuthority == this@getKnownUsernameFieldNull }

/**
 * A list of known username fields and their IDs.
 */
private val LEGACY_KNOWN_USERNAME_FIELDS: List<KnownUsernameField> = listOf(
    // SECTION A ——— World-renowned web sites/applications
    KnownUsernameField(
        uriAuthority = "amazon.ae",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.ca",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.cn",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.co.jp",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.co.uk",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.com",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.com.au",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.com.br",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.com.mx",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.com.tr",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.de",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.es",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.fr",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.in",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.it",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.nl",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.pl",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.sa",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.se",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "amazon.sg",
        accessOption = AccessOptions(
            matchValue = "/ap/signin",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewIds = listOf("ap_email_login", "ap_email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.aws.amazon.com",
        accessOption = AccessOptions(matchValue = "signin", usernameViewId = "resolving_input"),
    ),
    KnownUsernameField(
        uriAuthority = "id.atlassian.com",
        accessOption = AccessOptions(matchValue = "login", usernameViewId = "username"),
    ),
    KnownUsernameField(
        uriAuthority = "bitly.com",
        accessOption = AccessOptions(matchValue = "/sso/url_slug", usernameViewId = "url_slug"),
    ),
    KnownUsernameField(
        uriAuthority = "signin.befr.ebay.be",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.benl.ebay.be",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.cafr.ebay.ca",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.at",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.be",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.ca",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.ch",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.co.uk",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.com",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.com.au",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.com.hk",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.com.my",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.com.sg",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.de",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.es",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.fr",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.ie",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.in",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.it",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.nl",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.ph",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "signin.ebay.pl",
        accessOptions = listOf(
            AccessOptions(
                matchValue = "eBayISAPI.dll",
                matchingStrategy = AccessOptions.MatchingStrategy.ENDS_WITH_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
            AccessOptions(
                matchValue = "/signin/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_INSENSITIVE,
                usernameViewId = "userid",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "accounts.google.com",
        accessOptions = listOf(
            AccessOptions(matchValue = "identifier", usernameViewId = "identifierId"),
            AccessOptions(matchValue = "ServiceLogin", usernameViewId = "Email"),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "paypal.com",
        accessOptions = listOf(
            AccessOptions(matchValue = "signin", usernameViewId = "email"),
            AccessOptions(
                matchValue = "/connect/",
                matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
                usernameViewId = "email",
            ),
        ),
    ),
    KnownUsernameField(
        uriAuthority = "tumblr.com",
        accessOption = AccessOptions(
            matchValue = "login",
            usernameViewId = "signup_determine_email",
        ),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.az",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.by",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.co.il",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.com",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.com.am",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.com.ge",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.com.tr",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.ee",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.fi",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.fr",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.kg",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.kz",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.lt",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.lv",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.md",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.pl",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.ru",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.tj",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.tm",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.ua",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    KnownUsernameField(
        uriAuthority = "passport.yandex.uz",
        accessOption = AccessOptions(matchValue = "auth", usernameViewId = "passp-field-login"),
    ),
    // SECTION B ——— Top 100 worldwide
    // As of July 2020, all entries that needed to be added from
    // Top 100 (SimilarWeb, 2019) and Top 50 (Alexa Internet, 2020)
    // matched section A.
    // Therefore, no entry currently.

    // SECTION C ——— Top 20 for selected countries
    // For these selected countries, the Top 20 (SimilarWeb, 2020)
    // and the Top 20 (Alexa Internet, 2020) are covered.
    // Mobile and desktop versions supported.
    // Could not be added, however:
    // web sites/applications that don't use an "id" attribute for their login field.

    KnownUsernameField(
        uriAuthority = "cfg.smt.docomo.ne.jp",
        accessOption = AccessOptions(
            matchValue = "/auth/",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewId = "Di_Uid",
        ),
    ),
    KnownUsernameField(
        uriAuthority = "id.smt.docomo.ne.jp",
        accessOption = AccessOptions(
            matchValue = "/cgi7/",
            matchingStrategy = AccessOptions.MatchingStrategy.CONTAINS_CASE_SENSITIVE,
            usernameViewId = "Di_Uid",
        ),
    ),
    // SECTION D ——— Miscellaneous
    // No entry, currently.

    // SECTION Z ——— Special forms
    // Despite "user ID + password" fields both visible, detection rules required.
    // No entry, currently.

    // Test/example purposes only
    // GitHub is a VERY special case (signup form, just to test the proper functioning
    // of special forms).
    KnownUsernameField(
        uriAuthority = "github.com",
        accessOption = AccessOptions(matchValue = "", usernameViewId = "user[login]-footer"),
    ),
)
