// This software is released into the Public Domain.  See copying.txt for details.
package org.openstreetmap.osmosis.replication;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.plugin.PluginLoader;
import org.openstreetmap.osmosis.replication.v0_6.ChangeBoundingBoxFilterFactory;
import org.openstreetmap.osmosis.replication.v0_6.ReplicationFileSpliterInitializerFactory;
import org.openstreetmap.osmosis.replication.v0_6.ReplicationFileRegionSpliterFactory;

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
		factoryMap.put("split-replication-files",
				new ReplicationFileRegionSpliterFactory());
		factoryMap.put("srf", new ReplicationFileRegionSpliterFactory());
		factoryMap.put("split-replication-files-init",
				new ReplicationFileSpliterInitializerFactory());
		factoryMap.put("srfi", new ReplicationFileSpliterInitializerFactory());
		return factoryMap;
	}
}
