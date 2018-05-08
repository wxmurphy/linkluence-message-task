package com.bizlink.message.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringUtils implements ApplicationContextAware {

	public static ApplicationContext CTX;

	@Override
	public void setApplicationContext(ApplicationContext appContext) throws BeansException {
		CTX = appContext;
	}

	public static Object getBean(String name) {
		return CTX.getBean(name);
	}
	
	public static ApplicationContext getApplicationContext() {
		return CTX;
	}

}
