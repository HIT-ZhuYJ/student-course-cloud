package com.yun.studentcourse.student.mapper;

import com.yun.studentcourse.student.entity.UserAccount;
import com.yun.studentcourse.common.RoleEnum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAccountMapper {

    int insert(UserAccount userAccount);

    UserAccount findByUsername(@Param("username") String username);

    int updateStatusByRoleAndRelatedId(
            @Param("role") RoleEnum role,
            @Param("relatedId") Long relatedId,
            @Param("status") String status
    );
}
