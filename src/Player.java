import java.util.*;
import java.util.stream.Collectors;

class Player {

    public static void main(String args[]) {
        new Player().solve();
    }

    private int distance[][];
    private int shortcut[][];

    private void solve() {
        try (Scanner in = new Scanner(System.in)) {

            int n = in.nextInt();
            distance = new int[n][n];
            shortcut = new int[n][n];
            for (int i = 0; i < n; ++i) {
                distance[i][i] = 0xffff;
            }
            for (int i = 0, linkCount = in.nextInt(); i < linkCount; i++) {
                int f1 = in.nextInt();
                int f2 = in.nextInt();
                int d = in.nextInt() + 1;
                distance[f1][f2] = distance[f2][f1] = d;
            }
            int bomb = 2;
            boolean bombed[] = new boolean[n];

            for (int k = 0; k < n; ++k) {
                for (int i = 0; i < n; ++i) {
                    for (int j = 0; j < n; ++j) {
                        distance[i][j] = Math.min(distance[i][j], distance[i][k] + distance[k][j]);
                    }
                }
            }
            for (int i = 0; i < n; ++i) {
                for (int j = 0; j < n; ++j) {
                    int x = j;
                    for (int k = 0; k < n; ++k) {
                        if (distance[i][j] == distance[i][k] + distance[k][j] && distance[i][k] < distance[i][x]) {
                            x = k;
                        }
                    }
                    shortcut[i][j] = x;
                }
            }

            State state = new State();
            List<Order> orders = new ArrayList<>();
            while (true) {
                state.input(in);
                orders.clear();

                state.otherFactories = state.otherFactories.stream().filter(x -> x.result != Owner.aly).collect(Collectors.toList());

                for (Factory lose : state.allyFactories.stream().filter(x -> x.reserve < 0 && x.production > 0).collect(Collectors.toList())) {
                    int reserve = -lose.reserve;
                    Collections.sort(state.allyFactories, (a, b) -> distance[lose.id][a.id] - distance[lose.id][b.id]);
                    for (Factory s : state.allyFactories) {
                        if (s.reserve < 1) continue;
                        int send = Math.min(s.reserve, reserve);
                        orders.add(new Move(s.id, shortcut[s.id][lose.id], send));
                        s.reserve -= send;
                        reserve -= send;
                        if (reserve <= 0) break;
                    }
                }

                while (bomb > 0) {
                    int max_production = state.factories.stream().mapToInt(x -> x.production).max().getAsInt();
                    Optional<Factory> target = state.otherFactories.stream().filter(x -> !bombed[x.id] && x.owner == Owner.opp && x.result == Owner.opp && x.production == max_production).findFirst();
                    if (target.isPresent()) {
                        Optional<Factory> source = state.allyFactories.stream().sorted((a, b) -> distance[target.get().id][a.id] - distance[target.get().id][b.id]).findFirst();
                        if (source.isPresent()) {
                            orders.add(new SendBomb(source.get().id, target.get().id));
                            bombed[target.get().id] = true;
                            --bomb;
                        }
                    } else break;
                }

                for (Factory s : state.allyFactories) {
                    if (s.near == null) {
                        int d[] = distance[s.id];
                        Collections.sort(state.otherFactories, (a, b) -> {
                            int av = (a.owner == Owner.neutral ? 20 : 10) * a.production - a.cyborgs + a.alyCyb[9] - a.oppCyb[9] - d[a.id] * 5;
                            int bv = (b.owner == Owner.neutral ? 20 : 10) * b.production - b.cyborgs + b.alyCyb[9] - b.oppCyb[9] - d[b.id] * 5;
                            return bv - av;
                        });
                        for (Factory t : state.otherFactories) {
                            int tt = d[t.id] < 9 ? d[t.id] : 9;
                            int valid = t.cyborgs - t.alyCyb[tt];
                            if (t.owner != Owner.neutral && !t.isBomb) valid += +t.oppCyb[tt] + t.production * d[t.id];
                            if (valid < 0) continue;
                            if (s.reserve < 1 || s.reserve <= valid) {
                                break;
                            }
                            int send = t.owner == Owner.neutral ? valid + 1 : s.reserve;
                            orders.add(new Move(s.id, shortcut[s.id][t.id], send));
                            s.reserve -= send;
                            for (int i = tt; i < 10; ++i) {
                                t.alyCyb[i] += send;
                            }
                        }
                    }
                    if (s.reserve >= 10 && s.production < 3) {
                        orders.add(new Inc(s));
                        s.reserve -= 10;
                    }
                    if (s.reserve >= 15) {
                        state.factories.stream().sorted((a, b) -> (a.ownDist - a.oppDist) - (b.ownDist - b.oppDist)).filter(x -> x.ownDist <= x.oppDist && x.production == 0 && x.reserve + x.alyCyb[9] < 10).findFirst().ifPresent(x -> {
                            int send = 15;
                            orders.add(new Move(s.id, x.id, send));
                            s.reserve -= send;
                        });
                    }
                    if (s.near != null && (s.production == 3 || s.production == 0) && s.reserve > 0) {
                        orders.add(new Move(s.id, shortcut[s.id][s.near.id], s.reserve));
                        s.reserve = 0;
                    }
                }
                System.out.println(orders.isEmpty() ? "WAIT" : orders.stream().map(x -> x.toString()).collect(Collectors.joining(";")));
            }
        }
    }

