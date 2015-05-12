/*
 * Copyright (C) 2015 Turn Inc. All Rights Reserved.
 * Proprietary and confidential.
 */

package com.turn.sorcerer.config.impl;

import com.turn.sorcerer.module.ModuleType;
import com.turn.sorcerer.pipeline.Pipeline;
import com.turn.sorcerer.pipeline.type.PipelineType;
import com.turn.sorcerer.status.StatusStorage;
import com.turn.sorcerer.task.Task;
import com.turn.sorcerer.task.type.TaskType;
import com.turn.sorcerer.util.email.EmailType;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

/**
 * Class Description Here
 *
 * @author tshiou
 */
public class SorcererAbstractModule extends AbstractModule {

	private Map<String, TaskType> tasks = Maps.newHashMap();
	private Map<String, Class<? extends Task>> taskClasses = Maps.newHashMap();
	private Map<String, PipelineType> pipelines = Maps.newHashMap();
	private Map<String, Class<? extends Pipeline>> pipelineClasses = Maps.newHashMap();
	private ModuleType module;

	protected SorcererAbstractModule(
			Map<String, TaskType> tasks,
			Map<String, Class<? extends Task>> taskClasses,
			Map<String, PipelineType> pipelines,
			Map<String, Class<? extends Pipeline>> pipelineClasses,
			ModuleType module
	) {
		this.tasks = tasks;
		this.taskClasses = taskClasses;
		this.pipelines = pipelines;
		this.pipelineClasses = pipelineClasses;
		this.module = module;
	}

	@Override
	protected void configure() {
		// Task

		// Bind task types annotated by their name
		for (Map.Entry<String, TaskType> entry : tasks.entrySet()) {
			bind(TaskType.class).annotatedWith(Names.named(entry.getKey()))
					.toInstance(entry.getValue());
		}

		// Bind task classes
		for (Map.Entry<String, Class<? extends Task>> entry : taskClasses.entrySet()) {
			bind(Task.class).annotatedWith(Names.named(entry.getKey()))
					.to(entry.getValue());
		}


		// Pipelines
		Multibinder<PipelineType> pipelineSet = Multibinder.newSetBinder(binder(), PipelineType.class);

		for (Map.Entry<String, PipelineType> entry : pipelines.entrySet()) {
			// Bind pipelines to run
			pipelineSet.addBinding().toInstance(entry.getValue());

			// Bind pipeline type
			bind(PipelineType.class).annotatedWith(Names.named(entry.getKey()))
					.toInstance(entry.getValue());
		}

		// Bind pipeline classes
		for (Map.Entry<String, Class<? extends Pipeline>> entry : pipelineClasses.entrySet()) {
			bind(Pipeline.class).annotatedWith(Names.named(entry.getKey()))
					.to(entry.getValue());
		}

		// Bind module and configurations
		bind(ModuleType.class).toInstance(module);

		bind(EmailType.class).toInstance(module.getAdminEmail());

		bind(StatusStorage.class).to(module.getStorage().getStorageClass());
	}


}
