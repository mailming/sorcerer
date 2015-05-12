/*
 * Copyright (C) 2015 Turn Inc. All Rights Reserved.
 * Proprietary and confidential.
 */

package com.turn.sorcerer.pipeline.impl;

import com.turn.sorcerer.pipeline.Pipeline;
import com.turn.sorcerer.pipeline.type.PipelineType;
import com.turn.sorcerer.status.StatusManager;

/**
 * Class Description Here
 *
 * @author tshiou
 */
public class DefaultPipeline implements Pipeline {

	private final PipelineType type;

	public DefaultPipeline(PipelineType type) {
		this.type = type;
	}

	@Override
	public Integer getCurrentIterationNumber() {
		return StatusManager.get().getCurrentIterationNumberForPipeline(type) + 1;
	}
}