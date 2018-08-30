package org.matsim.contrib.carsharing.manager.supply.costs;

import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;

/**
 * @author balac
 */

public class CostCalculationExample implements CostCalculation {

	private final static double betaTravel = 15; // 6.0;
	private final static double betaRentalTIme = 1.0; //I don't think this should exist
	private final static double betaDistance = 0.0; // 0.0;
	private final static double betaWalking = 100.0;
	private final static double scaleTOMatchCar = 1.0; // 0.325; SCENDERE E VEDERE CHE SUCCEDE
	private final static double betaVOT = 10.0; // 1.0;
	private final static double rentalCost = 15.0;
	private final static double carsAvailable = 1.0;
	private final static double alfaCS = 1.0;
	private Person person;

	private static final Logger log = Logger.getLogger(CostCalculationExample.class);

	@Override
	public double getCost(RentalInfo rentalInfo) {

		double rentalTime = rentalInfo.getEndTime() - rentalInfo.getStartTime();
		double inVehicleTime = rentalInfo.getInVehicleTime();
		double accessTime = rentalInfo.getAccessEndTime() - rentalInfo.getAccessStartTime();
		double egressTime = rentalInfo.getEgressEndTime() - rentalInfo.getEgressStartTime();
		double distance = rentalInfo.getDistance();
		double personVot = 1;// (double) person.getAttributes().getAttribute("vot");

		double evaVot = CostCalculationExample.betaVOT / personVot;
		double evaTime = (CostCalculationExample.rentalCost * (rentalTime / 3600)) / CostCalculationExample.carsAvailable;
		double evaWalk = CostCalculationExample.betaWalking * ((accessTime + egressTime) / 3600);
		double evaDist = CostCalculationExample.betaDistance * (distance / 1000);
		double evaTrav = CostCalculationExample.betaTravel * (inVehicleTime / 3600);
		double distTrav = CostCalculationExample.betaRentalTIme * (evaTrav + evaDist);

		double costLu = scaleTOMatchCar * (CostCalculationExample.alfaCS + evaVot + distTrav + evaWalk + evaTrav);

		// need to insert the available cars carsAvailable (1/aj) under
		// (CostCalculationExample.rentalCost*rentalTIme)

		log.warn("^^^^^^^^^^> VOT " + evaVot);
		log.warn("==========> WALK " + evaWalk);
		log.warn("==========> TIME " + evaTime);
		log.warn("==========> DIST " + evaDist);
		log.warn("==========> TRAV " + evaTrav);
		log.warn("==========> DIST + TRAV " + distTrav);
		log.warn("__________> cost " + costLu);

		return costLu;

	}

}

/*
public class CostCalculationExample_new implements CostCalculation {

	private final static double betaTT = 1.0;
	private final static double betaRentalTIme = 1.0;
	private final static double scaleTOMatchCar = 4.0;
	
	@Override
	public double getCost(RentalInfo rentalInfo) {

		double rentalTIme = rentalInfo.getEndTime() - rentalInfo.getStartTime();
		double inVehicleTime = rentalInfo.getInVehicleTime();
		
		
		return CostCalculationExample_new.scaleTOMatchCar * 
				(inVehicleTime /60.0 * 0.3 + (rentalTIme - inVehicleTime) / 60.0 * 0.15);
	}

}

*/
