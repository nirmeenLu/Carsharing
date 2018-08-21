//Where can I understandand where this score goes? Where is it used this number?

package org.matsim.contrib.carsharing.manager.supply.costs;

import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
/** 
 * @author balac
 */
public interface CostCalculation {
	
	public double getCost(RentalInfo rentalInfo);

}
