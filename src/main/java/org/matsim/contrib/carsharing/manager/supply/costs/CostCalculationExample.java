package org.matsim.contrib.carsharing.manager.supply.costs;

import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.api.core.v01.population.Person;

/** 
 * @author balac
 */
public class CostCalculationExample implements CostCalculation {

	private final static double betaTT = 1.0;
	private final static double betaRentalTIme = 1.0;
	private final static double betaDistance = 0.0;	
	private final static double betaWalking = 1.0;
	private final static double scaleTOMatchCar = 4.0;
	private final static double betaVOT = 1.0;
	private final static double rentalCost = 1.0;
	private final static double carsAvailable = 1.0;
	private final static double alfaCS = 214778000.0; // alzare poco ma va bene
	private Person person;
	
	
	@Override
	public double getCost(RentalInfo rentalInfo) {

		double rentalTIme = rentalInfo.getEndTime() - rentalInfo.getStartTime();
		double inVehicleTime = rentalInfo.getInVehicleTime();
		double accessTime = rentalInfo.getAccessEndTime() - rentalInfo.getAccessStartTime();
		double egressTime = rentalInfo.getEgressEndTime() - rentalInfo.getEgressStartTime();
		double distance = rentalInfo.getDistance();
//		double personVoT = (double) person.getAttributes().getAttribute("vot");
		
	
		//need to insert the available cars carsAvailable (1/aj) under (CostCalculationExample.rentalCost*rentalTIme)
		return scaleTOMatchCar * (CostCalculationExample.alfaCS + (CostCalculationExample.betaVOT/1) + (CostCalculationExample.betaRentalTIme * (((CostCalculationExample.rentalCost*rentalTIme)/CostCalculationExample.carsAvailable)+ (CostCalculationExample.betaDistance * distance))) +
				(CostCalculationExample.betaWalking*(accessTime + egressTime)) + (CostCalculationExample.betaTT * inVehicleTime )) ;
		
		//Then where does it go? Why nothing happens if I modify it?
	}

}
