package com.bizlink.message.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.platform.util.Asserts;
import org.platform.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.bizlink.message.cache.Context;
import com.bizlink.message.constant.MessageType;
import com.bizlink.message.service.OrderMessageService;
import com.bizlink.persistent.entity.TBizBillHead;
import com.bizlink.persistent.entity.TBizBillHeadSideInfo;
import com.bizlink.persistent.entity.TBizBillHeadSideInfoExample;
import com.bizlink.persistent.entity.TBizMessage;
import com.bizlink.persistent.entity.TBizTransaction;
import com.bizlink.persistent.entity.TBizTransactionSideInfo;
import com.bizlink.persistent.entity.TBizTransactionSideInfoExample;
import com.bizlink.persistent.entity.TBizUserInteractPartner;
import com.bizlink.persistent.entity.TBizUserInteractPartnerExample;
import com.bizlink.persistent.entity.TBizUserRepo;
import com.bizlink.persistent.entity.TBizUserRepoExample;
import com.bizlink.persistent.mapper.TBizBillHeadSideInfoMapper;
import com.bizlink.persistent.mapper.TBizTransactionSideInfoMapper;
import com.bizlink.persistent.mapper.TBizUserInteractPartnerMapper;
import com.bizlink.persistent.mapper.TBizUserRepoMapper;

@Service("purchaseOrderMessageService")
public class PurchaseOrderMessageServiceImpl implements OrderMessageService {
	@Autowired
	private TBizBillHeadSideInfoMapper billHeadSideInfoMapper;
	
	@Autowired
	private TBizUserInteractPartnerMapper userInteractPartnerMapper;
	
	@Autowired
	private TBizUserRepoMapper userRepoMapper;
	
	@Autowired
	private TBizTransactionSideInfoMapper transactionSideInfoMapper;
	
