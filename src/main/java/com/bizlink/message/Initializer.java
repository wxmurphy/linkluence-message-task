package com.bizlink.message;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bizlink.message.cache.Context;
import com.bizlink.message.thread.MessageProduceThread;
import com.bizlink.message.thread.MessagePushThread;
import com.bizlink.message.utils.SpringUtils;
import com.bizlink.persistent.entity.TBizMessageTemplate;
import com.bizlink.persistent.entity.TBizMessageTemplateExample;
import com.bizlink.persistent.mapper.TBizMessageTemplateMapper;

@Component
public class Initializer {
	@Autowired
	private TBizMessageTemplateMapper messageTemplateMapper;
	
	public void start() throws InterruptedException {
		TBizMessageTemplateExample example = new TBizMessageTemplateExample();
		List<TBizMessageTemplate> list = messageTemplateMapper.selectByExample(example);
		for (TBizMessageTemplate m : list) {
			Context.TEMP_MSG_MAPPING.put(m.getId(), m.getTempData());
		}
		
		Random rand = new Random();
		
		ExecutorService producerService = Executors.newSingleThreadExecutor();
		MessageProduceThread producerTask = SpringUtils.CTX.getBean(MessageProduceThread.class);
		producerService.execute(producerTask);
		
		ExecutorService consumerService = Executors.newFixedThreadPool(5);
		for (int i = 0; i < 5; i++) {
			MessagePushThread consumerTask = SpringUtils.CTX.getBean(MessagePushThread.class);
			Thread.sleep(rand.nextInt(50));
			consumerService.execute(consumerTask);
		}
	}
}
