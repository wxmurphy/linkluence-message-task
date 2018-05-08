package com.bizlink.message.thread;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bizlink.message.cache.AppProperties;
import com.bizlink.message.cache.QueuePoolContext;
import com.bizlink.persistent.entity.TBizMessageQueue;
import com.bizlink.persistent.entity.TBizMessageQueueExample;
import com.bizlink.persistent.entity.TBizMessageQueueExample.Criteria;
import com.bizlink.persistent.mapper.TBizMessageQueueMapper;

@Component
@Scope(value = "prototype")
public class MessageProduceThread implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(MessageProduceThread.class);
	
	@Autowired
	private AppProperties appProperties;
	
	@Autowired
	private TBizMessageQueueMapper messageQueueMapper;
	
	@Override
	public void run() {
		while (true) {
			try {
				if (QueuePoolContext.PENDING_MESSAGE_QUEUE.isEmpty()) {
					TBizMessageQueueExample messageQueueExample = new TBizMessageQueueExample();
					Criteria c = messageQueueExample.createCriteria();
//					if (appProperties.TRANSCODE_LIST != null && appProperties.TRANSCODE_LIST.length > 0) {
//						if ("".equals(appProperties.TRANSCODE_LIST[0]) && appProperties.TRANSCODE_LIST.length == 1) {
//							
//						} else {
//							List<String> transCodeList = new ArrayList();
//							for (String transCode : appProperties.TRANSCODE_LIST) {
//								transCodeList.add(transCode);
//							}
//							c.andSourceIdIn(transCodeList);
//						}
//					}
					c.andPushFlagEqualTo(0);
					c.andStatusEqualTo(0);
					List<TBizMessageQueue> list = messageQueueMapper.selectByExample(messageQueueExample);
					
					LOG.info("查询消息队列:[{}]条待推送", list.size());
					
					for (TBizMessageQueue queue : list) {
						QueuePoolContext.PENDING_MESSAGE_QUEUE.put(queue);
					}
				}
				
			} catch (Exception e) {
				LOG.error("", e);
			}
			
			try {
				Thread.sleep(appProperties.PRODUCE_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
