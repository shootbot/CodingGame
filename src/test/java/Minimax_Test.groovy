import org.junit.jupiter.api.*

public class Minimax_Test {
    @Test
    void test() {
        def units = genUnits(5)
        def enemies = genUnits(5)

        def myHp = rng.nextInt(20) + 10
        def oppHp = rng.nextInt(30) + 10
        def mx = new Minimax(myHp, oppHp, units, enemies)

        def start = System.nanoTime();
        def res = mx.getPlan()
        def end = System.nanoTime();
        println (myHp + " "  + oppHp)
        println units
        println enemies
        println("time: " + (end - start) / 1000_000 + "ms")
        println(res)
        //== "ATTACK 1 2;ATTACK 3 6;"
    }

    def idCounter = 1
    def rng = new Random()

    def genUnits(int n) {
        def list = []
        for (int i = 0; i < n; i++) {
            def unit = [
                    id : idCounter++,
                    pow: rng.nextInt(12),
                    tou: rng.nextInt(11) + 1
            ]
            list.add(unit)
        }
        return list
    }

}
