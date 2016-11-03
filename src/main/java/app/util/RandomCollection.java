package app.util;

import java.util.*;

/**
 * @author Fran Lozano
 */
public class RandomCollection<E> {
    private TreeSet<Pair<E,Double>> set;
    private final Random random;
    private double total = 0;

    public RandomCollection(HashMap<E,Double> map) {
        this(new Random());
        Comparator<Pair<E,Double>> comp = (Pair<E,Double> o1, Pair<E,Double> o2) ->
                (o1.getRight().compareTo(o2.getRight()));
        this.set = new TreeSet<>(comp);
        Iterator<Map.Entry<E, Double>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<E, Double> pair = it.next();
            total += pair.getValue();
            this.set.add((new Pair<E,Double> (pair.getKey(), total)));
        }
    }

    private RandomCollection(Random random) {
        this.random = random;
    }

    public void add(E result,double weight) {
        if (weight <= 0) return;
        total += weight;
        set.add(new Pair<E,Double>(result,weight));
    }

    public E next() {
        double value = random.nextDouble() * total;
        return set.ceiling(new Pair <>(null,value)).getLeft();
    }

    public void update(E element, double diffWeight) {
        boolean found = false;
        Iterator<Pair<E, Double>> it = set.iterator();
        Comparator<Pair<E,Double>> comp = (Pair<E,Double> o1, Pair<E,Double> o2) ->
                (o1.getRight().compareTo(o2.getRight()));
        TreeSet<Pair<E,Double>> subSet;
        subSet = new TreeSet<>(comp);
        while (it.hasNext()) {
            Pair<E,Double> pair = (Pair<E, Double>) it.next();
            if (element.equals(pair.getLeft())) found = true;
            if (found) {
                subSet.add(new Pair<E,Double>(pair.getLeft(),pair.getRight()+diffWeight));
                it.remove();
            }
        }
        set.addAll(subSet);
    }

    public TreeSet<Pair<E,Double>> getElements () {
        return this.set;
    }
}
