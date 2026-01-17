package org.example.warehouse_management_sys.Service;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.DTO.LoginDTO;
import org.example.warehouse_management_sys.Entity.Operator;
import org.example.warehouse_management_sys.Mapper.OperatorMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import javax.annotation.Resource;

@Slf4j
@Service
public class AuthService {

    @Resource
    private OperatorMapper operatorMapper;

    /**
     * 用户登录验证
     */
    public Operator login(LoginDTO loginDTO) {
        try {
            // 对密码进行MD5加密
            String encryptedPassword = DigestUtils.md5DigestAsHex(
                    loginDTO.getPassword().getBytes()
            );

            // 查询用户
            Operator operator = operatorMapper.selectByLogin(
                    loginDTO.getUsername(),
                    encryptedPassword
            );

            if (operator != null) {
                log.info("用户登录成功: {}", operator.getOperatorId());
                // 不返回密码
                operator.setPassword(null);
            } else {
                log.warn("登录失败: {}", loginDTO.getUsername());
            }

            return operator;
        } catch (Exception e) {
            log.error("登录异常", e);
            throw new RuntimeException("登录异常");
        }
    }

    /**
     * 修改密码
     */
    public boolean changePassword(String operatorId, String oldPassword, String newPassword) {
        Operator operator = operatorMapper.selectById(operatorId);
        if (operator == null) {
            throw new IllegalArgumentException("操作员不存在");
        }

        // 验证旧密码
        String encryptedOldPassword = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        if (!encryptedOldPassword.equals(operator.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }

        // 加密新密码
        String encryptedNewPassword = DigestUtils.md5DigestAsHex(newPassword.getBytes());

        int result = operatorMapper.updatePassword(operatorId, encryptedNewPassword);
        log.info("修改密码: {}, 结果: {}", operatorId, result > 0);
        return result > 0;
    }

    /**
     * 验证操作员是否存在且状态正常
     */
    public boolean validateOperator(String operatorId) {
        Operator operator = operatorMapper.selectById(operatorId);
        return operator != null && "在职".equals(operator.getStatus());
    }
}