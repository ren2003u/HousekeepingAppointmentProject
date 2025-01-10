package com.gk.study.config;

import com.gk.study.jwt.JwtAuthenticationFilter;
import com.gk.study.service.impl.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;  // 自定义的UserDetailsService

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter; // JWT过滤器

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 这里可选：BCryptPasswordEncoder，或保持MD5(不推荐)等
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // 告诉 Spring Security 如何加载用户（从数据库查）
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 1.关闭跨站保护(CSRF)等
        http.csrf().disable();
        // 2.对特定接口放行，如注册、登录、静态资源等
        http.authorizeRequests()
                .antMatchers("/api/auth/**").permitAll()  // 登录/注册接口无需鉴权
                .antMatchers("/api/user/**").permitAll()
                .antMatchers("/user/**").permitAll()
                .antMatchers("/auth/**").permitAll()
                .antMatchers("/api/thing/list").permitAll()
                // ... 其他放行接口
                .anyRequest().authenticated();

        // 3.添加JWT过滤器，在 UsernamePasswordAuthenticationFilter 之前执行
        http.addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        // 4. 关闭 session
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        // 用于在AuthController里注入
        return super.authenticationManagerBean();
    }
}

