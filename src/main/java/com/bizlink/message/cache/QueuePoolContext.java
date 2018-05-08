package com.bizlink.message.cache;

import java.util.concurrent.LinkedBlockingDeque;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bizlink.persistent.entity.TBizMessageQueue;

@Component
@Scope("singleton")
public class QueuePoolContext {
	public static LinkedBlockingDeque<TBizMessageQueue> PENDING_MESSAGE_QUEUE = new LinkedBlockingDeque(5000);
}
	
