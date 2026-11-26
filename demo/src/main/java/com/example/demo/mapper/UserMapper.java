package com.example.demo.mapper;

import com.example.demo.UserAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

	@Select("SELECT id, username, password, role FROM users WHERE username = #{username}")
	UserAccount findByUsername(@Param("username") String username);

	@Insert("INSERT INTO users (username, password, role) VALUES (#{username}, #{password}, #{role})")
	int insert(UserAccount user);
}
