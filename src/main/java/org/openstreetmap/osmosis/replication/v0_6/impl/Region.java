package org.openstreetmap.osmosis.replication.v0_6.impl;

import org.openstreetmap.osmosis.core.domain.v0_6.Bound;

public class Region {
	private String name;

	/**
	 * re-using the core Bound object, it has all required attributes but it's
	 * concept is a little different, the origin is not needed.
	 */
	private Bound bound;
	private String key;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Bound getBound() {
		return bound;
	}

	public void setBound(Bound bound) {
		this.bound = bound;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("name: ").append(getName());
		sb.append(" | folder: ").append(getKey());
		sb.append(" | ").append(getBound());
		return sb.toString();
	}

}
