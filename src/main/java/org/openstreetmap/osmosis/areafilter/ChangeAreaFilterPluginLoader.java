// This software is released into the Public Domain.  See copying.txt for details.
package org.openstreetmap.osmosis.areafilter;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osmosis.areafilter.v0_6.ChangeBoundingBoxFilterFactory;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.plugin.PluginLoader;

/**
 * The plugin loader for the API Schema tasks.
 * 
 * @author Brett Henderson
 */
public class ChangeAreaFilterPluginLoader implements PluginLoader {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, TaskManagerFactory> loadTaskFactories() {
		Map<String, TaskManagerFactory> factoryMap;

		factoryMap = new HashMap<String, TaskManagerFactory>();

		factoryMap.put("bounding-box-change",
				new ChangeBoundingBoxFilterFactory());
		factoryMap.put("bbc", new ChangeBoundingBoxFilterFactory());
		return factoryMap;
	}
}
