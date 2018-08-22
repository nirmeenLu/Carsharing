package org.matsim.contrib.carsharing.manager.supply.costs;

import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;

/** 
 * @author balac
 */
public class CostCalculationExample implements CostCalculation {

	private final static double betaTT = 1.0;
	private final static double betaRentalTIme = 1.0;
	private final static double betaDistance = 1.0;	
	private final static double betaWalking = 1.0;
	private final static double scaleTOMatchCar = 1000.0;
	private final static double betaVOT = 3600.0;
	private final static double rentalCost = 1.0;
	private final static double carsAvailable = 1.0;
	private final static double alfaCS = 1.0; // alzare poco ma va bene 214778000
	private Person person;
	
	private static final Logger log = Logger.getLogger(CostCalculationExample.class);
	
	
	@Override
	public double getCost(RentalInfo rentalInfo) {

		double rentalTIme = rentalInfo.getEndTime() - rentalInfo.getStartTime();
		double inVehicleTime = rentalInfo.getInVehicleTime();
		double accessTime = rentalInfo.getAccessEndTime() - rentalInfo.getAccessStartTime();
		double egressTime = rentalInfo.getEgressEndTime() - rentalInfo.getEgressStartTime();
		double distance = rentalInfo.getDistance();
		double personVot = 1;//(double) person.getAttributes().getAttribute("vot");
		
		double evaVot = CostCalculationExample.betaVOT/personVot;
		double evaTime = (CostCalculationExample.rentalCost*rentalTIme)/CostCalculationExample.carsAvailable;
		double evaDist = CostCalculationExample.betaDistance * (distance);
		double evaWalk = CostCalculationExample.betaWalking*(accessTime + egressTime);
		double evaTrav = CostCalculationExample.betaTT * inVehicleTime;
		
		double costLu = scaleTOMatchCar * (CostCalculationExample.alfaCS + evaVot + (CostCalculationExample.betaRentalTIme * (evaTime + evaDist)) +	evaWalk + evaTrav) ;
		
		//need to insert the available cars carsAvailable (1/aj) under (CostCalculationExample.rentalCost*rentalTIme)
		
		log.warn("^^^^^^^^^^> VOT " + evaVot);
		log.warn("==========> TIME " + evaTime);
		log.warn("==========> DIST " + evaDist);
		log.warn("==========> WALK " + evaWalk);
		log.warn("==========> TRAV " + evaTrav);
		log.warn("__________> cost " + costLu);
		
		return costLu;
		
	}

}
