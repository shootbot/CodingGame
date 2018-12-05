

import java.util.*;

class Player {
    static Map<Integer, Card> creatures = new HashMap<>();
    static Random rng = new Random();
    static int myHealth;
    static int oppHealth;
    static List<Card> units = new ArrayList<>();
    static List<Card> hand = new ArrayList<>();
    static List<Card> enemies = new ArrayList<>();
    static Scanner in;
    static String missions = "";
    static int myMana;
    static Map<Integer, Integer> manaCurve = new HashMap<>();
    static int pickCount = 0;
    static int dgCount = 0; // drainer or guardian count
    
    public static void main(String args[]) {
        
        init();
        
        while (true) {
            clear();
            readState();
            
            if (pickCount < 30) {
                int choice = getBestCard(hand);
                Card c = hand.get(choice);
                updateManacurve(c);
                if (c.drain || c.guard) {
                    dgCount++;
                }
                
                debug("Draft: " + hand.get(0) + hand.get(1) + hand.get(2));
                debug("Choice: " + choice + " " + c.getEffic());
                sendPick(choice);
                pickCount++;
            } else {
                makeBattleMove();
            }
        }
        
    }
    
    private static void sendPick(int choice) {
        System.out.println("PICK " + choice);
    }
    
    private static void updateManacurve(Card c) {
        int cost = c.cost;
        if (cost > 6) {
            cost = 6;
        }
        manaCurve.put(cost, manaCurve.get(cost) + 1);
    }
    
    private static void init() {
        in = new Scanner(System.in);
        for (int i = 0; i <= 12; i++) {
            manaCurve.put(i, 0);
        }
    }
    
    private static void makeBattleMove() {
        summonCreatures();
        useItems();
        if (hasGuards()) {
            killGuards();
        }
        if (canKillOpponent()) {
            killOpponent();
        } else {
            if (hasDrainers()) {
                killDrainers();
            }
            if (isWinningRace()) {
                killOpponent();
            } else {
                playDefense();
            }
        }
        killOpponent();
        doMissions();
    }
    
    private static void useItems() {
        debug("useItems myMana=" + myMana);
        for (Card c : hand) {
            debug(c.toString());
            if (myMana < c.cost) continue;
            if (c.cardType == 1) {
                
                if (units.size() == 0) continue;
                int unit;
                if (getDrainEnemy(units) >= 0) {
                    unit = getDrainEnemy(units);
                } else {
                    int random = rng.nextInt(units.size());
                    unit = units.get(random).instanceId;
                }
                missions += "USE " + c.instanceId + " " + unit + ";";
                debug("USE " + c.instanceId + " " + unit);
            }
            if (c.cardType == 2) {
                
                if (enemies.size() > 0) {
                    enemies.sort((o1, o2) -> {
//                            return o1.cost + o1.defense > o2.cost + o2.defense ? -1 : 1;
                        return o1.drain ? -1 : 1;
                    });
                    missions += "USE " + c.instanceId + " " + enemies.get(0).instanceId + ";";
                    debug("USE " + c.instanceId + " " + enemies.get(0).instanceId);
                }
            }
            if (c.cardType == 3) {
                missions += "USE " + c.instanceId + " -1;";
                debug("USE " + c.instanceId + " -1");
            }
        }
    }
    
    private static void playDefense() {
        for (Card c : units) {
            if (c.attack <= 0 || (c.guard && !c.charge)) continue;
            int randomTarget = rng.nextInt(enemies.size());
            missions += "ATTACK " + c.instanceId + " " + enemies.get(randomTarget).instanceId + ";";
        }
        units.clear();
    }
    
    private static boolean isWinningRace() {
        int myPower = 0;
        for (Card c : units) {
            myPower += c.attack;
        }
        int oppPower = 0;
        for (Card c : enemies) {
            oppPower += c.attack;
        }
        int oppTurns = (int) Math.ceil((double) myHealth / oppPower);
        int myTurns = (int) Math.ceil((double) oppHealth / myPower);
        return myTurns <= oppTurns;
    }
    
    private static boolean hasDrainers() {
        return getDrainEnemy(enemies) >= 0;
    }
    
    private static boolean canKillOpponent() {
        int sumPower = 0;
        for (Card c : units) {
            sumPower += c.attack;
        }
        return oppHealth <= sumPower;
    }
    
    private static void killGuards() {
        while (hasGuards()) {
            int guardId = getGuardEnemy(enemies);
            Card guard = getById(guardId);
            int gDefense = guard.defense;
            
            if (units.size() == 0) break;
            
            List<Card> toRemove = new ArrayList<>();
            for (Card c : units) {
                if (gDefense > 0) {
                    if (c.attack != 0) {
                        missions += "ATTACK " + c.instanceId + " " + guardId + ";";
                        if (guard.ward) {
                            guard.ward = false;
                        } else {
                            gDefense -= c.attack;
                        }
                    }
                    toRemove.add(c);
                } else {
                    break;
                }
            }
            if (gDefense <= 0) {
                remove(enemies, guardId);
            }
            units.removeAll(toRemove);
        }
    }
    
