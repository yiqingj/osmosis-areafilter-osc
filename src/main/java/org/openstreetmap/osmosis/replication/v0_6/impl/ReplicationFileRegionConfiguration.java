package org.openstreetmap.osmosis.replication.v0_6.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;

public class ReplicationFileRegionConfiguration {

	private ArrayList<Region> regionList;

	public ArrayList<Region> getRegionList() {
		return regionList;
	}

	public ReplicationFileRegionConfiguration(File configfile) {
		FileReader reader;
		try {
			reader = new FileReader(configfile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			throw new OsmosisRuntimeException(
					"Unable to read the area configuration from file "
							+ configfile.getName() + ".", e);
		}
		init(reader);
	}

	public ReplicationFileRegionConfiguration(InputStream inputStream) {
		InputStreamReader reader = new InputStreamReader(inputStream);
		init(reader);
	}

	private void init(Reader reader) {
		regionList = new ArrayList<Region>();

		JSONArray areas = (JSONArray) JSONValue.parse(reader);
		for (Object obj : areas) {
			JSONObject regionJ = (JSONObject) obj;
			Region region = new Region();
			region.setName((String) regionJ.get("name"));
			region.setKey((String) regionJ.get("folder"));
			JSONObject boundJ = (JSONObject) regionJ.get("bound");

			String topLeft = (String) boundJ.get("top-left");
			String bottomRight = (String) boundJ.get("bottom-right");
			String[] values = topLeft.split(",| ");
			double top = Double.parseDouble(values[0]);
			double left = Double.parseDouble(values[1]);
			values = bottomRight.split(",| ");
			double bottom = Double.parseDouble(values[0]);
			double right = Double.parseDouble(values[1]);
			Bound bound = new Bound(right, left, top, bottom, "");
			region.setBound(bound);
			regionList.add(region);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Region region : regionList) {
			sb.append(region.toString());
			sb.append("\n");
		}
		return sb.toString();
	}

}
