package com.bizlink.message.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bizlink.message.service.OrderMessageService;
import com.bizlink.persistent.entity.TBizBillHead;
import com.bizlink.persistent.entity.TBizMessage;
import com.bizlink.persistent.entity.TBizTransaction;

@Service("quotationOrderMessageService")
public class QuotationOrderMessageServiceImpl implements OrderMessageService {

	@Override
	public List<TBizMessage> getMessage(TBizBillHead head, TBizTransaction transaction) {
		// TODO Auto-generated method stub
		return null;
	}

}
