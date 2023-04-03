package dev.breeze.settlements.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TestMain {

    public static void main(String[] args) {
        List<Integer> a = Arrays.asList(1, 2, 3);
        HashMap<List<Integer>, String> map = new HashMap<>();

        map.put(a, "owo");
        System.out.println(map.getOrDefault(Arrays.asList(1, 2, 3), "uwu"));
    }
}
