/*
 * Copyright (C) 2015 Turn Inc. All Rights Reserved.
 * Proprietary and confidential.
 */

package com.turn.sorcerer.status.impl;

import com.turn.sorcerer.status.Status;
import com.turn.sorcerer.status.StatusStorage;

import java.io.IOException;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

/**
 * Class Description Here
 *
 * @author tshiou
 */
public class HDFSStatusStorage implements StatusStorage {
	private static final Logger logger =
			LogManager.getLogger(HDFSStatusStorage.class);

	private FileSystem fs;

	private static final Joiner JOINER = Joiner.on(Path.SEPARATOR);

	private final String root;

	public HDFSStatusStorage(String root) {
		this.root = root;
	}

	private String getStatusPath(String identifier, int id) {
		return JOINER.join(getStatusPath(identifier), id);
	}

	private String getStatusPath(String identifier) {
		return JOINER.join(root, identifier);
	}

	@Override
	public DateTime getLastUpdateTime(String identifier, int id) throws IOException {
		Preconditions.checkNotNull(identifier);
		Preconditions.checkNotNull(id);

		if (fs == null) {
			try {
				fs = FileSystem.get(new Configuration());
			} catch (IOException e) {
				logger.error(e);
				throw e;
			}
		}

		long maxTS = 0;

		Path directoryPath = new Path(getStatusPath(identifier, id));

		FileStatus[] fileStatuses = fs.listStatus(directoryPath);

		for (FileStatus fileStatus : fileStatuses) {
			maxTS = Math.max(fileStatus.getModificationTime(), maxTS);
		}

		return new DateTime(maxTS);
	}

	@Override
	public DateTime getStatusUpdateTime(String identifier, int id, Status status) throws IOException {
		Preconditions.checkNotNull(identifier);
		Preconditions.checkNotNull(id);

		if (fs == null) {
			try {
				fs = FileSystem.get(new Configuration());
			} catch (IOException e) {
				logger.error(e);
				throw e;
			}
		}

		Path statusPath = new Path(getStatusPath(identifier, id), status.getString());

		return new DateTime(fs.getFileStatus(statusPath).getModificationTime());
	}

	@Override
	public int getCurrentIterNo(String identifier) throws IOException {
		Preconditions.checkNotNull(identifier);

		if (fs == null) {
			try {
				fs = FileSystem.get(new Configuration());
			} catch (IOException e) {
				logger.error(e);
				throw e;
			}
		}

		int maxIterNo = 0;

		Path directoryPath = new Path(getStatusPath(identifier));

		FileStatus[] fileStatuses = fs.listStatus(directoryPath);

		for (FileStatus fileStatus : fileStatuses) {
			int iterNo;
			try {
				iterNo = Integer.parseInt(fileStatus.getPath().getName());
			} catch (NumberFormatException e) {
				continue;
			}

			maxIterNo = Math.max(iterNo, maxIterNo);
		}

		return maxIterNo;
	}

	@Override
	public Status checkStatus(String identifier, int id) throws IOException {
		if (id == -1) {
			return Status.DEFAULT;
		}

		if (fs == null) {
			try {
				fs = FileSystem.get(new Configuration());
			} catch (IOException e) {
				logger.error(e);
				return Status.ERROR;
			}
		}

		Path successPath = new Path(getStatusPath(identifier, id), Status.SUCCESS.getString());

		if (fs.exists(successPath)) {
			return Status.SUCCESS;
		}

		Path progressPath = new Path(getStatusPath(identifier, id), Status.IN_PROGRESS.getString());

		if (fs.exists(progressPath)) {
			return Status.IN_PROGRESS;
		}

		Path errorPath = new Path(getStatusPath(identifier, id), Status.ERROR.getString());

		if (fs.exists(errorPath)) {
			return Status.ERROR;
		}

		return Status.DEFAULT;
	}

	@Override
	public void clearAllStatuses(String identifier, int jobId) throws IOException {
		Preconditions.checkNotNull(identifier);
		Preconditions.checkNotNull(jobId);

		if (fs == null) {
			try {
				fs = FileSystem.get(new Configuration());
			} catch (IOException e) {
				logger.error(e);
				return;
			}
		}

		Path directoryPath = new Path(getStatusPath(identifier, jobId));

		// Clear old status
		if (fs.exists(directoryPath)) {
			fs.delete(directoryPath, true);
		}
	}

	@Override
	public void removeStatus(String identifier, int jobId, Status status) throws IOException {
		Preconditions.checkNotNull(identifier);
		Preconditions.checkNotNull(jobId);

		if (fs == null) {
			try {
				fs = FileSystem.get(new Configuration());
			} catch (IOException e) {
				logger.error(e);
				return;
			}
		}

		Path statusPath = new Path(getStatusPath(identifier, jobId), status.getString());

		fs.delete(statusPath, true);
	}

	@Override
	public void commitStatus(String identifier, int jobId, Status status, DateTime time, boolean overwrite) throws IOException {
		Preconditions.checkNotNull(jobId);

		if (fs == null) {
			try {
				fs = FileSystem.get(new Configuration());
			} catch (IOException e) {
				logger.error(e);
				return;
			}
		}

		Path directoryPath = new Path(getStatusPath(identifier, jobId));

		// Clear old status
		if (overwrite && fs.exists(directoryPath)) {
			fs.delete(directoryPath, true);
		}

		// Commit new status
		Path path = new Path(directoryPath, status.getString());

		fs.createNewFile(path);
		fs.setTimes(path, time.toInstant().getMillis(), time.toInstant().getMillis());
		logger.debug("Created new status file: " + path.toUri());
	}
}