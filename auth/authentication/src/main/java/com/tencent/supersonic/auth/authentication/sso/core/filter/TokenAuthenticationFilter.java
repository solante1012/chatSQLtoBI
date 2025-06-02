package com.tencent.supersonic.auth.authentication.sso.core.filter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.auth.authentication.persistence.repository.UserRepository;
import com.tencent.supersonic.auth.authentication.sso.client.OAuth2Client;
import com.tencent.supersonic.auth.authentication.sso.client.UserClient;
import com.tencent.supersonic.auth.authentication.sso.core.LoginUser;
import com.tencent.supersonic.auth.authentication.sso.core.util.SecurityUtils;
import com.tencent.supersonic.auth.authentication.sso.dto.CommonResult;
import com.tencent.supersonic.auth.authentication.sso.dto.oauth2.OAuth2CheckTokenRespDTO;
import com.tencent.supersonic.auth.authentication.sso.dto.user.UserInfoRespDTO;
import com.tencent.supersonic.auth.authentication.utils.ComponentFactory;
import com.tencent.supersonic.auth.authentication.utils.TokenService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.ReturnCode;
import com.tencent.supersonic.common.util.ContextUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenAuthenticationFilter.class);
    private static final int CACHE_EXPIRE_SECONDS = 30;

    @Resource
    private OAuth2Client oauth2Client;
    @Resource
    private UserClient userClient;

    protected final LoadingCache<String, LoginUser> userCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
            .build(new CacheLoader<String, LoginUser>() {
                @Override
                public LoginUser load(String token) {
                    return buildLoginUser(token);
                }
            });

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = SecurityUtils.obtainAuthorization(request, "Authorization");
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            LoginUser loginUser = userCache.get(token);
            if (loginUser != null && loginUser.getUser() != null) {
                SecurityUtils.setLoginUser(loginUser, request);
            }
        } catch (Exception e) {
            LOGGER.warn("Get login user from cache failed : {}", e.getMessage());
            handleTokenWithoutCache(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private void handleTokenWithoutCache(String token, HttpServletRequest request) {
        LoginUser loginUser = buildLoginUser(token);
        if (loginUser != null) {
            SecurityUtils.setLoginUser(loginUser, request);
            userCache.put(token, loginUser);
        } else {
            tryLoginFromSubApp(token, request);
        }
    }

    private void tryLoginFromSubApp(String token, HttpServletRequest request) {
        TokenService tokenService = ContextUtils.getBean(TokenService.class);
        User user = UserHolder.findUser(token, tokenService.getAppKey(request));
        if (user != null) {
            LoginUser loginUser = new LoginUser().setUser(user);
            SecurityUtils.setLoginUser(loginUser, request);
            userCache.put(token, loginUser);
        }
    }

    private LoginUser buildLoginUser(String token) {
        try {
            LoginUser loginUser = buildLoginUserByToken(token);
            if (loginUser != null && loginUser.getUser() == null) {
                loginUser.setUser(buildUserFromAuthId(loginUser.getId()));
            }
            return loginUser;
        } catch (Exception e) {
            LOGGER.warn("Build login user failed for token: {}", token, e);
            return null;
        }
    }

    private User buildUserFromAuthId(Long authUserId) {
        UserRepository userRepository = ContextUtils.getBean(UserRepository.class);
        return Optional.ofNullable(userRepository.getUserByAuthId(authUserId))
                .map(userDO -> {
                    User user = new User();
                    BeanUtils.copyProperties(userDO, user);
                    return user;
                })
                .orElseGet(() -> registerNewUser(authUserId));
    }

    private User registerNewUser(Long authUserId) {
        UserInfoRespDTO userInfo = Optional.ofNullable(userClient.getUser())
                .map(CommonResult::getData)
                .orElseThrow(() -> new IllegalStateException("Failed to get user info"));

        UserReq userReq = new UserReq();
        userReq.setName(userInfo.getUsername());
        userReq.setPassword("123456");
        userReq.setAuthUserId(authUserId);

        ComponentFactory.getUserAdaptor().register(userReq);

        User user = new User();
        BeanUtils.copyProperties(userReq, user);
        return user;
    }


    private LoginUser buildLoginUserByToken(String token) {
        try {
            CommonResult<OAuth2CheckTokenRespDTO> result = oauth2Client.checkToken(token);
            if (result == null || ReturnCode.SUCCESS.getCode() != result.getCode() || result.getData() == null) {
                LOGGER.debug("Invalid token response for token: {}", token);
                return null;
            }

            OAuth2CheckTokenRespDTO accessToken = result.getData();
            return new LoginUser()
                    .setId(accessToken.getUserId())
                    .setUserType(accessToken.getUserType())
                    .setTenantId(accessToken.getTenantId())
                    .setScopes(accessToken.getScopes())
                    .setAccessToken(accessToken.getAccessToken());
        } catch (Exception e) {
            // 校验 Token 不通过时，考虑到一些接口是无需登录的，所以直接返回 null 即可
            LOGGER.debug("Token verification failed", e);
            return null;
        }
    }
}