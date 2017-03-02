import java.util.*;
import java.util.stream.Collectors;

class Player {

    public static void main(String args[]) {
        new Player().solve();
    }

    private int distance[][];

    private void solve() {
        try (Scanner in = new Scanner(System.in)) {

            int n = in.nextInt();
            distance = new int[n][n];
            for (int i = 0; i < n; ++i) {
                distance[i][i] = 0xffff;
            }
            for (int i = 0, linkCount = in.nextInt(); i < linkCount; i++) {
                int f1 = in.nextInt();
                int f2 = in.nextInt();
                int d = in.nextInt();
                distance[f1][f2] = distance[f2][f1] = d;
            }
            int bomb = 2;
            boolean bombed[] = new boolean[n];

            State state = new State();
            List<Order> orders = new ArrayList<>();
            while (true) {
                state.input(in);
                orders.clear();

                if (state.isInProductionFactory()) {
                    state.otherFactories = state.otherFactories.stream().filter(x -> x.production > 0 || x.remain == 0).collect(Collectors.toList());
                }
                state.otherFactories = state.otherFactories.stream().filter(x -> x.remain >= 0).collect(Collectors.toList());

                for (Factory lose : state.ownFactories.stream().filter(x -> x.remain < 0 && x.production > 0).collect(Collectors.toList())) {
                    int remain = -lose.remain;
                    Collections.sort(state.ownFactories, (a, b) -> distance[lose.id][a.id] - distance[lose.id][b.id]);
                    for (Factory source : state.ownFactories) {
                        if (source.remain < 0) continue;
                        int send = Math.min(source.remain, remain);
                        orders.add(new Move(source.id, lose.id, send));
                        source.remain -= send;
                        remain -= send;
                        if (remain <= 0) break;
                    }
                }

                if (bomb > 0) {
                    int max_production = state.factories.stream().mapToInt(x -> x.production).max().getAsInt();
                    Optional<Factory> target = state.otherFactories.stream().filter(x -> !bombed[x.id] && x.owner == Owner.opp && x.production == max_production).findFirst();
                    if (target.isPresent()) {
                        Optional<Factory> source = state.ownFactories.stream().sorted((a, b) -> distance[target.get().id][a.id] - distance[target.get().id][b.id]).findFirst();
                        if (source.isPresent()) {
                            orders.add(new SendBomb(source.get().id, target.get().id));
                            bombed[target.get().id] = true;
                            --bomb;
                        }
                    }
                }

                for (Factory source : state.ownFactories) {
                    if (source.near == null) {
                        Collections.sort(state.otherFactories, (a, b) -> {
                            int av = 10 * a.production - a.remain - distance[source.id][a.id] * 5 + (a.owner == Owner.neutral ? 10 : 0);
                            int bv = 10 * b.production - b.remain - distance[source.id][b.id] * 5 + (b.owner == Owner.neutral ? 10 : 0);
                            return bv - av;
                        });
                        for (Factory target : state.otherFactories) {
                            if (target.remain < 0) continue;
                            target.addCyborg(distance[source.id][target.id] + 1);
                            if (source.remain < 1 || source.remain <= target.remain) {
                                break;
                            }
                            int send = target.owner == Owner.neutral ? target.remain + 1 : source.remain;
                            orders.add(new Move(source.id, target.id, send));
                            source.remain -= send;
                            target.remain -= send;
                        }
                    }
                    if (source.remain >= 10 && source.production < 3) {
                        orders.add(new Inc(source));
                        source.remain -= 10;
                    }
                    if (source.remain > 30) {
                        source.remain -= 30;
                        state.factories.stream().filter(x -> x.production == 0).findFirst().ifPresent(x -> {
                            orders.add(new Move(source.id, x.id, 30));
                        });
                    }
                    if (source.near != null && (source.production == 3 || source.production == 0) && source.remain > 0) {
                        orders.add(new Move(source.id, source.near.id, source.remain));
                        source.remain = 0;
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
        List<Factory> ownFactories = new ArrayList<>();
        List<Factory> otherFactories = new ArrayList<>();
        List<Troop> troops = new ArrayList<>();
        List<Bomb> bombs = new ArrayList<>();

        void input(Scanner in) {
            factories.clear();
            troops.clear();
            ownFactories.clear();
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
                    if (factory.owner == Owner.own) ownFactories.add(factory);
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

        boolean isInProductionFactory() {
            for (Factory f : otherFactories) if (f.production > 0) return true;
            return false;
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
        int otherDist;
        int remain;
        boolean isBomb;
        private int time = 0;

        Factory(int id, int owner, int cyborgs, int production) {
            this.id = id;
            this.owner = Owner.get(owner);
            this.cyborgs = cyborgs;
            this.production = production;
        }

        void addCyborg(int time) {
            if (owner == Owner.neutral || isBomb) return;
            if (this.time < time) {
                remain += production * (time - this.time);
                this.time = time;
            }
        }

        void init1(State state) {
            {
                remain = this.cyborgs;
                int cyborgs = 0, time = 0;
                Collections.sort(troops, (a, b) -> {
                    if (a.remain != b.remain) return a.remain - b.remain;
                    int ad = Math.abs(owner.ordinal() - a.owner.ordinal());
                    int bd = Math.abs(owner.ordinal() - b.owner.ordinal());
                    return ad - bd;
                });
                for (Troop troop : troops) {
                    if (troop.owner == owner) cyborgs += troop.cyborgs;
                    else cyborgs -= troop.cyborgs;
                    int need = this.cyborgs + cyborgs;
                    if (owner != Owner.neutral) need += production * (troop.remain - time);
                    time = troop.remain;
                    if (remain > need) remain = need;
                }
                if (remain > this.cyborgs) remain = this.cyborgs;
            }
            isBomb = state.bombs.stream().anyMatch(x -> x.to == id);
            otherDist = state.factories.stream().filter(x -> x.owner != Owner.neutral && x.owner != this.owner).mapToInt(x -> distance[this.id][x.id]).sum();
        }

        void init2(State state) {
            near = null;
            final int d[] = distance[this.id];
            for (Factory factory : state.ownFactories.stream().sorted((a, b) -> {
                if (d[a.id] != d[b.id]) return d[a.id] - d[b.id];
                return a.otherDist - b.otherDist;
            }).collect(Collectors.toList())) {
                if (this == factory) continue;
                if (state.factories.stream().filter(x -> x.owner != Owner.own).allMatch(x -> distance[this.id][x.id] > distance[factory.id][x.id])) {
                    near = factory;
                    break;
                }
            }
        }

        @Override
        public String toString() {
            return Arrays.deepToString(new Object[]{"id", id, "owner", owner, "production", production, "troops", troops.size(), "cyborgs", cyborgs, "remain", remain});
        }
    }

    enum Owner {
        own(1), opp(-1), neutral(0);

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