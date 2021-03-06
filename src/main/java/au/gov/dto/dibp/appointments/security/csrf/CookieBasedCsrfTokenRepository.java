package au.gov.dto.dibp.appointments.security.csrf;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Cross-site request forgery (CSRF or CSRF) protection using double submit cookies:
 * https://www.owasp.org/index.php/Cross-Site_Request_Forgery_%28CSRF%29_Prevention_Cheat_Sheet#Double_Submit_Cookies
 * <p>
 * Issues to be aware of when using double submit cookies for CSRF protection: http://security.stackexchange.com/a/61039
 * <p>
 * Some code borrowed from Pivotal Cloud Foundry under the Apache 2.0 license:
 * https://github.com/cloudfoundry/uaa/blob/41dba9d81dbdf24ede4fb9719de28b1b88b3e1b4/common/src/main/java/org/cloudfoundry/identity/uaa/web/CookieBasedCsrfTokenRepository.java
 */
@Component
public class CookieBasedCsrfTokenRepository implements CsrfTokenRepository {
    private static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";
    public static final String CSRF_COOKIE_AND_PARAMETER_NAME = "_csrf";
    private static final int COOKIE_MAX_AGE_SECONDS = 60 * 30;

    private final SecureRandom secureRandom = new SecureRandom(); // expensive to create, do it only once

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        String tokenValue = new BigInteger(130, secureRandom).toString(32); // http://stackoverflow.com/a/41156
        return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_COOKIE_AND_PARAMETER_NAME, tokenValue);
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        Cookie csrfCookie;
        if (token == null) {
            csrfCookie = new Cookie(CSRF_COOKIE_AND_PARAMETER_NAME, "");
            csrfCookie.setMaxAge(0);
        } else {
            csrfCookie = new Cookie(token.getParameterName(), token.getToken());
            csrfCookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        }
        csrfCookie.setHttpOnly(true);
        csrfCookie.setSecure(request.isSecure());
        response.addCookie(csrfCookie);
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie != null && CSRF_COOKIE_AND_PARAMETER_NAME.equals(cookie.getName())) {
                    return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_COOKIE_AND_PARAMETER_NAME, cookie.getValue());
                }
            }
        }
        return null;
    }
}
