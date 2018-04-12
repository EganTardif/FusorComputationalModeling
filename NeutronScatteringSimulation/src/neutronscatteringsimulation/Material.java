/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package neutronscatteringsimulation;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point3D;

/**
 *
 * @author jfellows
 */
public class Material {

    public final ObservableList<Element> elements;
    private final HashMap<String, CrossSection> totals;
    private final HashMap<String, CrossSection> elastics;
    double density;

    public Material(double density, ObservableList<Element> elements) {
        totals = new HashMap<>();
        elastics = new HashMap<>();
        for (Element e : elements) {
            totals.put(e.isotope.get(), new CrossSection(e.isotope.get() + " N,TOT"));
            elastics.put(e.isotope.get(), new CrossSection(e.isotope.get() + " N,EL"));
        }
        this.density = density; // g/cm3
        this.elements = elements;
    }
    
    public double sigmaTotal(Point3D R) {
        double sigma = 0;
        for (Element e : elements) {
            sigma += e.massPercentage.get() * totals.get(e.isotope.get()).get(R.magnitude());
        }
        sigma *= density;
        return sigma;
    }
    
    public double sigmaElasticScattering(Point3D R) {
        double sigma = 0;
        for (Element e : elements) {
            sigma += e.massPercentage.get() * elastics.get(e.isotope.get()).get(R.magnitude());
        }
        sigma *= density;
        return sigma;
    }

    private static class CrossSection {

        private final TreeMap<Double, Double> pointsMap;

        public CrossSection(String filename) {
            pointsMap = new TreeMap<>();
            try (Scanner scanner = new Scanner(new File("cross_sections/" + filename + ".txt"))) {
                String[] pieces;
                while (scanner.hasNextLine()) {
                   pieces = scanner.nextLine().trim().split("[\\s]+");
                   if (pieces[0].charAt(0) == '#') {
                       continue;
                   }
                   pointsMap.put(Double.valueOf(pieces[0]), Double.valueOf(pieces[1]));
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Material.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public double get(Double energy) {
            // energy in eV
            Map.Entry<Double, Double> ceiling = pointsMap.ceilingEntry(energy);
            Map.Entry<Double, Double> floor = pointsMap.floorEntry(energy);
            double crossSection = ((ceiling.getValue() - floor.getValue()) * (energy - floor.getKey())) / (ceiling.getKey() - floor.getKey()) + floor.getValue(); // b
            return crossSection;
        }
    }

    public static class Element {

        public final SimpleStringProperty isotope;
        public final SimpleDoubleProperty massPercentage;

        public Element(String isotope, double massPercentage) {
            this.isotope = new SimpleStringProperty(isotope);
            this.massPercentage = new SimpleDoubleProperty(massPercentage);
        }
    }

}