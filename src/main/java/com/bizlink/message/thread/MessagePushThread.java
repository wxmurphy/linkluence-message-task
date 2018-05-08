package com.bizlink.message.thread;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.platform.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bizlink.message.cache.AppProperties;
import com.bizlink.message.cache.QueuePoolContext;
import com.bizlink.message.constant.BillType;
import com.bizlink.message.constant.MessageType;
import com.bizlink.message.service.OrderMessageService;
import com.bizlink.message.utils.SpringUtils;
import com.bizlink.persistent.entity.TBizBillComment;
import com.bizlink.persistent.entity.TBizBillCommentKey;
import com.bizlink.persistent.entity.TBizBillHead;
import com.bizlink.persistent.entity.TBizBillHeadExample;
import com.bizlink.persistent.entity.TBizMessage;
import com.bizlink.persistent.entity.TBizMessageQueue;
import com.bizlink.persistent.entity.TBizTransaction;
import com.bizlink.persistent.entity.TBizTransactionExample;
import com.bizlink.persistent.entity.TBizUserMerchant;
import com.bizlink.persistent.entity.TBizUserMerchantExample;
import com.bizlink.persistent.mapper.TBizBillCommentMapper;
import com.bizlink.persistent.mapper.TBizBillHeadMapper;
import com.bizlink.persistent.mapper.TBizMessageMapperB;
import com.bizlink.persistent.mapper.TBizMessageQueueMapper;
import com.bizlink.persistent.mapper.TBizTransactionMapper;
import com.bizlink.persistent.mapper.TBizUserMerchantMapper;

@Component
@Scope(value = "prototype")
public class MessagePushThread implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(MessagePushThread.class);
	
	@Autowired
	private AppProperties appProperties;
	
	@Autowired
	private TBizTransactionMapper transactionMapper;
	
	@Autowired
	private TBizBillHeadMapper billHeadMapper;
	
	@Autowired
	private TBizMessageMapperB messageMapperB;
	
	@Autowired
	private TBizMessageQueueMapper messageQueueMapper;
	
	@Autowired
	private TBizBillCommentMapper commentMapper;
	
	@Autowired
	private TBizUserMerchantMapper userMerchantMapper;
	
	public void run() {
		while (true) {
			try {
				TBizMessageQueue queue = QueuePoolContext.PENDING_MESSAGE_QUEUE.take();
				if (queue != null) {
					Date createTime = new Date();
					
					int pushType = queue.getPushType();
					
					LOG.info("处理消息队列-{}-{}-{}", queue.getId(), pushType, queue.getSourceId());
					
					List<TBizMessage> messageList = new ArrayList();
					
					/** 单据消息 */
					if (pushType == 1) {
						TBizTransactionExample example = new TBizTransactionExample();
						example.createCriteria().andTransCodeEqualTo(queue.getSourceId())
												.andStatusEqualTo(0);
						List<TBizTransaction> list = transactionMapper.selectByExample(example);
						if (list.isEmpty()) 
							continue;
						
						TBizTransaction transaction = list.get(0);
						// object_id可能是单据号或者操作对象ID
						String billNo = transaction.getObjectId();
						
						/** 
						 * <b>1</b> 查询通知用户列表
						 * 单据创建人
						 * 对方负责人
						 * 己方单据关注人
						 * 对方单据关注人
						 * 己方创建人汇报对象
						 * 对方负责人汇报对象
						 */
						
						/* 1)单据创建人 */
						TBizBillHeadExample billHeadExample = new TBizBillHeadExample();
						billHeadExample.createCriteria().andBillNoEqualTo(billNo).andStatusEqualTo(0);
						List<TBizBillHead> billHeadList = billHeadMapper.selectByExample(billHeadExample);
						if (billHeadList.isEmpty()) 
							continue;
						TBizBillHead head = billHeadList.get(0);
						
						OrderMessageService service = null;
						if (head.getBillType() == BillType.PLAN_ORDER) {
							service = SpringUtils.CTX.getBean("planOrderMessageService", OrderMessageService.class);
						}
						else if (head.getBillType() == BillType.QUOTATION_ORDER) {
							service = SpringUtils.CTX.getBean("quotationOrderMessageService", OrderMessageService.class);
						}
						else if (head.getBillType() == BillType.PURCHASE_ORDER) {
							service = SpringUtils.CTX.getBean("purchaseOrderMessageService", OrderMessageService.class);
						}
						else if (head.getBillType() == BillType.PROTOCOL) {
							service = SpringUtils.CTX.getBean("protocolMessageService", OrderMessageService.class);
						}
						else if (head.getBillType() == BillType.SELL_ORDER) {
							service = SpringUtils.CTX.getBean("sellOrderMessageService", OrderMessageService.class);
						}
						else if (head.getBillType() == BillType.INVITATION) {
							service = SpringUtils.CTX.getBean("invitationMessageService", OrderMessageService.class);
						}
						if (service == null) {
							continue;
						}
						
						messageList = service.getMessage(head, transaction);
						
					// @我消息
					} else if (pushType == 2) {
						Long commentId = Long.parseLong(queue.getSourceId());
						TBizBillCommentKey key = new TBizBillCommentKey();
						key.setId(commentId);
						TBizBillComment comment = commentMapper.selectByPrimaryKey(key);
						if (comment == null) {
							continue;
						}
						String billNo = comment.getBillNo();
						if (!Asserts.isEmpty(comment.getNoticeList())) {
							String[] atList = comment.getNoticeList().split(",");
							for (String atUserId : atList) {
								Long userId = Long.parseLong(atUserId);
								
								TBizBillHeadExample billHeadExample = new TBizBillHeadExample();
								billHeadExample.createCriteria().andBillNoEqualTo(billNo).andStatusEqualTo(0);
								List<TBizBillHead> list = billHeadMapper.selectByExample(billHeadExample);
								TBizBillHead head = list.get(0);
								
								int msgType = 0;
								TBizUserMerchantExample example = new TBizUserMerchantExample();
								example.createCriteria().andUserIdEqualTo(userId)
														.andMerIdEqualTo(head.getMerId())
														.andStatusEqualTo(0);
								List<TBizUserMerchant> umList = userMerchantMapper.selectByExample(example);
								
								
								
								if (head.getBillType() == BillType.PURCHASE_ORDER
										|| head.getBillType() == BillType.PLAN_ORDER
										|| head.getBillType() == BillType.QUOTATION_ORDER) {
									msgType = !umList.isEmpty() ? MessageType.PURCHASE : MessageType.SELL;
								} 
								else if (head.getBillType() == BillType.SELL_ORDER) {
									msgType = !umList.isEmpty() ? MessageType.SELL : MessageType.PURCHASE;
								}
								
								TBizMessage message = new TBizMessage();
								message.setBillNo(comment.getBillNo());
								message.setUserId(userId);
								message.setMsgType(msgType);
								message.setReadFlag(0);
								message.setAtFlag(1);
								message.setNoticeFlag(0);
								message.setMsgContent("有人@你了,快去看看");
								message.setCreateTime(createTime);
								message.setStatus(0);
								messageList.add(message);
							}
						}
					}
					
					if (!messageList.isEmpty()) {
						messageMapperB.batchInsert(messageList);
						
						TBizMessageQueue record = new TBizMessageQueue();
						record.setId(queue.getId());
						record.setPushFlag(1);
						messageQueueMapper.updateByPrimaryKeySelective(record);
					}
				}
			} catch (Exception e) {
				LOG.error("", e);
			}
			
			try {
				Thread.sleep(appProperties.CONSUMER_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
