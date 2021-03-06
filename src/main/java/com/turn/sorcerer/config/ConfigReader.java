/*
 * Copyright (c) 2015, Turn Inc. All Rights Reserved.
 * Use of this source code is governed by a BSD-style license that can be found
 * in the LICENSE file.
 */

package com.turn.sorcerer.config;

import com.turn.sorcerer.exception.SorcererException;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;

/**
 * Class Description Here
 *
 * @author tshiou
 */
public interface ConfigReader {

	// returns addPackages to search for annotations parsed from config
	Collection<String> read(Collection<File> configFiles) throws SorcererException;

	FilenameFilter getFileFilter();
}
