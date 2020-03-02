package com.ky.gmall.item;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

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