package com.transitnet.rpdemo.service.route.algotithms.metrics.landmark;

import com.transitnet.rpdemo.model.IEdge;
import com.transitnet.rpdemo.model.IGraph;
import com.transitnet.rpdemo.model.INode;

import java.util.*;

/**
 * Implementation of a landmark provider that selects landmarks randomly.<br>
 * <br>
 * If the nodes of the graph do not provide {@link RandomAccess}, the selection
 * might need to iterate all nodes in order to select the random nodes. In this
 * case the landmark generation runs in <code>O(n)</code>, else only in the amount
 * of landmarks.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 * @param <N> Type of the nodes and landmarks
 * @param <G> Type of the graph
 */
public final class RandomLandmarks<N extends INode, G extends IGraph<N, ? extends IEdge<N>>>
    implements ILandmarkProvider<N> {
  /**
   * The graph to select landmarks from.
   */
  private final G mGraph;
  /**
   * The random number generator to use for generating the indices of the nodes
   * to select as landmarks.
   */
  private final Random mRandom;

  /**
   * Creates a new landmark provider that selects landmarks randomly.
   *
   * @param graph The graph to select landmarks from
   */
  public RandomLandmarks(final G graph) {
    mGraph = graph;
    mRandom = new Random();
  }

  /**
   * Selects landmarks randomly from the set of nodes.<br>
   * <br>
   * If the nodes of the graph do not provide {@link RandomAccess}, the
   * selection might need to iterate all nodes in order to select the random
   * nodes. In this case the landmark generation runs in <code>O(n)</code>, else
   * only in the amount of landmarks.
   */
  @Override
  public Collection<N> getLandmarks(final int amount) {
    if (amount <= 0) {
      return Collections.emptyList();
    }

    int amountToUse = amount;
    if (amount > mGraph.size()) {
      amountToUse = mGraph.size();
    }

    final Collection<N> landmarks = new ArrayList<>(amountToUse);
    final Collection<N> nodes = mGraph.getNodes();
    final int amountOfNodes = nodes.size();

    // Designate random indices
    final HashSet<Integer> indicesSet = new HashSet<>(amountToUse);
    while (indicesSet.size() < amountToUse) {
      indicesSet.add(mRandom.nextInt(amountOfNodes));
    }

    // If the nodes support RandomAccess, fetch them directly
    if (nodes instanceof RandomAccess && nodes instanceof List) {
      final List<N> nodesAsList = (List<N>) nodes;
      for (final int index : indicesSet) {
        landmarks.add(nodesAsList.get(index));
      }
      return landmarks;
    }

    // Sort the indices
    final int[] indices = indicesSet.stream().mapToInt(Integer::intValue).toArray();
    Arrays.sort(indices);

    // Iterate to each index and collect the node
    // This loop is optimized and faster than a straightforward approach because
    // it
    // has no conditional checks
    final Iterator<N> nodeIter = nodes.iterator();
    int indexBefore = 0;
    for (final int index : indices) {
      // Throw away the values
      for (int j = indexBefore; j < index; j++) {
        nodeIter.next();
      }

      // Collect the value, it is at the desired index
      landmarks.add(nodeIter.next());
      // Prepare the next round
      indexBefore = index + 1;
    }

    return landmarks;
  }

}
