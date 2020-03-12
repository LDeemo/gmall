package com.ky.gmall.user.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.ky.gmall.beans.UmsMember;
import com.ky.gmall.beans.UmsMemberReceiveAddress;
import com.ky.gmall.service.UserService;
import com.ky.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.ky.gmall.user.mapper.UserMapper;
import com.ky.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;
    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<UmsMember> getAllUser() {
        //List<UmsMember> umsMembers = userMapper.getAllUser();
        List<UmsMember> umsMembers = userMapper.selectAll();
        return umsMembers;
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);
        for (UmsMemberReceiveAddress memberReceiveAddress : umsMemberReceiveAddresses) {
            String province = memberReceiveAddress.getProvince();
            String city = memberReceiveAddress.getCity();
            String region = memberReceiveAddress.getRegion();
            String detailAddress = memberReceiveAddress.getDetailAddress();
            memberReceiveAddress.setMemberFullAddress(province+city+region+detailAddress);
        }

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

    @Override
    public UmsMember login(UmsMember umsMember) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            if (jedis != null) {
                String umsMemberStr = jedis.get("user:" + umsMember.getPassword()+umsMember.getUsername() + ":info");
                if (StringUtils.isNotBlank(umsMemberStr)) {
                    //密码正确
                    UmsMember umsMemberFromCache = JSON.parseObject(umsMemberStr, UmsMember.class);
                    return umsMemberFromCache;
                }
            }

            //需要开启数据库
            UmsMember umsMemberFromDb = loginFromDb(umsMember);
            if (umsMemberFromDb != null){
                jedis.setex("user:" + umsMember.getPassword()+umsMember.getUsername() + ":info",60*60*24,JSON.toJSONString(umsMemberFromDb));
            }
            return umsMemberFromDb;

        } finally {
            jedis.close();
        }
    }

    @Override
    public void addUserToken(String token, String memberId) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            jedis.setex("user:"+memberId+":token",60*60*2,token);

        }finally {
            jedis.close();
        }
    }

    @Override
    public UmsMember addOauthUser(UmsMember umsMember) {
        userMapper.insertSelective(umsMember);
        return  umsMember;
    }

    @Override
    public UmsMember checkOauthUser(UmsMember umsCheck) {
        UmsMember umsMember = userMapper.selectOne(umsCheck);
        return umsMember;
    }

    @Override
    public UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId) {
        UmsMemberReceiveAddress umsMemberReceiveAddressCheck = new UmsMemberReceiveAddress();
        umsMemberReceiveAddressCheck.setId(receiveAddressId);
        UmsMemberReceiveAddress umsMemberReceiveAddress = umsMemberReceiveAddressMapper.selectOne(umsMemberReceiveAddressCheck);
        return umsMemberReceiveAddress;
    }

    private UmsMember loginFromDb(UmsMember umsMember) {
        List<UmsMember> umsMembers = userMapper.select(umsMember);
        if (umsMembers != null){
            return umsMembers.get(0);
        }
        return null;
    }
}
