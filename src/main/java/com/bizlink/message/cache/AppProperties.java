package com.bizlink.message.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class AppProperties {
	@Value("${biz.produce.interval}")
	public Integer PRODUCE_INTERVAL;
	
	@Value("${biz.consumer.interval}")
	public Integer CONSUMER_INTERVAL;
	
	@Value("#{'${biz.spec.proc.transcode}'.split(',')}")
	public String[] TRANSCODE_LIST;
}
