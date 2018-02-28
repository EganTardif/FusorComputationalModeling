/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fusorcompmodeling;

/**
 *
 * @author sfreisem-kirov
 */
public class euler implements Solver{

    @Override
    public Atom moveForward(Point[] points, double TS, Atom a, EField e) {
        Ray v0 = a.velocity;
        Ray g = StatsGen.FToAcc(points, a.position,a.mass,e.EFieldSum(points, a.position));
        
        a.position.x += (v0.x*TS) + ((g.x*TS*TS)/2);
        a.position.y += (v0.y*TS) + ((g.y*TS*TS)/2);
        a.position.z += (v0.z*TS) + ((g.z*TS*TS)/2);
        
        a.velocity.x = v0.x + (g.x*TS);
        a.velocity.y = v0.y + (g.y*TS);
        a.velocity.z = v0.z + (g.z*TS);
        
    return a;
        
    }
    
}
