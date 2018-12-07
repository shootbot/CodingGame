import java.util.*;
import java.util.stream.*;

public class Sim {
    Map<Integer, Card> world;
    int myHp;
    int oppHp;
    
    public Sim() {
        world = Minimax.all.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> new Card((Card) e.getValue())));
        myHp = Minimax.myHp;
        oppHp = Minimax.oppHp;
    }
    
    Card getById(int instanceId) {
        return world.get(instanceId);
    }
}
