package com.nubons.nnp.api.gw.service.client.to;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
public class HeartBeatResp {
	
	private boolean refresh;
	private long lastUpdated;

}
