package com.ky.gmall.passport.controller;

import com.ky.gmall.beans.UmsMember;
import com.sun.org.apache.regexp.internal.RE;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PassportController {

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember){
        //调用用户服务验证用户名和密码



        return "token";
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
    public String verify(String token){
        //通过JWT校验token真假

        return "success";
    }
}
