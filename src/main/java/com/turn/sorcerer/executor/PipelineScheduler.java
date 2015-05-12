/*
 * Copyright (C) 2015 Turn Inc. All Rights Reserved.
 * Proprietary and confidential.
 */

package com.turn.sorcerer.executor;

import com.turn.sorcerer.pipeline.Pipeline;
import com.turn.sorcerer.pipeline.type.PipelineType;
import com.turn.sorcerer.pipeline.executable.ExecutablePipeline;
import com.turn.sorcerer.pipeline.executable.impl.PipelineFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class Description Here
 *
 * @author tshiou
 */
public class PipelineScheduler implements Runnable {

	private static final Logger logger =
			LogManager.getFormatterLogger(PipelineScheduler.class);

	private final PipelineType pipelineType;
	private final Pipeline iterNoGenerator;

	// The scheduler should only use a single thread
	volatile boolean abort = false;

	// Instances of pipeline to run
	private Map<Integer, ExecutablePipeline> pipelineInstances = Maps.newHashMap();

	// Pipeline executors
	private Map<Integer, PipelineExecutor> pipelineExecutors = Maps.newConcurrentMap();
	private Map<Integer, ExecutorService> pipelineSchedulers = Maps.newHashMap();

	public PipelineScheduler(PipelineType pipelineType) {
		this.pipelineType = pipelineType;
		this.iterNoGenerator = PipelineFactory.get().getPipeline(pipelineType);
	}

	@Override
	public void run() {

		logger.debug("Launching pipeline " + pipelineType.getName());

		while (!abort) {
			int currIter = iterNoGenerator.getCurrentIterationNumber();

			// Get prior days
			List<Integer> iterationsToRun = Lists.newArrayList();
			for (int sequenceNumber = currIter ;
			     sequenceNumber >= currIter - pipelineType.getLookback() ;
			     sequenceNumber-- ) {

				iterationsToRun.add(sequenceNumber);

				if (pipelineInstances.containsKey(sequenceNumber) == false) {
					pipelineInstances.put(sequenceNumber, null);
				}
			}

			// Attempt to run pipeline on
			Set<Integer> sequenceNumbers = Sets.newHashSet(pipelineInstances.keySet());
			for (Integer sequenceNumber : sequenceNumbers) {
				logger.debug("Attempting to run %s for iteration %s",
						pipelineType.getName(), sequenceNumber);

				ExecutablePipeline pipelineInstance = pipelineInstances.get(sequenceNumber);
				ExecutorService executor = pipelineSchedulers.get(sequenceNumber);

				// If we're not interested in running a date, remove from pipeline instances
				if (iterationsToRun.contains(sequenceNumber) == false) {
					logger.info("Removing %s from pipeline queue", pipelineInstance);
					pipelineInstances.remove(sequenceNumber);
					removeExecutorForPipeline(sequenceNumber);
					pipelineExecutors.remove(sequenceNumber);
					PipelineFactory.get().expireSequenceNumber(pipelineType, sequenceNumber);
				}

				// If this is the first time running this instance of pipeline, create it
				if (pipelineInstance == null) {
					logger.info("Creating new instance of pipeline %s for %s",
							pipelineType.getName(), sequenceNumber);
					pipelineInstance = PipelineFactory.get().getExecutablePipeline(pipelineType, sequenceNumber);
					pipelineInstances.put(sequenceNumber, pipelineInstance);
				}

				// Update pipeline status
				pipelineInstance.updatePipelineCompletion();

				// If instance of pipeline is completed, don't schedule
				if (pipelineInstance.isCompleted()) {
					logger.debug("Pipeline %s is completed", pipelineInstance);
					continue;
				}

				// If pipeline hasn't been scheduled, schedule pipeline
				if (executor == null) {
					logger.debug("Scheduling pipeline " + pipelineType);
					executor = Executors.newSingleThreadExecutor();

					PipelineExecutor pipelineExecutor = new PipelineExecutor(pipelineType, sequenceNumber);
					pipelineExecutors.put(sequenceNumber, pipelineExecutor);
					executor.submit(pipelineExecutor);
					pipelineSchedulers.put(sequenceNumber, executor);
				}
			}

			try {
				Thread.sleep(1000 * pipelineType.getInterval());
			} catch (InterruptedException e) {
				logger.warn("Thread sleep was interrupted", e);
			}
		}

	}

	private void removeExecutorForPipeline(int sequenceNumber) {
		ExecutorService executor = pipelineSchedulers.get(sequenceNumber);

		if (executor == null) {
			return;
		}

		if (executor.isShutdown() == false) {
			executor.shutdown();
		}
		pipelineSchedulers.remove(sequenceNumber);
	}

	public void abort() {
		abort = true;
	}

}
