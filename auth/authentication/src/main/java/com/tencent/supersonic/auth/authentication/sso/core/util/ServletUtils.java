package com.tencent.supersonic.auth.authentication.sso.core.util;


import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 客户端工具类
 *
 * @author 芋道源码
 */
public class ServletUtils {

    /**
     * 返回 JSON 字符串
     *
     * @param response 响应
     * @param object 对象，会序列化成 JSON 字符串
     */
    @SuppressWarnings("deprecation") // 必须使用 APPLICATION_JSON_UTF8_VALUE，否则会乱码
    public static void writeJSON(HttpServletResponse response, Object object){
        String content = JSONObject.toJSONString(object);
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }



}
