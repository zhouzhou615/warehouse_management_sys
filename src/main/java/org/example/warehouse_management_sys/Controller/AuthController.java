package org.example.warehouse_management_sys.Controller;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.DTO.LoginDTO;
import org.example.warehouse_management_sys.Entity.Operator;
import org.example.warehouse_management_sys.Service.AuthService;
import org.example.warehouse_management_sys.Utils.Result;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO) {
        Operator operator = authService.login(loginDTO);

        if (operator == null) {
            return Result.error("用户名或密码错误");
        }

        // 构建返回数据
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("operatorId", operator.getOperatorId());
        userInfo.put("operatorName", operator.getOperatorName());
        userInfo.put("department", operator.getDepartment());
        userInfo.put("phone", operator.getPhone());
        userInfo.put("email", operator.getEmail());

        Map<String, Object> result = new HashMap<>();
        result.put("userInfo", userInfo);
        result.put("token", generateToken(operator.getOperatorId())); // 简单的token生成

        return Result.success("登录成功", result);
    }

    /**
     * 修改密码
     */
    @PutMapping("/changePassword")
    public Result<Boolean> changePassword(
            @RequestParam String operatorId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        try {
            boolean success = authService.changePassword(operatorId, oldPassword, newPassword);
            return Result.success("密码修改成功", success);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return Result.error("密码修改失败");
        }
    }

    /**
     * 验证token/用户状态（用于前端验证登录状态）
     */
    @GetMapping("/validate")
    public Result<Boolean> validate(@RequestParam String operatorId) {
        boolean isValid = authService.validateOperator(operatorId);
        return Result.success(isValid ? "用户有效" : "用户无效", isValid);
    }

    /**
     * 简单的token生成（生产环境应使用JWT等更安全的方案）
     */
    private String generateToken(String operatorId) {
        return DigestUtils.md5DigestAsHex(
                (operatorId + System.currentTimeMillis()).getBytes()
        );
    }
}