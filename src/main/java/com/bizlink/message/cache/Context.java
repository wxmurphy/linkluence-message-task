package com.bizlink.message.cache;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class Context {
	public static final Map<Integer, String> TEMP_MSG_MAPPING = new HashMap();
}