    private static void killDrainers() {
        int drainId = getDrainEnemy(enemies);
        int bestDrainFit = getBestFit(drainId, units);
        if (bestDrainFit != -1) {
            missions += "ATTACK " + bestDrainFit + " " + drainId + ";";
            remove(enemies, drainId);
            remove(units, bestDrainFit);
        } else {
            while (hasDrainers()) {
                int drainerId = getDrainEnemy(enemies);
                Card drainer = getById(drainerId);
                int dDefense = drainer.defense;
                
                if (units.size() == 0) break;
                
                List<Card> toRemove = new ArrayList<>();
                for (Card c : units) {
                    if (dDefense > 0) {
                        if (c.attack != 0) {
                            missions += "ATTACK " + c.instanceId + " " + drainerId + ";";
                            if (drainer.ward) {
                                drainer.ward = false;
                            } else {
                                dDefense -= c.attack;
                            }
                        }
                        toRemove.add(c);
                    } else {
                        break;
                    }
                }
                if (dDefense <= 0) {
                    remove(enemies, drainerId);
                }
                units.removeAll(toRemove);
            }
        }
    }
    
    private static List<Move> genMoves() {
        List<Move> res = new ArrayList<>();
        
        for (Card c : units) {
            for (Card e: enemies) {
                res.add(new Move(c.instanceId, e.instanceId));
            }
        }
        
        return res;
    }
    
    private static Move getMaxMove(List<Move> s) {
        int ev = 0;
        for (Move m : s) {
            ev += getMoveValue(m);
        }
        
        
        return null;
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
    
    private static void remove(List<Card> list, int id) {
        for (int i = 0; i < list.size(); i++) {
            Card c = list.get(i);
            if (c.instanceId == id) {
                list.remove(i);
                break;
            }
        }
    }
    
    private static void killOpponent() {
        for (Card c : units) {
            missions += "ATTACK " + c.instanceId + " -1;";
        }
        units.clear();
    }
    
    private static void doMissions() {
        System.out.println(missions);
    }
    
    private static boolean hasGuards() {
        return getGuardEnemy(enemies) >= 0;
    }
    
    private static void summonCreatures() {
        List<Card> toRemove = new ArrayList<>();
        hand.sort((o1, o2) -> o1.cost > o2.cost ? -1 : 1);
        if (!isWinningRace()) {
            for (Card c : hand) {
                if (myMana < 0) break;
                
                if (c.charge || c.guard) {
                    missions += "SUMMON " + c.instanceId + ";";
                    if (myMana >= c.cost) {
                        myMana -= c.cost;
                        toRemove.add(c);
                        if (c.charge) {
                            units.add(c);
                        }
                    }
                }
            }
        }
        hand.removeAll(toRemove);
        toRemove.clear();
        for (Card c : hand) {
            if (myMana < 0) break;
            
            missions += "SUMMON " + c.instanceId + ";";
            if (myMana >= c.cost) {
                myMana -= c.cost;
                toRemove.add(c);
                if (c.charge) {
                    units.add(c);
                }
            }
        }
        hand.removeAll(toRemove);
    }
    
    static void clear() {
        units.clear();
        hand.clear();
        enemies.clear();
        creatures.clear();
        missions = "";
    }
    
    private static void readState() {
        myHealth = in.nextInt();
        myMana = in.nextInt();
        int myDeck = in.nextInt();
        int myRune = in.nextInt();
        int myDraw = in.nextInt();
        
        oppHealth = in.nextInt();
        int oppMana = in.nextInt();
        int oppDeck = in.nextInt();
        int oppRune = in.nextInt();
        int oppDraw = in.nextInt();
        
        int opponentHand = in.nextInt();
        int opponentActions = in.nextInt();
        if (in.hasNextLine()) {
            in.nextLine();
        }
        for (int i = 0; i < opponentActions; i++) {
            String cardNumberAndAction = in.nextLine();
            debug("opp: " + cardNumberAndAction);
        }
        
        int cardCount = in.nextInt();
        for (int i = 0; i < cardCount; i++) {
            int cardNumber = in.nextInt();
            int instanceId = in.nextInt();
            int location = in.nextInt();
            int cardType = in.nextInt();
            int cost = in.nextInt();
            int attack = in.nextInt();
            int defense = in.nextInt();
            String abilities = in.next();
            int myHealthChange = in.nextInt();
            int opponentHealthChange = in.nextInt();
            int cardDraw = in.nextInt();
            
            Card c = new Card(
                    attack,
                    defense,
                    cost,
                    instanceId,
                    myHealthChange,
                    opponentHealthChange,
                    cardDraw,
                    abilities,
                    cardType,
                    cardNumber);
            
            creatures.put(instanceId, c);
            if (location == 0) {
                hand.add(c);
            }
            if (location == -1) {
                enemies.add(c);
            }
            if (location == 1) {
                units.add(c);
            }
        }
    }
    
    
    private static Card getById(int instanceId) {
        return creatures.get(instanceId);
    }
    
    private static int getGuardEnemy(List<Card> enemies) {
        for (Card c : enemies) {
            if (c.guard) {
                return c.instanceId;
            }
        }
        return -1;
    }
    
    private static int getDrainEnemy(List<Card> enemies) {
        for (Card c : enemies) {
            if (c.drain) {
                return c.instanceId;
            }
        }
        return -1;
    }
    
    private static int getBestFit(int enemyId, List<Card> units) {
        if (enemyId == -1) return -1;
        
        List<Card> list = new ArrayList<>();
        
        for (Card c : units) {
            if (c.attack == getById(enemyId).defense) {
                list.add(c);
            }
        }
        if (list.size() == 0) {
            return -1;
        }
        
        Card min = list.get(0);
        int minId = min.instanceId;
        for (int i = 0; i < list.size(); i++) {
            Card c = list.get(i);
            if (c.defense < min.defense) {
                min = c;
                minId = c.instanceId;
            }
        }
        
        return minId;
    }
    
    static int getBestCard(List<Card> list) {
        Card c0 = list.get(0);
        Card c1 = list.get(1);
        Card c2 = list.get(2);
        
        if (c0.compareTo(c1) >= 0 && c0.compareTo(c2) >= 0) return 0;
        if (c1.compareTo(c2) >= 0 && c1.compareTo(c0) >= 0) return 1;
        return 2;
    }
    
    static void debug(String s) {
        System.err.println(s);
    }
}

class Card implements Comparable {
    int attack;
    int defense;
    int cost;
    int instanceId;
    int myHealthChange;
    int opponentHealthChange;
    int cardDraw;
    boolean guard;
    boolean breakthrough;
    boolean charge;
    boolean lethal;
    boolean drain;
    boolean ward;
    int cardType;
    int cardNumber;
    
