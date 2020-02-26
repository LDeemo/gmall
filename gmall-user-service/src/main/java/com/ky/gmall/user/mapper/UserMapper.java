package com.ky.gmall.user.mapper;

import com.ky.gmall.beans.UmsMember;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * 继承了通用mapper,增删改查单表操作交给它
 */
@Repository
public interface UserMapper extends Mapper<UmsMember> {
    List<UmsMember> findAllUser();

}
