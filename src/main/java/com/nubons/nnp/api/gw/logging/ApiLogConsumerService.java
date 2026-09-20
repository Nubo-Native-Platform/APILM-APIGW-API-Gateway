package com.nubons.nnp.api.gw.logging;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.nubons.nnp.api.abs.to.ApiLogTO;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Gourab Guha
 *
 */
@Service
@ConditionalOnExpression(value = "${api.logging.enable:false}==true && ${api.logging.enable.db:false}==true")
@Slf4j
public class ApiLogConsumerService {
	
	@Autowired
	@Qualifier("scheduled-thread-pool-with-one")
	private ScheduledExecutorService executorSvc;

	/*
	 * ApiLogBuffer will be autowired if api.logging.enable & api.logging.enable.db
	 * both set as true (as the condition on expression defined on this class)
	 */
	@Autowired
	private ApiLogBuffer logBuffer;

	@Autowired
	private RestTemplate restTemplate;

	@Value("${nnp.apiecosystem.service.url}")
	protected String svcBaseUrl;

	@Value("${api.logging.enable.db.task.maxcount:50}")
	private int taskMaxCount;

	@Value("${api.logging.enable.db.task.delayinseconds}")
	private int delayInSeconds;

	@PostConstruct
	public void startup() {
		//executorSvc = Executors.newScheduledThreadPool(1);
		//log.info("ScheduledThreadPoolExecutorService is constructd");
		executorSvc.scheduleWithFixedDelay(new LogConsumerTask(), 0, delayInSeconds, TimeUnit.SECONDS);
		log.info("Consumer Task has been started with {} seconds interval", delayInSeconds);
	}

	@PreDestroy
	public void shutdown() {

		try {
			log.info(">>> Shutting down ScheduledThreadPoolExecutorService >>>");
			if (Objects.nonNull(executorSvc)) {
				boolean terminationStatus = executorSvc.awaitTermination(10, TimeUnit.SECONDS);
				log.info("<<< An attempt has been made to shutdown the ScheduledThreadPoolExecutorService, the status is {} <<<", terminationStatus);
			} else {
				log.error(
						"<<< ScheduledThreadPoolExecutorService could not be shutted down properly, as the instance is set with null <<<");
			}
		} catch (Exception ex) {
			log.error(
					"@@@@@@@@@@@@@ ScheduledThreadPoolExecutorService could not be shutted down properly @@@@@@@@@@@@@ ",
					ex);
		} finally {
			executorSvc = null;
		}
	}

	private class LogConsumerTask implements Runnable {

		@Override
		public void run() {
			
			log.debug("LogConsumerTask has been started at {}", new Date());
			List<ApiLogTO> logToList = null;
			try {
				logToList = new ArrayList<>(taskMaxCount / 2);
				int counter = 0;
				ApiLogTO logTo = null;
				do {
					logTo = logBuffer.poll();
					if (Objects.nonNull(logTo)) {
						logToList.add(logTo);
						counter++;
					}
				} while (Objects.nonNull(logTo) && counter <= taskMaxCount);

				if (logToList.size() > 0) {
					publish(logToList);
					log.debug("Published {} ApiLog(s) ", logToList.size());
				}
			} catch (Exception ex) {
				log.error("Exception caught while consuming Log {}", ex);
			} finally {
				if (Objects.nonNull(logToList) && logToList.size() > 0) {
					logToList.clear();
				}
				log.debug("LogConsumerTask is being stopped at {}", new Date());
			}
		}

		private void publish(List<ApiLogTO> logToList) {

			ResponseEntity<?> logPersistResp = restTemplate.postForEntity(svcBaseUrl + "/log", logToList,
					ResponseEntity.class);
			log.debug("Response from gw service after persisting log {}", logPersistResp.getStatusCode());
		}
	}
}
