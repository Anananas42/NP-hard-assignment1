package astar.util;

import java.util.*;

// Used for bipartite deadlock detection
public class BipartiteMatcher {
    private final List<List<Integer>> edges;
    private final Map<Integer, Integer> pairV = new HashMap<>();
    private final int[] pairU;
    private int[] dist;

    public BipartiteMatcher(int n, List<Integer> V, List<List<Integer>> edges) {
        this.edges = edges;
        this.pairU = new int[n]; // Initialize pairings for U vertices
        Arrays.fill(pairU, -1); // Initialize all U vertices as unmatched

        V.forEach(v -> pairV.put(v, -1)); // Initialize pairings for V vertices as unmatched
    }

    private boolean bfs() {
        Queue<Integer> queue = new LinkedList<>();
        dist = new int[pairU.length];
        Arrays.fill(dist, Integer.MAX_VALUE); // Initialize distances as infinite

        for (int u = 0; u < pairU.length; u++) {
            if (pairU[u] == -1) {
                dist[u] = 0;
                queue.add(u);
            }
        }

        boolean isPath = false;
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : edges.get(u)) {
                if (!pairV.containsKey(v) || pairV.get(v) == -1) {
                    isPath = true;
                } else if (dist[pairV.get(v)] == Integer.MAX_VALUE) {
                    dist[pairV.get(v)] = dist[u] + 1;
                    queue.add(pairV.get(v));
                }
            }
        }
        return isPath;
    }

    private boolean dfs(int u) {
        for (int v : edges.get(u)) {
            if (!pairV.containsKey(v) || pairV.get(v) == -1 || (dist[pairV.get(v)] == dist[u] + 1 && dfs(pairV.get(v)))) {
                pairV.put(v, u);
                pairU[u] = v;
                return true;
            }
        }
        return false;
    }

    public int hopcroftKarp() {
        int matching = 0;
        while (bfs()) {
            for (int u = 0; u < pairU.length; u++) {
                if (pairU[u] == -1 && dfs(u)) {
                    matching++;
                }
            }
        }
        return matching;
    }

    public static void main(String[] args) {
        // Test Case 1: Perfect Matching Exists
        List<Integer> V1 = Arrays.asList(1, 2, 3, 4, 5);
        List<List<Integer>> edges1 = Arrays.asList(
            Arrays.asList(1, 2),    
            Arrays.asList(2, 5),
            Arrays.asList(3),
            Arrays.asList(5),
            Arrays.asList(3, 4)
        );
    
        BipartiteMatcher bm1 = new BipartiteMatcher(edges1.size(), V1, edges1);
        int matchingSize1 = bm1.hopcroftKarp();
        System.out.println("Test Case 1 - Maximum Matching Size: " + matchingSize1);
    
        if (edges1.size() == V1.size() && matchingSize1 == edges1.size()) {
            System.out.println("Test Case 1: Perfect matching exists.");
        } else {
            System.out.println("Test Case 1: Perfect matching does not exist.");
        }

        List<List<Integer>> edges2 = Arrays.asList(
            Arrays.asList(1, 2),    
            Arrays.asList(2, 5),
            Arrays.asList(3),
            Arrays.asList(4),
            Arrays.asList(3, 4)
        );

        BipartiteMatcher bm2 = new BipartiteMatcher(edges2.size(), V1, edges2);
        int matchingSize2 = bm2.hopcroftKarp();
        System.out.println("Test Case 2 - Maximum Matching Size: " + matchingSize2);
    
        if (edges2.size() == V1.size() && matchingSize2 == edges2.size()) {
            System.out.println("Test Case 2: Perfect matching exists.");
        } else {
            System.out.println("Test Case 2: Perfect matching does not exist.");
        }
    }
}
