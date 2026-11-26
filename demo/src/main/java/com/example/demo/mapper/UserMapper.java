package com.example.demo.mapper;

import com.example.demo.UserAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface UserMapper {

	@Select("SELECT id, username, password, role, enabled, updated_at FROM users WHERE username = #{username}")
	UserAccount findByUsername(@Param("username") String username);

	@Select("SELECT id, username, role, enabled, updated_at FROM users ORDER BY username")
	List<UserAccount> findAll();

	@Select("SELECT id, username, role, enabled, updated_at FROM users WHERE id = #{id}")
	UserAccount findById(@Param("id") Long id);

	@Insert("INSERT INTO users (username, password, role, enabled) VALUES (#{username}, #{password}, #{role}, #{enabled})")
	int insert(UserAccount user);

	@Update("UPDATE users SET role = #{role}, enabled = #{enabled}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
	int updateAdminFields(UserAccount user);

	@Update("UPDATE users SET role = 'ADMIN', updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
	int promoteToAdmin(@Param("id") Long id);

	@Update("UPDATE users SET role = #{role}, enabled = #{enabled}, password = #{password}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
	int updateAdminAccount(UserAccount user);

	@Update("UPDATE users SET password = #{password}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
	int updatePassword(@Param("id") Long id, @Param("password") String password);

	@org.apache.ibatis.annotations.Delete("DELETE FROM users WHERE id = #{id}")
	int deleteById(@Param("id") Long id);

	@Select("SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND enabled = TRUE")
	int countActiveAdmins();
}
