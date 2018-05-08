package com.bizlink.message.constant;

public interface OpCode {
	static final String BILL_CONFIRM = "BC";
	
	static final String BILL_CONFIRM_COMPLETE = "BCC";
	
	static final String BILL_UNCONFIRM = "BU";
	
	static final String BILL_CREATE = "BA";
	
	static final String BILL_RETURN_ITEM = "BR";
	
	static final String BILL_RECV_ITEM = "RI";
	
	static final String BILL_RET_RECV_ITEM = "RR";
	
	static final String BILL_SEND_ITEM = "BS";
	
	static final String BILL_UPDATE = "BE";
	
	static final String REQUEST_FOR_CANCEL = "REC";
	
	static final String AGREE_CANCEL = "AC";
	
	static final String REFUSE_CANCEL = "RC";
}
