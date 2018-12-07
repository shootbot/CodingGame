import java.util.*;

public class Minimax {
    static List<Card> units = new ArrayList<>();
    static List<Card> enemies = new ArrayList<>();
    static Map<Integer, Card> all = new HashMap<>();
    static final int ENEMY = -1;
    static final int NOTHING = 0;
    static final int WIN_EV = 1000;
    static List<int[]> plans = new ArrayList<>();
    static int myHp;
    static int oppHp;
    
    
    public Minimax(int myHp, int oppHp, List<Map<String, Object>> units, List<Map<String, Object>> enemies) {
        for (Map<String, Object> unit : units) {
            Card c = new Card((Integer) unit.get("id"), (Integer) unit.get("pow"), (Integer) unit.get("tou"));
            this.units.add(c);
            all.put(c.instanceId, c);
        }
        for (Map<String, Object> e : enemies) {
            Card c = new Card((Integer) e.get("id"), (Integer) e.get("pow"), (Integer) e.get("tou"));
            this.enemies.add(c);
            all.put(c.instanceId, c);
        }
        this.myHp = myHp;
        this.oppHp = oppHp;
        
        
    }
    
    public static String getPlan() {
        List<int[]> perm = getPermutations();
        plans = genMoves(perm);
        int[] maxPlan = getMaxPlan();
        return buildMissions(maxPlan);
    }
    
    private static List<int[]> genMoves(List<int[]> perms) {
        int depth = units.size();
        int[] plan = new int[depth * 2];
        for (int[] perm : perms) {
            recur(perm, plan, depth);
        }
        return plans;
    }
    
    public static void recur(int[] perm, int[] plan, int depth) {
        if (depth == 0) {
            plans.add(Arrays.copyOf(plan, plan.length));
//            System.out.println(Arrays.toString(plan));
            return;
        }
        for (int target = -1; target <= enemies.size(); target++) {
            int att = units.size() - depth;
            plan[att * 2] = perm[att];
            plan[att * 2 + 1] = target <= 0 ? target : enemies.get(target - 1).instanceId;
            recur(perm, plan, depth - 1);
        }
    }
    
    private static int[] getMaxPlan() {
        int maxEv = Integer.MIN_VALUE;
        int maxI = -1;
        for (int i = 0; i < plans.size(); i++) {
            int ev = getEv(plans.get(i));
            if (ev >= maxEv) {
                maxEv = ev;
                maxI = i;
            }
        }
        int[] maxPlan = plans.get(maxI);
        return maxPlan;
    }
    
    // ev += points
    private static int getEv(int[] plan) {
        Sim sim = new Sim();
        int ev = 0;
        for (int i = 0; i < plan.length - 1; i += 2) {
            Card a = sim.getById(plan[i]);
            
            if (plan[i + 1] == ENEMY) {
                ev += a.attack;
                sim.oppHp -= a.attack;
                if (sim.oppHp <= 0) {
                    ev += WIN_EV;
                }
            } else if (plan[i + 1] == NOTHING) {
                ev += 0;
            } else {
                Card d = sim.getById(plan[i + 1]);
                if (d.defense <= 0) continue;
                
                if (d.defense <= a.attack) {
                    if (a.breakthrough) {
                        int breakthrough = d.defense - a.attack;
                        sim.oppHp -= breakthrough;
                        if (sim.oppHp <= 0) {
                            ev += WIN_EV;
                        } else {
                            ev += breakthrough;
                        }
                    }
                    ev += d.defense;
                    ev += d.attack;
                    ev++; // bounty
                    d.defense = 0;
                } else {
                    ev += a.attack;
                    d.defense -= a.attack;
                }
                
                if (d.attack >= a.defense) {
                    
                    ev -= a.defense;
                    ev -= a.attack;
                    a.defense = 0;
                } else {
                    ev -= d.attack;
                    a.defense -= d.attack;
                }
            }
        }
        
        return ev;
    }
    
    private static String buildMissions(int[] plan) {
        if (plan.length % 2 != 0) {
            throw new IllegalArgumentException("Количество элементов в плане нечетно");
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < plan.length - 1; i += 2) {
            sb.append("ATTACK ").append(plan[i]).append(' ').append(plan[i + 1]).append(';');
        }
        return sb.toString();
    }
    
    public static List<int[]> getPermutations() {
        List<int[]> res = new ArrayList<>();
        
        int[] arr = units.stream()
                .map(c -> c.instanceId)
                .mapToInt(x -> x)
                .toArray();
        permutation(arr, arr.length, arr.length, res);
        
        return res;
    }
    
    private static void printArr(int a[], int n) {
        for (int i = 0; i < n; i++) {
            System.out.print(a[i] + " ");
        }
        System.out.println();
    }
    
    // Heap’s Algorithm for generating permutations
    private static void permutation(int a[], int size, int n, List<int[]> res) {
        if (size == 1) {
//            printArr(a, n);
            res.add(Arrays.copyOf(a, a.length));
        }
        
        for (int i = 0; i < size; i++) {
            permutation(a, size - 1, n, res);
            
            if (size % 2 == 1) {
                int temp = a[0];
                a[0] = a[size - 1];
                a[size - 1] = temp;
            } else {
                int temp = a[i];
                a[i] = a[size - 1];
                a[size - 1] = temp;
            }
        }
    }
    
    
    private static Card getById(int id) {
        return all.get(id);
    }
    
    private static int getMoveValue(Move m) {
        Card att = getById(m.attackerId);
        Card def = getById(m.defenderId);
        int BOUNTY = 1;
        int ev = 0;
        
        if (att.attack >= def.defense) {
            ev += def.attack + def.defense + BOUNTY;
        } else {
            ev += att.attack;
        }
        
        if (def.attack >= att.defense) {
            ev -= att.attack + att.defense + BOUNTY;
        } else {
            ev -= def.attack;
        }
        
        if (att.drain) {
            ev += att.attack / 2;
        }
        
        return ev;
    }
}
