package com.nubons.nnp.api.gw.filter;

import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;

/**
 * 
 * @author Gourab Guha
 *
 */
public abstract class AbstractCustomFilter extends AbstractGatewayFilterFactory<AbstractCustomFilter.Config> {

	protected AbstractCustomFilter() {
		super(Config.class);
	}
	protected AbstractCustomFilter(Class<? extends Config> tClass) {
		super((Class<Config>) tClass);
	}

	public static class Config {
		private String name;
		private String order;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getOrder() {
			return order;
		}

		public void setOrder(String order) {
			this.order = order;
		}
	}

}
