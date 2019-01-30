package org.duniter.core.client.model.bma;

/*
 * #%L
 * UCoin Java :: Core Client API
 * %%
 * Copyright (C) 2014 - 2016 EIS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.io.Serializable;

public class BlockchainDifficulties implements Serializable {
	private static final long serialVersionUID = -5631089862715942431L;

	private Long block;
	private DifficultyLevel[] levels;

	public Long getBlock() {
		return block;
	}
	public void setBlock(Long block) {
		this.block = block;
	}
	public DifficultyLevel[] getLevels() {
		return levels;
	}
	public void setLevels(DifficultyLevel[] levels) {
		this.levels = levels;
	}

	public static class DifficultyLevel implements Serializable {
		private static final long serialVersionUID = 1L;

		private String uid;
		private int level;

		public String getUid() {
			return uid;
		}

		public void setUid(String uid) {
			this.uid = uid;
		}

		public int getLevel() {
			return level;
		}

		public void setLevel(int level) {
			this.level = level;
		}
	}
}
