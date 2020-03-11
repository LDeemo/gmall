package com.ky.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.ky.gmall.beans.UmsMember;
import com.ky.gmall.service.UserService;
import com.ky.gmall.util.HttpclientUtil;
import com.ky.gmall.util.JwtUtil;
import com.ky.gmall.util.MD5Utils;
import com.sun.org.apache.regexp.internal.RE;
import jdk.nashorn.internal.parser.Token;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证中心只负责token的颁发或者验证,其他概不负责
 */
@Controller
public class PassportController {

    @Reference
    UserService userService;

    @RequestMapping("vlogin")
    public String vlogin(HttpServletRequest request, String code, ModelMap map){
        //授权码换取access_token
        String s3 = "https://api.weibo.com/oauth2/access_token?";
        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("client_id","414771847");
        paramMap.put("client_secret","cc7b112953664bac9075e8eff776b802");
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri","http://passport.gmall.com:8085/vlogin");
        paramMap.put("code",code);
        String access_token_json = HttpclientUtil.doPost(s3,paramMap);
        Map<String,Object> access_map = JSON.parseObject(access_token_json, Map.class);

        //access_token换取用户信息
        String uid = (String) access_map.get("uid");
        String access_token = (String) access_map.get("access_token");
        String show_user_url = "https://api.weibo.com/2/users/show.json?access_token="+access_token+"&uid="+uid;
        String user_json = HttpclientUtil.doGet(show_user_url);
        Map<String,Object> user_map = JSON.parseObject(user_json, Map.class);

        //将用户信息存入数据库,用户类型设置成微博用户
        UmsMember umsMember = new UmsMember();
        umsMember.setSourceUid((Long)user_map.get("id"));
        umsMember.setSourceType(2);
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(access_token);
        umsMember.setNickname((String) user_map.get("screen_name"));
        umsMember.setUsername((String) user_map.get("name"));
        umsMember.setCity((String) user_map.get("location"));
        String gender = (String) user_map.get("gender");
        if(gender.equals("m") || gender.equals("男")){
            umsMember.setGender(1);
        }else {
            umsMember.setGender(0);
        }

        UmsMember umsCheck = new UmsMember();
        umsCheck.setSourceUid(umsMember.getSourceUid());
        UmsMember umsMemberCheck = userService.checkOauthUser(umsCheck); //检查该用户(社交用户)以前是否登陆过系统
        if (umsMemberCheck == null){
            umsMember = userService.addOauthUser(umsMember);
        }else {
            umsMember = umsMemberCheck;
        }

        //生成jwt的token,并且重定向到首页,携带该token
        String memberId = umsMember.getId(); //rpc的主键返回策略失效
        String nickname = umsCheck.getNickname();
        String token = createToken(request, memberId, nickname);
        userService.addUserToken(token,memberId);

        return "redirect:http://search.gmall.com:8083/index?token=" + token;
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request){
        String token = "";

        //调用用户服务验证用户名和密码,颁发token
        UmsMember umsMemberLogin = userService.login(umsMember);
        if (umsMemberLogin != null){
            //登录成功
            //用jwt制作token
            String memberId = umsMemberLogin.getId();
            String nickname = umsMemberLogin.getNickname();
            token = createToken(request,memberId,nickname);
            //将token存入redis一份
            userService.addUserToken(token,memberId);

        }else {
            //登录失败
            token = "fail";
        }

        return token;
    }

    private String createToken(HttpServletRequest request, String memberId, String nickname) {
        Map<String,Object> userMap = new HashMap<>();
        userMap.put("memberId",memberId);
        userMap.put("nickname",nickname);

        String ip = request.getHeader("x-forwarded-for");
        if (StringUtils.isBlank(ip)){
            ip = request.getRemoteAddr();//从request中获取ip
            if (StringUtils.isBlank(ip)){
                ip = "192.168.1.118";
            }
        }
        ip = MD5Utils.toMD5(ip); //MD5加密

        String token = JwtUtil.encode("2020gmallky", userMap, ip);
        return token;
    }

    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap map){
        if (StringUtils.isNotBlank(ReturnUrl)){
            map.put("ReturnUrl", ReturnUrl);
        }
        return "index";
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token,String currentIp){
        //通过JWT校验token真假
        Map<String,String> map = new HashMap<>();

        Map<String, Object> decode = JwtUtil.decode(token, "2020gmallky",currentIp);
        if (decode != null){
            map.put("status","success");
            map.put("memberId", (String) decode.get("memberId"));
            map.put("nickname", (String) decode.get("nickname"));
        }else {
            map.put("status","fail");
        }

        return JSON.toJSONString(map);
    }
}