    double getEffic() {
        double res = 0;
        if (cardType != 0) res += -303;
        if (cost == 0) res -= 500;
        
        res += ((double)
                (attack < 2 ? attack - 2 : attack)
                + defense
                + myHealthChange
                - opponentHealthChange
                + 3 * cardDraw
                + (guard ? 2 : 0)
                + (breakthrough ? 1 : 0)
                + (charge ? attack : 0)
                + (drain ? attack * 2 : 0)
                + (lethal ? 1 : 0)
                + (ward ? (guard ? attack : 1) : 0)
        ) / (cost < 3 ? cost + 1 : cost);
        
        if (cost >= 6 && Player.manaCurve.get(6) >= 4) {
            res -= 0.5;
        }
        if (cost <= 3 && Player.manaCurve.get(6) >= 7) {
            res += 1;
        }

//        if ((double) Player.dgCount / Player.pickCount > 0.5) {
//            if (drain || guard) {
//                res += 1;
//            }
//            if (cardType == 1) {
//                res = (Math.sqrt(attack * defense) + cardDraw) / cost;
//            }
//        }
        
        return res;
    }
    
    public Card(
            int attack,
            int defense,
            int cost,
            int instanceId,
            int myHealthChange,
            int opponentHealthChange,
            int cardDraw,
            String abilities,
            int cardType,
            int cardNumber) {
        this.attack = attack;
        this.defense = defense;
        this.cost = cost;
        this.instanceId = instanceId;
        this.myHealthChange = myHealthChange;
        this.opponentHealthChange = opponentHealthChange;
        this.cardDraw = cardDraw;
        this.cardType = cardType;
        this.cardNumber = cardNumber;
        
        guard = abilities.contains("G");
        breakthrough = abilities.contains("B");
        charge = abilities.contains("C");
        drain = abilities.contains("D");
        lethal = abilities.contains("L");
        ward = abilities.contains("W");
    }
    
    public int compareTo(Object o) {
        Card other = (Card) o;
        return Double.compare(getEffic(), other.getEffic());
    }
    
    @Override
    public String toString() {
        return "Card{number=" + cardNumber
                + ", cost=" + cost
                + ", type=" + cardType
                + ", attack=" + attack
                + ", def=" + defense
                + ", draw=" + cardDraw + "}";
        
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card that = (Card) o;
        return instanceId == that.instanceId;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(instanceId);
    }
}

class Move {
    int attackerId;
    int defenderId;
    
    public Move(int attackerId, int defenderId) {
        this.attackerId = attackerId;
        this.defenderId = defenderId;
    }
}
