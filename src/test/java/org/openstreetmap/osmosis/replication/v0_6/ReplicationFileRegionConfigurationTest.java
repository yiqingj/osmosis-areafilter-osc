package org.openstreetmap.osmosis.replication.v0_6;

import java.io.InputStream;

import org.junit.Test;
import org.openstreetmap.osmosis.replication.v0_6.impl.ReplicationFileRegionConfiguration;

public class ReplicationFileRegionConfigurationTest {

	@Test
	public void testReplicationFileRegionConfiguration() {
		InputStream is = ReplicationFileRegionConfigurationTest.class.getResourceAsStream("/area-list.json");
		ReplicationFileRegionConfiguration config = new ReplicationFileRegionConfiguration(is);
		System.out.println(config);
	}

}
