package com.ky.gmall.user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.ky.gmall.beans.UmsMember;
import com.ky.gmall.beans.UmsMemberReceiveAddress;
import com.ky.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class UserController {

    @Reference
    private UserService userService;

    @RequestMapping("index")
    @ResponseBody
    public String index(){
        return "hello user";
    }

    @RequestMapping("getAllUser")
    @ResponseBody
    public List<UmsMember> getAllUser(){
       List<UmsMember> umsMembers = userService.getAllUser();
       return umsMembers;
    }


    @RequestMapping("saveUser")
    @ResponseBody
    public int saveUser(@RequestBody UmsMember umsMember){
        int flag;
        flag = userService.saveUser(umsMember);
        return flag;
    }

    @RequestMapping("deleteUser")
    @ResponseBody
    public int deleteUser(@RequestBody String id){
        int flag;
        flag = userService.deleteUser(id);
        return flag;
    }

    @RequestMapping("updateUser")
    @ResponseBody
    public int updateUser(@RequestBody UmsMember umsMember){
        int flag;
        flag = userService.updateUser(umsMember);
        return flag;
    }

    @RequestMapping("saveUserReceiveAddress")
    @ResponseBody
    public int saveUserReceiveAddress(@RequestBody UmsMemberReceiveAddress umsMemberReceiveAddress){
        int flag;
        flag = userService.saveUserReceiveAddress(umsMemberReceiveAddress);
        return flag;
    }

    @RequestMapping("deleteUserReceiveAddress")
    @ResponseBody
    public int deleteUserReceiveAddress(@RequestBody String id){
        int flag;
        flag = userService.deleteUserReceiveAddress(id);
        return flag;
    }

    @RequestMapping("updateUserReceiveAddress")
    @ResponseBody
    public int updateUserReceiveAddress(@RequestBody UmsMemberReceiveAddress umsMemberReceiveAddress){
        int flag;
        flag = userService.updateUserReceiveAddress(umsMemberReceiveAddress);
        return flag;
    }

    @RequestMapping("getReceiveAddressByMemberId")
    @ResponseBody
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(@RequestBody String memberId){
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = userService.getReceiveAddressByMemberId(memberId);
        return umsMemberReceiveAddresses;
    }

}
