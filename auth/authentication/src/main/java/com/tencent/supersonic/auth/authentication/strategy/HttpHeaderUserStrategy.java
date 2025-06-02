package com.tencent.supersonic.auth.authentication.strategy;

import com.tencent.supersonic.auth.api.authentication.constant.UserConstants;
import com.tencent.supersonic.auth.api.authentication.service.UserStrategy;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.persistence.repository.UserRepository;
import com.tencent.supersonic.auth.authentication.sso.core.LoginUser;
import com.tencent.supersonic.auth.authentication.sso.core.util.SecurityUtils;
import com.tencent.supersonic.auth.authentication.utils.TokenService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.ContextUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class HttpHeaderUserStrategy implements UserStrategy {

    public static final String STRATEGY_NAME = "http";
    private final TokenService tokenService;

    public HttpHeaderUserStrategy(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public boolean accept(boolean isEnableAuthentication) {
        return isEnableAuthentication;
    }

    @Override
    public User findUser(HttpServletRequest request, HttpServletResponse response) {
        return getUser(request);
    }

    @Override
    public User findUser(String token, String appKey) {
        return getUser(token, appKey);
    }

    public User getUser(HttpServletRequest request) {
        final Optional<Claims> claimsOptional = tokenService.getClaims(request);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (claimsOptional.isEmpty() && loginUser != null) {
            return Optional.ofNullable(loginUser.getUser()).orElse(User.getVisitUser());
        }
        return claimsOptional.map(this::getUser).orElse(User.getVisitUser());
    }

    public User getUser(String token, String appKey) {
        // 先从应用自身token找， 找不到从单点登录的上下文找
        final Optional<Claims> claimsOptional = tokenService.getClaims(token, appKey);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (claimsOptional.isEmpty() && loginUser != null) {
            return Optional.ofNullable(loginUser.getUser()).orElse(User.getVisitUser());
        }
        return claimsOptional.map(this::getUser).orElse(User.getVisitUser());
    }

    private User getUser(Claims claims) {
        Long userId =
                Long.parseLong(claims.getOrDefault(UserConstants.TOKEN_USER_ID, 0).toString());
        String userName = String.valueOf(claims.get(UserConstants.TOKEN_USER_NAME));
        String email = String.valueOf(claims.get(UserConstants.TOKEN_USER_EMAIL));
        String displayName = String.valueOf(claims.get(UserConstants.TOKEN_USER_DISPLAY_NAME));
        Integer isAdmin = claims.get(UserConstants.TOKEN_IS_ADMIN) == null ? 0
                : Integer.parseInt(claims.get(UserConstants.TOKEN_IS_ADMIN).toString());
        return User.get(userId, userName, displayName, email, isAdmin);
    }

}
