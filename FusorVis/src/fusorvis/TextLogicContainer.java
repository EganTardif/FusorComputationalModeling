/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fusorvis;

/**
 *
 * @author guberti
 */
public abstract class TextLogicContainer {
    public double calc() {return 0.0;};
    public String getText() {
        return Double.toString(calc());
    }
}
