package com.ky.gmall.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class MyMvcConfig extends WebMvcConfigurerAdapter {
    @Bean
    public WebMvcConfigurerAdapter webMvcConfigurerAdapter(){
        WebMvcConfigurerAdapter adapter = new WebMvcConfigurerAdapter() {

            @Bean
            public WebMvcConfigurerAdapter webMvcConfigurerAdapter(){
                WebMvcConfigurerAdapter adapter = new WebMvcConfigurerAdapter() {
                    @Override
                    public void addInterceptors(InterceptorRegistry registry) {
                        //super.addInterceptors(registry);
                        registry.addInterceptor(new LoginHandlerInterceptor()).addPathPatterns("/**")
                                .excludePathPatterns("/index.html","/","/user/login","/bootstrap/**","/image/**","/scss/**","/js/**","/img/**");
                    }
                };
                return  adapter;
            }
        };
        return  adapter;
    }
}