	@Override
	public List<TBizMessage> getMessage(TBizBillHead head, TBizTransaction transaction) {
		List<TBizMessage> result = new ArrayList();
		
		Date createTime = new Date();
		
		Long merId = head.getMerId();
		Long relativeMerId = head.getRelativeMerId();
		
		/** 1)单据创建人 */
		Set<Long> customerUserSet = new HashSet();
		Set<Long> supplierUserSet = new HashSet();
		
		customerUserSet.add(head.getMarker());
		
		/** 2)对方负责人 */
		// 按限定条件查找对方负责人
		// 对方负责己方负责人
		boolean partnerHasManager = false;
		TBizUserInteractPartnerExample userInteractPartnerExample = new TBizUserInteractPartnerExample();
		userInteractPartnerExample.createCriteria().andMerIdEqualTo(relativeMerId)
												   .andPartnerIdEqualTo(merId)
												   .andStatusEqualTo(0);
		List<TBizUserInteractPartner> interactPartnerList = userInteractPartnerMapper.selectByExample(userInteractPartnerExample);
		if (!interactPartnerList.isEmpty()) {
			partnerHasManager = true;
			
			List<Long> interactUserList = new ArrayList();
			for (TBizUserInteractPartner t : interactPartnerList) {
				supplierUserSet.add(t.getUserId());
				interactUserList.add(t.getUserId());
			}
			
			TBizUserRepoExample userRepoExample = new TBizUserRepoExample();
			userRepoExample.createCriteria().andMerIdEqualTo(relativeMerId)
											.andUserIdIn(interactUserList)
											.andStatusEqualTo(0);
			List<TBizUserRepo> repoTo = userRepoMapper.selectByExample(userRepoExample);
			for (TBizUserRepo repo : repoTo) {
				supplierUserSet.add(repo.getRepoUserId());
			}
		} 
		
		if (!partnerHasManager) {
			return result;
		}
		
		// 3)对方及己方单据关注人
		TBizBillHeadSideInfoExample example = new TBizBillHeadSideInfoExample();
		example.createCriteria().andHidEqualTo(head.getId()).andStatusEqualTo(0);
		List<TBizBillHeadSideInfo> billHeadSideInfoList = billHeadSideInfoMapper.selectByExample(example);
		for (TBizBillHeadSideInfo hsi : billHeadSideInfoList) {
			String noticeList = hsi.getNoticeList();
			if (!Asserts.isEmpty(noticeList)) {
				String[] arr = noticeList.split(",");
				for (String userId : arr) {
					if (hsi.getOriginator() == 1) {
						customerUserSet.add(Long.parseLong(userId));
					} else {
						supplierUserSet.add(Long.parseLong(userId));
					}
				}
			}
		}
		
		// 4)己方汇报对象
		TBizUserRepoExample userRepoExample = new TBizUserRepoExample();
		userRepoExample.createCriteria().andMerIdEqualTo(merId)
										.andUserIdEqualTo(head.getMarker())
										.andStatusEqualTo(0);
		List<TBizUserRepo> userRepoList = userRepoMapper.selectByExample(userRepoExample);
		for (TBizUserRepo t : userRepoList) {
			customerUserSet.add(t.getRepoUserId());
		}
		
		TBizTransactionSideInfoExample tsiExample = new TBizTransactionSideInfoExample();
		tsiExample.createCriteria().andTransCodeEqualTo(transaction.getTransCode())
								   .andStatusEqualTo(0);
		List<TBizTransactionSideInfo> tsiList = transactionSideInfoMapper.selectByExample(tsiExample);
		
		String customerData = "";
		String supplierData = "";
		for (TBizTransactionSideInfo tsi : tsiList) {
			if (merId.equals(tsi.getMerId())) {
				customerData = tsi.getData();
			}
			else if (relativeMerId.equals(tsi.getMerId())) {
				supplierData = tsi.getData();
			}
		}
		
		
		Iterator<Long> it = customerUserSet.iterator();
		while (it.hasNext()) {
			Long userId = it.next();
			TBizMessage message = new TBizMessage();
			message.setBillNo(transaction.getObjectId());
			message.setUserId(userId);
			message.setMsgType(MessageType.PURCHASE);
			message.setReadFlag(0);
			Map data = generateTemplateMessage(customerData);
			message.setMsgContent(JSON.toJSONString(data));
			message.setCreateTime(createTime);
			message.setStatus(0);
			result.add(message);
		}
		
		it = supplierUserSet.iterator();
		while (it.hasNext()) {
			Long userId = it.next();
			TBizMessage message = new TBizMessage();
			message.setBillNo(transaction.getObjectId());
			message.setUserId(userId);
			message.setMsgType(MessageType.SELL);
			message.setReadFlag(0);
			Map data = generateTemplateMessage(supplierData);
			message.setMsgContent(JSON.toJSONString(data));
			message.setCreateTime(createTime);
			message.setStatus(0);
			result.add(message);
		}
		
		return result;
	}
	
	private Map generateTemplateMessage(String data) {
		Map result = new HashMap();
		String[] arr = data.split(",");
		List<String> list = new ArrayList();
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == null || "".equals(arr[i])) {
				continue;
			}
			if (i <= 0) {
				String headData = arr[i];
				String[] headArr = headData.split("#");
				Integer headTemplateId = Integer.parseInt(headArr[1]);
				String[] dataParams = headArr[0].split("\\|");
				
				String template = Context.TEMP_MSG_MAPPING.get(headTemplateId);
				String message = StringUtils.setParameters(template, dataParams);
				result.put("title", message);
			} else {
				String d = arr[i];
				String[] dArr = d.split("#");
				Integer templateId = Integer.parseInt(dArr[1]);
				String[] dataParams = dArr[0].split("\\|");
				
				String template = Context.TEMP_MSG_MAPPING.get(templateId);
				String message = StringUtils.setParameters(template, dataParams);
				list.add(message);
			}
		}
		result.put("list", list);
		return result;
	}

}
