package com.ky.gmall.service;



import com.ky.gmall.beans.UmsMember;
import com.ky.gmall.beans.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {
    List<UmsMember> findAllUser();

    List<UmsMemberReceiveAddress> findReceiveAddressByMemberId(String memberId);

    int saveUser(UmsMember umsMember);

    int deleteUser(String id);

    int updateUser(UmsMember umsMember);

    int deleteUserReceiveAddress(String id);

    int saveUserReceiveAddress(UmsMemberReceiveAddress umsMemberReceiveAddress);

    int updateUserReceiveAddress(UmsMemberReceiveAddress umsMemberReceiveAddress);

    UmsMember login(UmsMember umsMember);

    void addUserToken(String token, String memberId);

    void addOauthUser(UmsMember umsMember);

    UmsMember checkOauthUser(UmsMember umsCheck);
}
