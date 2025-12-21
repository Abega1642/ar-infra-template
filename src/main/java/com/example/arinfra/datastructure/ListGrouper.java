package com.example.arinfra.datastructure;

import static java.lang.Math.min;

import com.example.arinfra.InfraGenerated;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

/**
 * Utility component for partitioning lists into smaller batches.
 *
 * <p>This generic utility splits a list into multiple sublists (batches) of a specified maximum
 * size. The last batch may contain fewer elements if the total list size is not evenly divisible by
 * the group size.
 *
 * <p>This component is primarily used for batch processing of events in the messaging
 * infrastructure, allowing large collections to be processed in manageable chunks.
 *
 * <p><b>Example usage:</b>
 *
 * <pre>{@code
 * ListGrouper<String> grouper = new ListGrouper<>();
 * List<String> items = List.of("A", "B", "C", "D", "E");
 * List<List<String>> batches = grouper.apply(items, 2);
 * // Result: [[A, B], [C, D], [E]]
 * }</pre>
 *
 * <p>Implements {@link BiFunction} to allow functional-style composition and integration with
 * stream pipelines.
 *
 * @param <T> the type of elements in the list to be grouped
 */
@InfraGenerated
@Component
public class ListGrouper<T> implements BiFunction<List<T>, Integer, List<List<T>>> {

  /**
   * Partitions a list into batches of the specified size.
   *
   * <p>Creates a list of sublists where each sublist contains at most {@code groupSize} elements.
   * The original list is processed sequentially, and elements maintain their relative order in the
   * resulting batches.
   *
   * <p><b>Behavior:</b>
   *
   * <ul>
   *   <li>If the list size is evenly divisible by groupSize, all batches have equal size
   *   <li>The last batch may contain fewer elements than groupSize
   *   <li>Returns an empty list if the input list is empty
   *   <li>Each batch is a new ArrayList, independent of the original list
   * </ul>
   *
   * @param list the list to partition into batches (non-null)
   * @param groupSize the maximum number of elements per batch (must be positive)
   * @return a list of batches, where each batch is a list containing at most groupSize elements
   * @throws IllegalArgumentException if groupSize is less than or equal to 0
   */
  @Override
  public List<List<T>> apply(List<T> list, Integer groupSize) {
    List<List<T>> groupedList = new ArrayList<>();
    int size = list.size();

    for (int i = 0; i < size; i += groupSize) {
      int end = min(size, i + groupSize);
      groupedList.add(new ArrayList<>(list.subList(i, end)));
    }

    return groupedList;
  }
}
