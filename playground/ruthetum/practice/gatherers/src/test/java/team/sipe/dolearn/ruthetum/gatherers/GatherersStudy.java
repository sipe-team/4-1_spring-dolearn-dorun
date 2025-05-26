package team.sipe.dolearn.ruthetum.gatherers;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Gatherers;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GatherersStudy {

    @Test
    public void fold() {
        // given
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        // when
        List<String> list = numbers.stream()
                .gather(Gatherers.fold(() -> "", (string, number) -> string + number))
                .toList();

        // then
        assertEquals(List.of("12345"), list);
    }

    @Test
    public void scan() {
        // given
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        // when
        List<Integer> list = numbers.stream()
                .gather(Gatherers.scan(() -> 4, Integer::sum))
                .toList();

        // then
        assertEquals(List.of(4, 5, 7, 10, 14), list);
    }

    @Test
    public void windowFixed() {
        // given
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        // when
        List<List<Integer>> windows = numbers.stream()
                .gather(Gatherers.windowFixed(2))
                .toList();

        // then
        assertEquals(List.of(List.of(1, 2), List.of(3, 4), List.of(5)), windows);
    }

    @Test
    public void windowSliding() {
        // given
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        // when
        List<List<Integer>> windows = numbers.stream()
                .gather(Gatherers.windowSliding(2))
                .toList();

        // then
        assertEquals(List.of(List.of(1, 2), List.of(2, 3), List.of(3, 4), List.of(4, 5)), windows);
    }

    @Test
    public void mapConcurrent() {
        // given
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        // when
        List<Integer> list = numbers.stream()
                .gather(Gatherers.mapConcurrent(1, number -> number * 2))
                .toList();

        // then
        assertEquals(List.of(2, 4, 6, 8, 10), list);
    }
}
