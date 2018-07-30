package org.matsim.contrib.carsharing.scoring;

import java.util.Set;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.demand.AgentRentals;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.TwoWayContainer;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.stations.CarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.functions.ScoringParameters;

import com.google.common.collect.ImmutableSet;


public class CarsharingLegScoringFunction extends org.matsim.core.scoring.functions.CharyparNagelLegScoring {

	
	private Config config;	
	
	private CostsCalculatorContainer costsCalculatorContainer;
	private DemandHandler demandHandler;
	private Person person;
	private CarsharingSupplyInterface carsharingSupplyContainer;
	
	/*private static final  Set<String> walkingLegs = ImmutableSet.of("egress_walk_ow", "access_walk_ow",
			"egress_walk_tw", "access_walk_tw", "egress_walk_ff", "access_walk_ff");
	
	private static final  Set<String> carsharingLegs = ImmutableSet.of("oneway_vehicle", "twoway_vehicle",
			"freefloating_vehicle");*/
	
	public CarsharingLegScoringFunction(ScoringParameters params, 
			Config config,  Network network, DemandHandler demandHandler,
			CostsCalculatorContainer costsCalculatorContainer, CarsharingSupplyInterface carsharingSupplyContainer,
			Person person)
	{
		super(params, network);
		this.config = config;
		this.demandHandler = demandHandler;
		this.carsharingSupplyContainer = carsharingSupplyContainer;
		this.costsCalculatorContainer = costsCalculatorContainer;
		this.person = person;		
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);		
	}	
	
	@Override
	public void finish() {		
		super.finish();		
		
		AgentRentals agentRentals = this.demandHandler.getAgentRentalsMap().get(person.getId());
		if (agentRentals != null) {
			double marginalUtilityOfMoney = ((PlanCalcScoreConfigGroup)this.config.getModule("planCalcScore")).getMarginalUtilityOfMoney();
			for(RentalInfo rentalInfo : agentRentals.getArr()) {
				CSVehicle vehicle = this.carsharingSupplyContainer.getAllVehicles().get(rentalInfo.getVehId().toString());
				if (marginalUtilityOfMoney != 0.0)
					score += -1 * this.costsCalculatorContainer.getCost(vehicle.getCompanyId(), 
							rentalInfo.getCarsharingType(), rentalInfo) * marginalUtilityOfMoney;
			}			
		}				
	}	
	
	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg) {
		
		
		double tmpScore = 0.0D;
		
		//person VOT
		double personVoT = (double) person.getAttributes().getAttribute("vot");
		//Beta VOT form config.xml
		Double constantVot = Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("votTwoWayCarsharing"));
		
		
		double travelTime = arrivalTime - departureTime;
		String mode = leg.getMode();
		
		int availCars = 1;
		//startLink of person leg route
		Link startLink = network.getLinks().get(leg.getRoute().getStartLinkId());
		
		//Search distance constant value form config.xml
		double searchDistance =  Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("searchDistanceTwoWayCarsharing")); 
		
		//Gets nearest car to the person depending on his location and search distance 
		CSVehicle vehicle = this.carsharingSupplyContainer.findClosestAvailableVehicle(startLink, "twoway", "car", "Mobility", searchDistance);

		if (vehicle != null && mode.equals("car")) {
			
			//get the station of the nearest vehicle to the person
			CarsharingStation nearestStation = ((TwoWayContainer) this.carsharingSupplyContainer
					.getCompany(vehicle.getCompanyId()).getVehicleContainer("twoway"))
							.getTwowaycarsharingstationsMap()
							.get(((StationBasedVehicle) vehicle).getStationId());
			
			//gets the number of available cars in the nearest station 
			availCars = ((TwoWayCarsharingStation) nearestStation).getNumberOfVehicles(vehicle.getType());
			
			System.out.println("---------------------------------------- availCars " + availCars);
			
			//adds constant in disutility of travel equation
			tmpScore += Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("constantTwoWayCarsharing"));
			
			//adds personVot and number of available car updates to  disutility of travel equation
			tmpScore += constantVot * personVoT * ((travelTime * Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("travelingTwoWayCarsharing")) / 3600.0)/availCars);

		}
			

		/*if (carsharingLegs.contains(mode)) {
			if (("oneway_vehicle").equals(mode)) {				
				tmpScore += Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("constantOneWayCarsharing"));
				tmpScore += constantVot * personVoT * travelTime * Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("travelingOneWayCarsharing")) / 3600.0;
			}		
		
			else if (("freefloating_vehicle").equals(mode)) {				
				
				tmpScore += Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("constantFreeFloating"));
				tmpScore += constantVot * personVoT * travelTime * Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("travelingFreeFloating")) / 3600.0;
			}		
			
			else if (("twoway_vehicle").equals(mode)) {		
				
				
				tmpScore += Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("constantTwoWayCarsharing"));
				tmpScore += constantVot * personVoT * ((travelTime * Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("travelingTwoWayCarsharing")) / 3600.0)/availCars);
			}
		}
		
		else if (walkingLegs.contains(mode)) {
			
			tmpScore += getWalkScore(leg.getRoute().getDistance(), travelTime);
			
		}	*/	
		
		return tmpScore;
	}

	/*private double getWalkScore(double distance, double travelTime)
	{
		double score = 0.0D;

		score += travelTime * this.params.modeParams.get(TransportMode.walk).marginalUtilityOfTraveling_s + this.params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m * distance;

		return score;
	}*/
}
