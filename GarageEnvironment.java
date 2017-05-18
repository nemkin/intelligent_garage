// Environment code for project Garage.mas2j

import java.io.*;
import java.util.*;
import java.util.logging.*;

import jason.asSyntax.*;
import jason.environment.*;
import jason.asSyntax.parser.*;

import jason.environment.grid.Location;

public class GarageEnvironment extends Environment {

    private Timer eventTimer;
    public class EventTimer extends TimerTask {

        private GarageEnvironment env;

        public EventTimer(GarageEnvironment env) {
            super();
            this.env = env;
        }

        @Override
            public void run() {
                env.randomizeEvents();
            }
    }

    GarageModel model;

    private Term up    = DefaultTerm.parse("do(up)");
    private Term down  = DefaultTerm.parse("do(down)");
    private Term right = DefaultTerm.parse("do(right)");
    private Term left  = DefaultTerm.parse("do(left)");

    @Override
        public void init(String[] args) {
            super.init(args);
            
            try {

                BufferedReader mapFile = new BufferedReader(new FileReader(GarageModel.mapPath));

                String[] dim = mapFile.readLine().split(" ");
                int mapx = Integer.parseInt(dim[0]);
                int mapy = Integer.parseInt(dim[1]);

                model = new GarageModel(mapx, mapy);

                mapFile.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            if(args.length>=1 && args[0].equals("gui")) {
                GarageView view = new GarageView(model);
                model.setView(view);
            }
            updatePercepts();

            eventTimer = new Timer();
            eventTimer.schedule(new EventTimer(this), 0, 500);
        }

    @Override
        public void stop() {
            deletePercepts();
            super.stop();
        }

    public void randomizeEvents() {

        Random rand = new Random();

        for(int i=0; i<model.getWidth(); ++i) {
            for(int j=0; j<model.getHeight(); ++j) {

                // Car can arrive here.
                if(model.hasObject(model.GATE, i,j) && (model.getCarAt(i,j) == null)) {
                    if(rand.nextInt(100) < 250) {
                        model.generateCar(i,j);
                    } 
                }

                // Car can leave here.
                if(model.hasObject(model.PARKINGSPOT,i,j)  && (model.getCarAt(i,j)!=null)) {
                    if(rand.nextInt(100) < 50) {
                        model.getCarAt(i,j).leaving = true;
                    }
                }
            }
        }

        updatePercepts();
    }

    private void updatePercepts() {

        deletePercepts();

        try {

            addPercept("navigator", ASSyntax.parseLiteral("dimension("+model.getWidth()+","+model.getHeight()+")"));
            
            Location valetLoc = model.getAgPos(0);
            addPercept("valet", ASSyntax.parseLiteral("position("+valetLoc.x+","+valetLoc.y+")"));

            for(int i=0; i<model.getWidth(); ++i) {
                for(int j=0; j<model.getHeight(); ++j) {

                    if(model.isFree(i,j)) {
                        addPercept("navigator", ASSyntax.parseLiteral("~obstacle("+i+","+j+")"));
                    } else {
                        addPercept("navigator", ASSyntax.parseLiteral("obstacle("+i+","+j+")"));
                    }

                    if(model.hasObject(model.PARKINGSPOT,i,j) && model.hasObject(model.CAR,i,i)) {
                        addPercept("surveillance", ASSyntax.parseLiteral("takenparkingspot("+i+","+j+")"));
                        if(model.getCarAt(i,j).leaving) {
                            addPercept("surveillance", ASSyntax.parseLiteral("carLeaving("+i+","+j+")"));
                        }
                    }

                    if(model.hasObject(model.PARKINGSPOT,i,j) && !(model.hasObject(model.CAR,i,j))) {
                        addPercept("surveillance", ASSyntax.parseLiteral("emptyparkingspot("+i+","+j+")"));
                    }

                    if(model.hasObject(model.GATE,i,j)) {
                        addPercept("surveillance", ASSyntax.parseLiteral("gate("+i+","+j+")"));
                        if(!(model.getCarAt(i,j)!=null && model.getCarAt(i,j).leaving)) {
                            addPercept("surveillance", ASSyntax.parseLiteral("carArrived("+i+","+j+")"));
                        }
                    }
                }
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
    } 

    private void deletePercepts() {
        clearPercepts();
        clearPercepts("navigator");
        clearPercepts("surveillance");
        clearPercepts("valet");
    }


    @Override
        public boolean executeAction(String agName, Structure action) {
            if(agName.equals("valet")) {
                if(action.equals(up)) return model.moveAgentUp(0);
                if(action.equals(down)) return model.moveAgentDown(0);
                if(action.equals(left)) return model.moveAgentLeft(0);
                if(action.equals(right)) return model.moveAgentRight(0);
            }
            return super.executeAction(agName, action);
        }
}