    abstract class Order {
        abstract public String toString();
    }

    class Move extends Order {
        final int from;

        final int to;

        final int cyborgs;

        Move(int from, int to, int cyborgs) {
            if (cyborgs < 1) throw new RuntimeException();
            this.from = from;
            this.to = to;
            this.cyborgs = cyborgs;
        }

        @Override
        public String toString() {
            return "MOVE " + from + " " + to + " " + cyborgs;
        }
    }

    class SendBomb extends Order {
        final int from;

        final int to;

        SendBomb(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            return "BOMB " + from + " " + to;
        }
    }

    class Inc extends Order {
        final int id;

        Inc(Factory factory) {
            id = factory.id;
        }

        @Override
        public String toString() {
            return "INC " + id;
        }
    }


    class State {

        List<Factory> factories = new ArrayList<>();

        List<Factory> allyFactories = new ArrayList<>();

        List<Factory> otherFactories = new ArrayList<>();

        List<Troop> troops = new ArrayList<>();

        List<Bomb> bombs = new ArrayList<>();

        void input(Scanner in) {
            factories.clear();
            troops.clear();
            allyFactories.clear();
            otherFactories.clear();
            bombs.clear();
            for (int i = 0, entityCount = in.nextInt(); i < entityCount; i++) {
                int entityId = in.nextInt();
                String entityType = in.next();
                int arg1 = in.nextInt();
                int arg2 = in.nextInt();
                int arg3 = in.nextInt();
                int arg4 = in.nextInt();
                int arg5 = in.nextInt();
                if (entityType.equals("FACTORY")) {
                    Factory factory = new Factory(entityId, arg1, arg2, arg3);
                    factories.add(factory);
                    if (factory.owner == Owner.aly) allyFactories.add(factory);
                    else otherFactories.add(factory);
                } else if (entityType.equals("BOMB")) {
                    Bomb bomb = new Bomb(entityId, arg1, arg2, arg3, arg4);
                    bombs.add(bomb);
                } else {
                    Troop troop = new Troop(entityId, arg1, arg2, arg3, arg4, arg5);
                    troops.add(troop);
                }
            }
            for (Troop troop : troops) {
                for (Factory factory : factories) {
                    if (troop.to == factory.id) {
                        factory.troops.add(troop);
                        break;
                    }
                }
            }
            for (Factory f : factories) {
                f.init1(this);
            }
            for (Factory f : factories) {
                f.init2(this);
            }
        }
    }

    class Bomb {
        final int id;

        final Owner owner;

        final int from;

        final int to;

        final int remain;

        Bomb(int id, int owner, int from, int to, int remain) {
            this.id = id;
            this.owner = Owner.get(owner);
            this.from = from;
            this.to = to;
            this.remain = remain;
        }

