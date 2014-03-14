// This software is released into the Public Domain.  See copying.txt for details.
package org.openstreetmap.osmosis.replication.v0_6;

import java.util.Map;

import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.filter.common.IdTracker;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSinkChangeSource;

/**
 * A base class for all tasks filter entities within an area.
 * 
 * @author Brett Henderson
 * @author Karl Newman
 */
public abstract class ChangeAreaFilter implements ChangeSinkChangeSource {

	private IdTracker availableNodes; // Nodes within the area.
	private IdTracker requiredNodes; // Nodes needed to complete referencing
										// entities.
	private IdTracker availableWays; // Ways within the area.
	private IdTracker requiredWays; // Ways needed to complete referencing
									// relations.
	private IdTracker availableRelations; // Relations within the area.
	private IdTracker requiredRelations; // Relations needed to complete
											// referencing relations.
	private boolean completeWays;
	private boolean storeEntities;
	private boolean cascadingRelations;
	private SimpleObjectStore<ChangeContainer> allWays;
	private SimpleObjectStore<ChangeContainer> allNodes;
	// this duplicates as a container for held-back relations in the
	// cascadingRelations case:
	private SimpleObjectStore<ChangeContainer> allRelations;

	private ChangeSink changeSink;

	/**
	 * Creates a new instance.
	 * 
	 * @param idTrackerType
	 *            Defines the id tracker implementation to use.
	 * @param clipIncompleteEntities
	 *            If true, entities referring to non-existent entities will be
	 *            modified to ensure referential integrity. For example, ways
	 *            will be modified to only include nodes inside the area.
	 * @param completeWays
	 *            Include all nodes for ways which have at least one node inside
	 *            the filtered area.
	 * @param completeRelations
	 *            Include all relations referenced by other relations which have
	 *            members inside the filtered area.
	 * @param cascadingRelations
	 *            Make sure that a relation referencing a relation which is
	 *            included will also be included.
	 */
	public ChangeAreaFilter(IdTrackerType idTrackerType,
			boolean clipIncompleteEntities, boolean completeWays,
			boolean completeRelations, boolean cascadingRelations) {
		// Allowing complete relations without complete ways is very difficult
		// and not allowed for
		// now.
		this.completeWays = completeWays || completeRelations;
		// cascadingRelations is included for free with any of the complete
		// options so you don't
		// need it if those are set.
		this.cascadingRelations = cascadingRelations && !completeRelations
				&& !completeWays;

		availableNodes = IdTrackerFactory.createInstance(idTrackerType);
		requiredNodes = IdTrackerFactory.createInstance(idTrackerType);
		availableWays = IdTrackerFactory.createInstance(idTrackerType);
		requiredWays = IdTrackerFactory.createInstance(idTrackerType);
		availableRelations = IdTrackerFactory.createInstance(idTrackerType);
		requiredRelations = IdTrackerFactory.createInstance(idTrackerType);

		// If either complete ways or complete relations are required, then all
		// data must be stored
		// during processing.
		storeEntities = completeWays || completeRelations;
		if (storeEntities) {
			allNodes = new SimpleObjectStore<ChangeContainer>(
					new SingleClassObjectSerializationFactory(
							ChangeContainer.class), "afn", true);
			allWays = new SimpleObjectStore<ChangeContainer>(
					new SingleClassObjectSerializationFactory(
							ChangeContainer.class), "afw", true);
			allRelations = new SimpleObjectStore<ChangeContainer>(
					new SingleClassObjectSerializationFactory(
							ChangeContainer.class), "afr", true);
		} else if (cascadingRelations) {
			allRelations = new SimpleObjectStore<ChangeContainer>(
					new SingleClassObjectSerializationFactory(
							ChangeContainer.class), "afr", true);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void initialize(Map<String, Object> metaData) {
		changeSink.initialize(metaData);
	}

	/**
	 * {@inheritDoc}
	 */
	public void process(ChangeContainer changeContainer) {
		// Ask the entity container to invoke the appropriate processing method
		// for the entity type.
		EntityContainer ec = changeContainer.getEntityContainer();
		if (ec instanceof NodeContainer) {
			processNodeChange(changeContainer);
		} else if (ec instanceof WayContainer) {
			processWayChange(changeContainer);
		} else if (ec instanceof RelationContainer) {
			processRelationChange(changeContainer);
		}
	}

	/**
	 * Indicates if the node lies within the area required.
	 * 
	 * @param node
	 *            The node to be checked.
	 * @return True if the node lies within the area.
	 */
	protected abstract boolean isNodeWithinArea(Node node);

	/**
	 * @param container
	 *            The container to be processed
	 */
	protected void processNodeChange(ChangeContainer container) {
		Node node;

		node = (Node) container.getEntityContainer().getEntity();

		// Check if we're storing entities for later.
		if (storeEntities) {
			allNodes.add(container);
		}

		// Only add the node if it lies within the box boundaries.
		if (isNodeWithinArea(node)) {
			availableNodes.set(node.getId());

			// If we're not storing entities, we pass it on immediately.
			if (!storeEntities) {
				emitChange(container);
			}
		}
	}

	/**
	 * @param container
	 *            The container to be processed.
	 */
	protected void processWayChange(ChangeContainer container) {
		Way way;
		boolean inArea;

		way = (Way) container.getEntityContainer().getEntity();

		// Check if we're storing entities for later.
		if (storeEntities) {
			allWays.add(container);
		}

		// First look through all the nodes to see if any are within the
		// filtered area
		inArea = false;
		for (WayNode nodeReference : way.getWayNodes()) {
			if (availableNodes.get(nodeReference.getNodeId())) {
				inArea = true;
				break;
			}
		}

		// If the way has at least one node in the filtered area.
		if (inArea) {
			availableWays.set(way.getId());

			// If complete ways are desired, mark any unavailable nodes as
			// required.
			if (completeWays) {
				for (WayNode nodeReference : way.getWayNodes()) {
					long nodeId = nodeReference.getNodeId();

					if (!availableNodes.get(nodeId)) {
						requiredNodes.set(nodeId);
					}
				}
			}

			// If we're not storing entities, we pass it on immediately.
			if (!storeEntities) {
				emitChange(container);
			}
		}
	}

	/**
	 * @param container
	 *            The container to be processed
	 */
	protected void processRelationChange(ChangeContainer container) {
		Relation relation;
		boolean inArea;
		boolean holdBackRelation;

		relation = (Relation) container.getEntityContainer().getEntity();

		// First look through all the node and way members to see if any are
		// within the filtered area
		inArea = false;
		holdBackRelation = false;

		for (RelationMember member : relation.getMembers()) {
			switch (member.getMemberType()) {
			case Node:
				inArea = availableNodes.get(member.getMemberId());
				break;
			case Way:
				inArea = availableWays.get(member.getMemberId());
				break;
			case Relation:
				inArea = availableRelations.get(member.getMemberId());
				break;
			default:
				break;
			}

			if (inArea) {
				break;
			}
		}

		if (cascadingRelations) { // && referencesOtherRelation && (!inArea ||
									// clipIncompleteEntities)) {
			holdBackRelation = true;
		}

		// Check if we're storing entities for later.
		if (storeEntities || holdBackRelation) {
			allRelations.add(container);
		}

		// If the relation has at least one member in the filtered area.
		if (inArea) {
			availableRelations.set(relation.getId());

			// If we're not storing entities, we pass it on immediately.
			if (!storeEntities && !holdBackRelation) {
				emitChange(container);
			}
		}
	}

	/**
	 * Sends a node to the sink. This will perform any necessary transformations
	 * on the node before sending it.
	 * 
	 * @param nodeContainer
	 *            Node to be sent.
	 */
	private void emitChange(ChangeContainer container) {
		changeSink.process(container);
	}

	private void pumpNodesToSink() {
		ReleasableIterator<ChangeContainer> i = allNodes.iterate();

		try {
			while (i.hasNext()) {
				ChangeContainer nodeContainer = i.next();
				if (availableNodes.get(nodeContainer.getEntityContainer()
						.getEntity().getId())) {
					emitChange(nodeContainer);
				}
			}

		} finally {
			i.release();
		}
	}

	private void pumpWaysToSink() {
		ReleasableIterator<ChangeContainer> i = allWays.iterate();

		try {
			while (i.hasNext()) {
				ChangeContainer wayContainer = i.next();
				if (availableWays.get(wayContainer.getEntityContainer()
						.getEntity().getId())) {
					emitChange(wayContainer);
				}
			}

		} finally {
			i.release();
		}
	}

	private void pumpRelationsToSink() {
		ReleasableIterator<ChangeContainer> i = allRelations.iterate();

		try {
			while (i.hasNext()) {
				ChangeContainer relationContainer = i.next();
				if (availableRelations.get(relationContainer
						.getEntityContainer().getEntity().getId())) {
					emitChange(relationContainer);
				}
			}

		} finally {
			i.release();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void complete() {
		// If we've stored entities temporarily, we now need to forward the
		// selected ones to the output.
		if (storeEntities) {
			// Select all parents of current relations.

			// Merge required ids into available ids.
			availableNodes.setAll(requiredNodes);
			availableWays.setAll(requiredWays);
			availableRelations.setAll(requiredRelations);
			requiredNodes = null;
			requiredWays = null;
			requiredRelations = null;

			// Send the selected entities to the output.
			pumpNodesToSink();
			pumpWaysToSink();
			pumpRelationsToSink();
		} else if (cascadingRelations) {
			// Select all parents of current relations.
			availableRelations.setAll(requiredRelations);
			// nodes, ways, and relations *not* referencing other relations will
			// already have
			// been written in this mode. we only pump the remaining ones,
			// relations that
			// reference other relations. this may result in an un-ordered
			// relation stream.
			pumpRelationsToSink();
		}

		changeSink.complete();
	}

	/**
	 * {@inheritDoc}
	 */
	public void release() {
		if (allNodes != null) {
			allNodes.release();
		}
		if (allWays != null) {
			allWays.release();
		}
		if (allRelations != null) {
			allRelations.release();
		}
		changeSink.release();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setChangeSink(ChangeSink changeSink) {
		this.changeSink = changeSink;
	}
}
