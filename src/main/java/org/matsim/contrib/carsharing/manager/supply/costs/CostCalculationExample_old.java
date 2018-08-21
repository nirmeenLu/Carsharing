package org.matsim.contrib.carsharing.manager.supply.costs;

import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
/** 
 * @author balac
 */
public class CostCalculationExample_old implements CostCalculation {

	private final static double betaTT = 1.0;
	private final static double betaRentalTIme = 1.0;
	private final static double scaleTOMatchCar = 40000000000000000000000000000000000000000000000000000000000000000000.0;
	
	@Override
	public double getCost(RentalInfo rentalInfo) {

		double rentalTIme = rentalInfo.getEndTime() - rentalInfo.getStartTime();
		double inVehicleTime = rentalInfo.getInVehicleTime();
		
		
		return CostCalculationExample_old.scaleTOMatchCar * 
				(inVehicleTime /60.0 * 0.3 + (rentalTIme - inVehicleTime) / 60.0 * 0.15);
	}

}
