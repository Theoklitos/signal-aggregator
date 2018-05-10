package com.quantbro.aggregator.configuration;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	// https://stackoverflow.com/questions/40286549/spring-boot-security-cors
	@Override
	protected void configure(final HttpSecurity http) throws Exception {
		http.exceptionHandling().authenticationEntryPoint(new AuthenticationEntryPoint() {

			@Override
			public void commence(final HttpServletRequest request, final HttpServletResponse response, final AuthenticationException authException)
					throws IOException, ServletException {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
			}
		}).and().formLogin()
				// .successHandler(ajaxSuccessHandler)
				.failureHandler(new AuthenticationFailureHandler() {

					@Override
					public void onAuthenticationFailure(final HttpServletRequest request, final HttpServletResponse response,
							final AuthenticationException exception) throws IOException, ServletException {
						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						final PrintWriter writer = response.getWriter();
						writer.write(exception.getMessage());
						writer.flush();
					}
				}).loginProcessingUrl("/auth").passwordParameter("password").usernameParameter("username").and().logout().deleteCookies("JSESSIONID")
				.invalidateHttpSession(true).logoutUrl("/logout").logoutSuccessUrl("/").and().csrf().disable().cors().disable().authorizeRequests()
				.antMatchers("/**").permitAll();
		// .antMatchers("/api/overview*").hasRole("USER") TODO access control
	}

	@Autowired
	public void configureGlobal(final AuthenticationManagerBuilder auth) throws Exception {
		auth.inMemoryAuthentication().withUser("test").password("test").roles("USER");
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurerAdapter() {

			@Override
			public void addCorsMappings(final CorsRegistry registry) {
				registry.addMapping("/**").allowedOrigins("*");
			}
		};
	}

}
