package org.matsim.contrib.carsharing.manager.supply.costs;

import org.apache.log4j.Logger;
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
/** 
 * @author balac
 */
public class CostCalculationExample_old implements CostCalculation {

	private final static double betaTT = 1.0;
	private final static double betaRentalTIme = 1.0;
	private final static double scaleTOMatchCar = 1.0;
	
	private static final Logger log = Logger.getLogger(CostCalculationExample_old.class);
	
	@Override
	public double getCost(RentalInfo rentalInfo) {

		double rentalTIme = rentalInfo.getEndTime() - rentalInfo.getStartTime();
		double inVehicleTime = rentalInfo.getInVehicleTime();
		
		
		double costTEST = CostCalculationExample_old.scaleTOMatchCar * 
				(inVehicleTime /60.0 * 0.3 + (rentalTIme - inVehicleTime) / 60.0 * 0.15);
		
		//Added by me
		log.warn("__________> rental & veh " + (rentalTIme - inVehicleTime));		
		log.warn("__________> inVehicleTime " + inVehicleTime);		
		log.warn("==========> costTEST " + costTEST);
		
		return costTEST;
	}

}
