package com.ky.gmall.user.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.ky.gmall.beans.UmsMember;
import com.ky.gmall.beans.UmsMemberReceiveAddress;
import com.ky.gmall.service.UserService;
import com.ky.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.ky.gmall.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Override
    public List<UmsMember> findAllUser() {
        //List<UmsMember> umsMembers = userMapper.findAllUser();
        List<UmsMember> umsMembers = userMapper.selectAll();
        return umsMembers;
    }

    @Override
    public List<UmsMemberReceiveAddress> findReceiveAddressByMemberId(String memberId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);

        return umsMemberReceiveAddresses;
    }

    @Override
    public int saveUser(UmsMember umsMember) {
        return userMapper.insert(umsMember);
    }

    @Override
    public int deleteUser(String id) {
        UmsMember umsMember = new UmsMember();
        umsMember.setId(id);
        return userMapper.delete(umsMember);
    }

    @Override
    public int updateUser(UmsMember umsMember) {
        return userMapper.updateByPrimaryKey(umsMember);
    }

    @Override
    public int deleteUserReceiveAddress(String id) {
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setId(id);
        return umsMemberReceiveAddressMapper.delete(umsMemberReceiveAddress);
    }

    @Override
    public int saveUserReceiveAddress(UmsMemberReceiveAddress umsMemberReceiveAddress) {
        return umsMemberReceiveAddressMapper.insert(umsMemberReceiveAddress);
    }


    @Override
    public int updateUserReceiveAddress(UmsMemberReceiveAddress umsMemberReceiveAddress) {
        return umsMemberReceiveAddressMapper.updateByPrimaryKey(umsMemberReceiveAddress);
    }
}