        @Override
        public String toString() {
            return Arrays.deepToString(new Object[]{"id", id, "owner", owner, "from", from, "to", to, "remain", remain});
        }
    }

    class Troop {
        final int id;

        final Owner owner;

        final int from;

        final int to;

        final int cyborgs;

        final int remain;

        Troop(int id, int owner, int from, int to, int cyborgs, int remain) {
            this.id = id;
            this.owner = Owner.get(owner);
            this.from = from;
            this.to = to;
            this.cyborgs = cyborgs;
            this.remain = remain;
        }

        @Override
        public String toString() {
            return Arrays.deepToString(new Object[]{"id", id, "owner", owner, "from", from, "to", to, "cyborgs", cyborgs, "remain", remain});
        }
    }

    class Factory {
        final int id;

        final Owner owner;

        final int cyborgs;

        final int production;

        final List<Troop> troops = new ArrayList<>();

        Factory near;

        int ownDist, oppDist;

        Owner result;

        int reserve;
        int[] alyCyb = new int[10], oppCyb = new int[10];

        boolean isBomb;

        private int time = 0;

        Factory(int id, int owner, int cyborgs, int production) {
            this.id = id;
            this.owner = Owner.get(owner);
            this.cyborgs = cyborgs;
            this.production = production;
        }

        void init1(State state) {
            result = owner;
            reserve = cyborgs;
            int time = 0;
            int currentCyb = cyborgs;
            Collections.sort(troops, (a, b) -> {
                if (a.remain != b.remain) return a.remain - b.remain;
                int ad = Math.abs(owner.ordinal() - a.owner.ordinal());
                int bd = Math.abs(owner.ordinal() - b.owner.ordinal());
                return ad - bd;
            });
            for (Troop troop : troops) {
                {
                    int tt = troop.remain < 9 ? troop.remain : 9;
                    if (troop.owner == Owner.aly) {
                        alyCyb[tt] += troop.cyborgs;
                    } else {
                        oppCyb[tt] += troop.cyborgs;
                    }
                }
                if (result == Owner.neutral) currentCyb -= troop.cyborgs;
                else {
                    currentCyb += production * (troop.remain - time);
                    if (result == troop.owner) currentCyb += troop.cyborgs;
                    else currentCyb -= troop.cyborgs;
                }
                time = troop.remain;
                if (reserve > currentCyb) reserve = currentCyb;
                if (currentCyb < 0) {
                    currentCyb *= -1;
                    result = troop.owner;
                }
            }
            for (int i = 1; i < 10; ++i) {
                alyCyb[i] += alyCyb[i - 1];
                oppCyb[i] += oppCyb[i - 1];
            }
            isBomb = state.bombs.stream().anyMatch(x -> x.to == id);
            ownDist = state.factories.stream().filter(x -> this != x && x.owner == Owner.aly).mapToInt(x -> distance[this.id][x.id]).sum();
            oppDist = state.factories.stream().filter(x -> this != x && x.owner == Owner.opp).mapToInt(x -> distance[this.id][x.id]).sum();
        }

        void init2(State state) {
            near = null;
            final int d[] = distance[this.id];
            for (Factory factory : state.factories.stream().filter(x -> x.result == Owner.aly).sorted((a, b) -> a.oppDist - b.oppDist).collect(Collectors.toList())) {
                if (this == factory) continue;
                if (state.factories.stream().filter(x -> x.result != Owner.aly).allMatch(x -> distance[this.id][x.id] > distance[factory.id][x.id])) {
                    near = factory;
                    break;
                }
            }
        }

        @Override
        public String toString() {
            return Arrays.deepToString(new Object[]{"id", id, "owner", owner, "production", production, "troops", troops.size(), "cyborgs", cyborgs});
        }
    }

    enum Owner {
        aly(1), opp(-1), neutral(0);

        private final int id;

        Owner(int id) {
            this.id = id;
        }

        static Owner get(int id) {
            for (Owner o : values()) if (o.id == id) return o;
            return null;
        }
    }

    void tr(Object... o) {
        System.err.println(Arrays.deepToString(o));
    }
}