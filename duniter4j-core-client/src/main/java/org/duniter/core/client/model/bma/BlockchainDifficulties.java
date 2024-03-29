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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.ArrayUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@FieldNameConstants
public class BlockchainDifficulties implements Serializable {
	private static final long serialVersionUID = -5631089862715942431L;

	private Long block;
	private DifficultyLevel[] levels;

	@Data
	@FieldNameConstants
	public static class DifficultyLevel implements Serializable {
		private static final long serialVersionUID = 1L;

		private String uid;
		private int level;
	}

	@JsonIgnore
	public Map<String, Integer> toMapByUid() {
		return toMapByUid(getLevels());
	}

	public static Map<String, Integer> toMapByUid(DifficultyLevel[] levels) {
		if (ArrayUtils.isEmpty(levels)) return null;
		return Stream.of(levels).collect(Collectors.toMap(d -> d.getUid(), d -> d.getLevel()));
	}
}
