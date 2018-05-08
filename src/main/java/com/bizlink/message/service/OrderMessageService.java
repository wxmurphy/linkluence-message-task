package com.bizlink.message.service;

import java.util.List;

import com.bizlink.persistent.entity.TBizBillHead;
import com.bizlink.persistent.entity.TBizMessage;
import com.bizlink.persistent.entity.TBizTransaction;

public interface OrderMessageService {
	List<TBizMessage> getMessage(TBizBillHead head, TBizTransaction transaction);
}
