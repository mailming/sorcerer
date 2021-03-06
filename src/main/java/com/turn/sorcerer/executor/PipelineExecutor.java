/*
 * Copyright (c) 2015, Turn Inc. All Rights Reserved.
 * Use of this source code is governed by a BSD-style license that can be found
 * in the LICENSE file.
 */


package com.turn.sorcerer.executor;

import com.turn.sorcerer.pipeline.type.PipelineType;
import com.turn.sorcerer.pipeline.executable.ExecutablePipeline;
import com.turn.sorcerer.pipeline.executable.impl.PipelineFactory;
import com.turn.sorcerer.task.type.TaskType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class Description Here
 *
 * @author tshiou
 */
public class PipelineExecutor implements Runnable, Abortable {


	private static final Logger logger =
			LoggerFactory.getLogger(PipelineExecutor.class);

	private final ScheduledExecutorService executor;
	private final ExecutablePipeline pipeline;
	private final TaskScheduler taskScheduler;
	private final int interval;

	public PipelineExecutor(PipelineType pipelineType, int jobId) {
		this(pipelineType, jobId, new HashMap<TaskType, Map<String, String>>(), false, false);
	}

	public PipelineExecutor(PipelineType pipelineType,
	                        int jobId, Map<TaskType, Map<String, String>> taskArgMap,
	                        boolean adhoc, boolean overwriteTasks) {
		this.interval = pipelineType.getInterval();

		int threads = pipelineType.getThreads();

		executor = Executors.newSingleThreadScheduledExecutor();

		pipeline = PipelineFactory.get().getExecutablePipeline(pipelineType, jobId);

		// after we call getExecutablePipeline(), all parameters of pipelineFactory are cleared

		taskScheduler = new TaskScheduler(
				pipeline, pipeline.getId(), threads, taskArgMap, adhoc, overwriteTasks);
	}

	@Override
	public void run() {

		logger.info("Scheduling {} every {} seconds", pipeline, interval);

		pipeline.updatePipelineCompletion();

		executor.scheduleWithFixedDelay(taskScheduler, 0, interval,	TimeUnit.SECONDS);

		while (pipeline.isCompleted() == false) {
			try {
				Thread.sleep(1000 * interval);
			} catch (InterruptedException e) {
				logger.warn("Sleep interrupted");
				Thread.currentThread().interrupt();
			}
		}

		abort();
	}

	public void abort() {
		logger.info("Shutting down pipeline executor for " + pipeline);
		taskScheduler.abort();
		executor.shutdown();
	}

}
