package com.nubons.nnp.api.gw.logging;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import com.nubons.nnp.api.abs.to.ApiLogTO;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Gourab Guha
 *
 */
@Service
@ConditionalOnExpression(value = "${api.logging.enable:true}==true && ${api.logging.enable.db:true}==true")
@Slf4j
public class ApiLogBuffer {

	private ConcurrentLinkedQueue<ApiLogTO> logBuffer = new ConcurrentLinkedQueue<>();

	public void add(ApiLogTO logObj) {
		logBuffer.add(logObj);
		log.debug("ApiLog added in logBuffer {}", logObj);
	}

	public ApiLogTO poll() {
		return logBuffer.poll();
	}

}
