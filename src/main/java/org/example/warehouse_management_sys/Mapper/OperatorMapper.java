package org.example.warehouse_management_sys.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.warehouse_management_sys.Entity.Operator;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OperatorMapper {
    // 根据操作员ID或用户名和密码查询
    Operator selectByLogin(@Param("username") String username,
                           @Param("password") String password);

    // 根据ID查询
    Operator selectById(String operatorId);

    // 更新密码
    int updatePassword(@Param("operatorId") String operatorId,
                       @Param("newPassword") String newPassword);
}