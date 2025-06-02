package com.tencent.supersonic.auth.authentication.sso.config;


import com.tencent.supersonic.auth.authentication.sso.core.filter.TokenAuthenticationFilter;
import com.tencent.supersonic.auth.authentication.sso.core.handler.AccessDeniedHandlerImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfiguration {

    @Resource
    private TokenAuthenticationFilter tokenAuthenticationFilter;

    @Resource
    private AccessDeniedHandlerImpl accessDeniedHandler;
    @Resource
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .authorizeHttpRequests(auth -> auth
                        // 静态资源，可匿名访问
                        .requestMatchers(HttpMethod.GET, "/*.html", "/**/*.html", "/**/*.css", "/**/*.js").permitAll()
                        // 登录相关接口，可匿名访问
                        .requestMatchers("/auth/login-by-code").permitAll()
                        .requestMatchers("/auth/refresh-token").permitAll()
                        .requestMatchers("/auth/logout").permitAll()
                        .requestMatchers("/api/auth/user/login").permitAll()
                        // 兜底规则，必须认证
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())  // 新的写法：使用 lambda 禁用 CSRF
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 添加 Token Filter
        httpSecurity.addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }

}
