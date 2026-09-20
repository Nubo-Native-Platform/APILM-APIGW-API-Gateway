package com.nubons.nnp.api.gw.filter;

import lombok.Getter;

public enum GwFilterOrderingEnum {
	
	ORDER_1(-5), ORDER_2(-4), ORDER_3(-3), ORDER_4(-2), ORDER_5(-1), ORDER_6(0), ORDER_7(1);
	
	@Getter
	private final Integer order;

	private GwFilterOrderingEnum(Integer order) {
		this.order = order;
	}
	
	public Integer getValue() {
		return order;
	}
}
