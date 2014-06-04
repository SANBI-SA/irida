package ca.corefacility.bioinformatics.irida.ria.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;

import ca.corefacility.bioinformatics.irida.config.IridaApiSecurityConfig;

/**
 * Customizes Spring Security configuration.
 * 
 * @author Josh Adam <josh.adam@phac-aspc.gc.ca>
 */
@Configuration
@EnableWebSecurity
@Import({ IridaApiSecurityConfig.class })
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/resources/**").antMatchers("/public/**");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http

		// Prevent Cross Site Request Forgery
		.csrf()
				.disable()
				// Refactor login form

				// See https://jira.springsource.org/browse/SPR-11496
				// This is for SockJS and Web Sockets
				.headers()
				.addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN))
				.and()

				.formLogin().defaultSuccessUrl("/app").loginPage("/login").failureUrl("/login?error=true").permitAll().and()
				.logout().logoutSuccessUrl("/login").logoutUrl("/logout").permitAll().and()

				.authorizeRequests().regexMatchers("/login((\\?lang=[a-z]{2}|#.*))?").permitAll().antMatchers("/")
				.permitAll().antMatchers("/license").permitAll().antMatchers("/resources/**").permitAll()
				.antMatchers("/**").fullyAuthenticated();

	}
}
