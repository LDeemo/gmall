package com.ky.gmall.interceptors;

import com.ky.gmall.annotations.LoginRequired;
import com.ky.gmall.util.CookieUtil;
import com.ky.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    /**
     * 拦截
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断被拦截请求的访问方法的注解(是否是需要拦截的)
        HandlerMethod hm = (HandlerMethod)handler;
        //通过反射得到方法的注解
        LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);

        //是否拦截
        if (methodAnnotation == null){
            //为空,没有拦截注解,直接放行
            return true;
        }

        //获取token并且判断token四个分支
        String token = getToken(request);

        //已经拦截,判断该请求是否必须登陆成功
        boolean loginSuccess = methodAnnotation.loginSuccess();
        //调用认证中心进行验证
        String success = "fail";
        if (StringUtils.isNotBlank(token)){
            success = HttpclientUtil.doGet("http://passport.gmall.com:8085/verify?token="+token);
        }

        if (loginSuccess){
            //必须登录成功才能使用
            if (!success.equals("success")){
                //重定向回passport登录
                StringBuffer requestURL = request.getRequestURL();
                response.sendRedirect("http://passport.gmall.com:8085/index?ReturnUrl="+requestURL);
                return false;
            }
            //需要将token携带的用户信息写入
            request.setAttribute("memberId","1");
            request.setAttribute("nickName","test李华");
        }else{
            //没有登录,但是必须验证
            if (success.equals("success")){
                //需要将token携带的用户信息写入
                request.setAttribute("memberId","1");
                request.setAttribute("nickName","test李华");
            }
        }

        //验证通过,覆盖cookie中的token
        if (StringUtils.isNotBlank(token)){
            CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
        }
        return true;
    }

    public String getToken(HttpServletRequest request){
        String token = "";

        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true); //浏览器中缓存的token
        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }

        String newToken = request.getParameter("token"); //请求中刚过来的token
        if (StringUtils.isNotBlank(newToken)){
            token = newToken;
        }

        return token;
    }
}