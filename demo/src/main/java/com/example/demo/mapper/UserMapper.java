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

	@Select("SELECT id, username, password, role, enabled FROM users WHERE username = #{username}")
	UserAccount findByUsername(@Param("username") String username);

	@Select("SELECT id, username, role, enabled FROM users ORDER BY username")
	List<UserAccount> findAll();

	@Select("SELECT id, username, role, enabled FROM users WHERE id = #{id}")
	UserAccount findById(@Param("id") Long id);

	@Insert("INSERT INTO users (username, password, role) VALUES (#{username}, #{password}, #{role})")
	int insert(UserAccount user);

	@Update("UPDATE users SET role = #{role}, enabled = #{enabled} WHERE id = #{id}")
	int updateAdminFields(UserAccount user);

	@Update("UPDATE users SET role = 'ADMIN' WHERE id = #{id}")
	int promoteToAdmin(@Param("id") Long id);

	@Select("SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND enabled = TRUE")
	int countActiveAdmins();
}
