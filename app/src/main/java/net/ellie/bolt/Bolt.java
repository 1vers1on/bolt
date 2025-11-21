package net.ellie.bolt;

public class Bolt {
    public static Bolt instance;

    public static Bolt getInstance() {
        if (instance == null) {
            instance = new Bolt();
        }
        return instance;
    }

    public void start() {
        System.out.println("Bolt started!");
    }
}
