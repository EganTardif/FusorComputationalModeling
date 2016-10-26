/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fusorcompmodeling;

import static fusorcompmodeling.PointDistributer.electricPotential;

/**
 *
 * @author sfreisem-kirov
 */
public class StatsGen {
    public StatsGen() {
    
    }
    public static double avgPotential(Point[] points, int charge){
        double avgPotential;
        double totalPotential = 0;
        for(int i = 0; i < points.length; i++){
            if (points[i].charge == charge){
                totalPotential += electricPotential(points,points[i]);
            }
        }
        avgPotential = 2 * totalPotential/(points.length);
        return avgPotential;
        
    }
    public static double getDeltaPhi(double posAvgPot, double negAvgPot){
        double DeltaPhi;
        DeltaPhi = posAvgPot - negAvgPot;
        return DeltaPhi;
    }

    public static VectorForce getVforce(Point[] points, double voltage, double q, Point r){
        VectorForce vForce = new VectorForce();
        double kQ = EField.getkQ(voltage);
        vForce.setXForce((q*kQ)/r.x);
        vForce.setYForce((q*kQ)/r.y);
        vForce.setZForce((q*kQ)/r.z);
        return vForce;
    }
    public static VectorAcceleration getAcceleration(Point[] points, double voltage, double q, Point r, double mass){
        VectorAcceleration vAcc = new VectorAcceleration();
        vAcc.setXAcceleration(getVforce(points, voltage, q, r).getXForce()/mass);
        vAcc.setYAcceleration(getVforce(points, voltage, q, r).getYForce()/mass);
        vAcc.setZAcceleration(getVforce(points, voltage, q, r).getZForce()/mass);
        return vAcc;
    }
    public static VectorVelocity getVelocity(Point[] points, double voltage, double q, Point r, double mass, double t, VectorVelocity initialV){
        VectorVelocity vVel = new VectorVelocity();
        vVel.setXVelocity(initialV.getXVelocity() - (getAcceleration(points, voltage, q, r, mass).getXAcceleration()*t));
        vVel.setYVelocity(initialV.getYVelocity() - (getAcceleration(points, voltage, q, r, mass).getYAcceleration()*t));
        vVel.setZVelocity(initialV.getZVelocity() - (getAcceleration(points, voltage, q, r, mass).getZAcceleration()*t));
        return vVel;
    }
}
