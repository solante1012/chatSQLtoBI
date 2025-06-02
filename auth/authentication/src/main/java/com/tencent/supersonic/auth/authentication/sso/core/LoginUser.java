package com.tencent.supersonic.auth.authentication.sso.core;

import com.tencent.supersonic.common.pojo.User;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 登录用户信息
 *
 * @author 芋道源码
 */
@Data
@Accessors(chain = true)
public class LoginUser {

    /**
     * 用户编号
     */
    private Long id;
    /**
     * 用户类型
     */
    private Integer userType;
    /**
     * 租户编号
     */
    private Long tenantId;
    /**
     * 授权范围
     */
    private List<String> scopes;

    /**
     * 访问令牌
     */
    private String accessToken;

    // 子应用用户
    private User user;
}
