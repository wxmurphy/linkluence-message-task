package com.bizlink.message;

import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.fastjson.JSON;
import com.bizlink.message.cache.AppProperties;
import com.bizlink.message.utils.SpringUtils;

public class Bootstrap {
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		System.out.println("---\nLINKLUENCE-MESSAGE-TASK starting...");

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-context.xml");

        AppProperties appProperties = context.getBean(AppProperties.class);

        System.out.println("\nConfiguration:" + JSON.toJSONString(appProperties, true));

        try {
			SpringUtils.CTX.getBean(Initializer.class).start();
			
			// context.close();
		} catch (BeansException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
