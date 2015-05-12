/*
 * Copyright (C) 2015 Turn Inc. All Rights Reserved.
 * Proprietary and confidential.
 */

package com.turn.sorcerer.task.executable;

import com.turn.sorcerer.dependency.Dependency;
import com.turn.sorcerer.exception.SorcererException;
import com.turn.sorcerer.status.Status;
import com.turn.sorcerer.status.StatusManager;
import com.turn.sorcerer.task.Context;
import com.turn.sorcerer.task.Task;
import com.turn.sorcerer.task.type.TaskType;
import com.turn.sorcerer.util.Constants;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Class Description Here
 *
 * @author tshiou
 */
public class ExecutableTask {

	private boolean adhoc = false;

	private final int sequenceNumber;
	private final TaskType type;
	private final Task task;

	List<String> taskParameters = Lists.newArrayList();

	protected ExecutableTask(TaskType type, Task task, int seq) {
		this.type = type;
		this.task = task;
		this.sequenceNumber = seq;
	}

	public void parameterize(String[] args) {
		if (args == null || args.length == 0) {
			return;
		}

		for (String arg : args) {
			if (Constants.ADHOC.equals(arg)) {
				this.adhoc = true;
			}
			taskParameters.add(arg);
		}
	}

	public boolean checkDependencies() {
		// If the task doesn't have dependencies, return true
		if (task.getDependencies(sequenceNumber) == null ||
				task.getDependencies(sequenceNumber).size() == 0) {
			return true;
		}
		ImmutableList<Dependency> dependencies =
				ImmutableList.copyOf(task.getDependencies(sequenceNumber));

		// If any dependencies not met, return false
		for (Dependency dependency : dependencies) {
			if (dependency.check(sequenceNumber) == false) {
				return false;
			}
		}

		return true;
	}

	public void execute(Context context) throws SorcererException {
		try {
			StatusManager.get().commitTaskStatus(this.type, sequenceNumber, Status.IN_PROGRESS);

			task.exec(context);

		} catch (SorcererException e) {
			StatusManager.get()
					.removeInProgressTaskStatus(this.type, sequenceNumber);
			if (!adhoc) {
				StatusManager.get().commitTaskStatus(
						this.type, sequenceNumber, Status.ERROR, true);
			}
			throw e;
		}

		StatusManager.get().commitTaskStatus(this.type, sequenceNumber, Status.SUCCESS, true);
		StatusManager.get().removeInProgressTaskStatus(this.type, sequenceNumber);

	}

	public boolean isCompleted() {
		return StatusManager.get().isTaskComplete(this.type, this.sequenceNumber);
	}

	public boolean isRunning() {
		return StatusManager.get().isTaskRunning(this.type, this.sequenceNumber);
	}

	public boolean hasError() {
		return StatusManager.get().isTaskInError(this.type, this.sequenceNumber);
	}

	public String name() {
		return this.type.getName();
	}

	@Override
	public String toString() {
		return name() + ":" + sequenceNumber;
	}
}