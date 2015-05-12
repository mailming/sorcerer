/*
 * Copyright (C) 2015 Turn Inc. All Rights Reserved.
 * Proprietary and confidential.
 */

package com.turn.sorcerer.pipeline.impl;

import com.turn.sorcerer.pipeline.Pipeline;
import com.turn.sorcerer.util.CronExpression;

/**
 * Class Description Here
 *
 * @author tshiou
 */
public class CronPipeline implements Pipeline {

	private final CronExpression cronExp;

	public CronPipeline(String cronString) {
		this.cronExp = new CronExpression(cronString);
	}

	@Override
	public Integer getCurrentIterationNumber() {



		return null;
	}
}