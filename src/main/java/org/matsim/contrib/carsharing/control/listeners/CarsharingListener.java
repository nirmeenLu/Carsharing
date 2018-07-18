package org.matsim.contrib.carsharing.control.listeners;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.demand.AgentRentals;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

/** 
 * 
 * @author balac
 */
public class CarsharingListener implements IterationEndsListener{

	@Inject private MatsimServices controler;
	@Inject private DemandHandler demandHandler;
	@Inject private CarsharingSupplyInterface carsharingSupply;

	ArrayList<Integer> rentalsPerIteration = new ArrayList<>();
	ArrayList<Double> avgAccessTimePerIteration = new ArrayList<>(); //Array created to store the avg rental time every iteration

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {		
		int numberOfRentals = 0;

		Map<Id<Person>, AgentRentals> agentRentalsMap = demandHandler.getAgentRentalsMap();
		
		Set<String> companies = carsharingSupply.getCompanyNames();					//crea un set di stringhe con tutti i valori (nomi) dell'oggetto companies
		Map<String,CSVehicle> allVehiclesMap = carsharingSupply.getAllVehicles();	//crea una mappa di <stringhe,CSVehicle> per tutti i veicoli del CS. Da qui trovo le stationId
		
		//Map<String, CompanyAgent> companyAgents = carsharingSupply.getCompany(companyId);
		
		//******************************************
		//CREAZIONE Lista RentedCars per Station.TXT
		//******************************************
		
		final BufferedWriter rentedCarsXStation = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(event.getIteration(), "RentedCarsXStation.txt"));
	
		try {
			rentedCarsXStation.write("rentedCars,Station");
			rentedCarsXStation.newLine();
							
			for (Id<Person> personId: agentRentalsMap.keySet()) {
				for (RentalInfo i : agentRentalsMap.get(personId).getArr()) {
				CSVehicle rental = this.carsharingSupply.getAllVehicles().get(i.getVehId().toString());
				rentedCarsXStation.write(rental.getVehicleId() + "," + rental.getStationId());
				rentedCarsXStation.newLine();
				}
			}
			
			rentedCarsXStation.flush();
			rentedCarsXStation.close();	
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//**********************
		//CREAZIONE Lista CS.TXT
		//**********************

		final BufferedWriter outLink = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(event.getIteration(), "CS.txt"));
		try {
			outLink.write("personID,carsharingType,startTime,endTIme,startLink,pickupLink,dropoffLink,endLink,distance,inVehicleTime,accessTime,egressTime,vehicleID,bookingTime,companyID,vehicleType");
			outLink.newLine();		
		
		for (Id<Person> personId: agentRentalsMap.keySet()) {
			
			for (RentalInfo i : agentRentalsMap.get(personId).getArr()) {
				CSVehicle vehicle = this.carsharingSupply.getAllVehicles().get(i.getVehId().toString());		
				numberOfRentals++;
				double bookingTime = i.getEndTime() - i.getStartTime();
				outLink.write(personId + "," + i.toString() + "," + bookingTime + "," + vehicle.getCompanyId() + "," + vehicle.getType());
				outLink.newLine();
			}
			
		}
		rentalsPerIteration.add(numberOfRentals);

		outLink.flush();
		outLink.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
/*											//**********************
*											//CREAZIONE Lista CS.TXT
*											//**********************
*							
*											final BufferedWriter outLink = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(event.getIteration(), "CS.txt"));
*											try {
*												outLink.write("personID,carsharingType,startTime,endTIme,startLink,pickupLink,dropoffLink,endLink,distance,inVehicleTime,accessTime,egressTime,vehicleID,bookingTime"
O														+ "companyID,vehicleType");
R												outLink.newLine();		
I											
G											for (Id<Person> personId: agentRentalsMap.keySet()) {
I												
N												for (RentalInfo i : agentRentalsMap.get(personId).getArr()) {
A													CSVehicle vehicle = this.carsharingSupply.getAllVehicles().get(i.getVehId().toString());		
L													numberOfRentals++;
-													double bookingTime = i.getEndTime() - i.getStartTime();
O													double accessTime = i.getAccessEndTime() - i.getAccessStartTime();
N													outLink.write(personId + "," + i.toString() + "," + bookingTime + "," + vehicle.getCompanyId() + "," + vehicle.getType());
E													outLink.newLine();
*												}
*												
*											}
*											rentalsPerIteration.add(numberOfRentals);
*							
*											outLink.flush();
*											outLink.close();
*											
*											} catch (IOException e) {
*												// TODO Auto-generated catch block
*												e.printStackTrace();
*											}
*/
		
		//***************************************
		//CREAZIONE Lista Rentals X Iteration.TXT
		//***************************************
		
		if (event.getIteration() == controler.getConfig().controler().getLastIteration()) {
			final BufferedWriter outLinkStats = IOUtils.getBufferedWriter(this.controler.getControlerIO().getOutputFilename("Rentals X Iteration.txt"));
			try {
				outLinkStats.write("iteration,numberOfRentals");
				outLinkStats.newLine();
				int k = 0;
				for (Integer i : rentalsPerIteration) {
					outLinkStats.write(k + "," + i);
					k++;
					outLinkStats.newLine();
				     				     
				}
				
				outLinkStats.flush();
				outLinkStats.close();	
			     
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//***********************
		//Average Access Time.TXT
		//***********************
		
		double avgAccessTime = 0; //it's needed to calculate the average of all times
		
		for (Id<Person> personId: agentRentalsMap.keySet()) {
			
			for (RentalInfo i : agentRentalsMap.get(personId).getArr()) {
				double accessTime = i.getAccessEndTime() - i.getAccessStartTime();			    
				avgAccessTime += accessTime; //SUM of all the distances that have to be divided to get the AVG
				}
		}
		
		avgAccessTime = avgAccessTime/numberOfRentals; //computing the average
		
		avgAccessTimePerIteration.add(avgAccessTime); //stores the current iteration rental time in the array 
		
		if (event.getIteration() == controler.getConfig().controler().getLastIteration()) { //this prints the array at the end of the simulation
			final BufferedWriter outLinkStats = IOUtils.getBufferedWriter(this.controler.getControlerIO().getOutputFilename("avgAccessTime.txt"));
			try {
			outLinkStats.write("iteration,avgAccessTime");
			outLinkStats.newLine();
			int k = 0;
			for (Double i : avgAccessTimePerIteration) {
				outLinkStats.write(k + "," + i);
				k++;
				outLinkStats.newLine();
			     				     
			}
			
			outLinkStats.flush();
			outLinkStats.close();	
		     
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
		//**********************************
		//PROVA Grafico RENTALS X ITERATIONS
		//**********************************
		
		if (event.getIteration() == controler.getConfig().controler().getLastIteration()) {
			int k = 0; //Iteration
			DefaultCategoryDataset RentalXIterData = new DefaultCategoryDataset();
			for (Integer i : rentalsPerIteration) {
				k++;
				RentalXIterData.setValue(i, "Rentals X Iteration", "" + k);
			}
			
			//Create a chart
			JFreeChart chartRxI = ChartFactory.createBarChart("Rentals X Iteration", "#Iteration", "#Rentals", RentalXIterData, PlotOrientation.VERTICAL, false, true, true);
			JFreeChart plotRxI = ChartFactory.createLineChart("Rentals X Iteration", "#Iteration", "#Rentals", RentalXIterData);
			
			
			//Set a tick unit of 1 without decimal
			CategoryPlot plot = chartRxI.getCategoryPlot();
			NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
			rangeAxis.setTickUnit(new NumberTickUnit(1));
			
		     try {
				ChartUtilities.saveChartAsPNG(new File(this.controler.getControlerIO().getOutputFilename("chart_Rentals X Iters.png")), chartRxI, 800, 450);
				ChartUtilities.saveChartAsPNG(new File(this.controler.getControlerIO().getOutputFilename("plot_Rentals X Iters.png")), plotRxI, 800, 450);
		
		     } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//************************************************
		//PROVA Lista DISTANCE FROM CARSHARING STATION.TXT
		//************************************************
		
		final BufferedWriter time2StationW = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(event.getIteration(), "Time2StationW.txt"));
				
		try {
			time2StationW.write("personID,accessTime,bookingTime");
			time2StationW.newLine();		
		
		for (Id<Person> personId: agentRentalsMap.keySet()) {
			
			for (RentalInfo i : agentRentalsMap.get(personId).getArr()) {
				//CSVehicle vehicle = this.carsharingSupply.getAllVehicles().get(i.getVehId().toString());		
				double accessTime = i.getAccessEndTime() - i.getAccessStartTime();
				double bookingTime = i.getEndTime() - i.getStartTime();
				time2StationW.write(personId + "," + (i.getAccessEndTime() - i.getAccessStartTime()) + "," + bookingTime);
				time2StationW.newLine();
			}
			
		}

		time2StationW.flush();
		time2StationW.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//**************************************************************************
		//PROVA Grafico WALKING DISTANCE(time) FROM CARSHARING STATION PER ITERATION
		//**************************************************************************
		
		DefaultCategoryDataset time2StationWGraphXITER = new DefaultCategoryDataset();

		
		for (Id<Person> personId: agentRentalsMap.keySet()) {
				
			for (RentalInfo i : agentRentalsMap.get(personId).getArr()) {
				double accessTime = i.getAccessEndTime() - i.getAccessStartTime();
				double bookingTime = i.getEndTime() - i.getStartTime();
				
				time2StationWGraphXITER.setValue(accessTime, personId, "personID" + personId);
			}
		}
		
	    int k = controler.getIterationNumber();			
		//Create a chart
		JFreeChart chartT2SXITER = ChartFactory.createBarChart("Time2StationITER" + k, "UserID", "Time", time2StationWGraphXITER, PlotOrientation.VERTICAL, false, true, true);
		
	    try {
	    	//Write something to get the iteration number in order to print one chart per iteration
		ChartUtilities.saveChartAsPNG(new File(this.controler.getControlerIO().getIterationFilename(event.getIteration(),"chart_Time2StationW.png")),chartT2SXITER, 600, 450);	// getIterationFilename(event.getIteration()"chart_Time2StationW.png"), 
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//************************************************************
		//PROVA Grafico WALKING DISTANCE(time) FROM CARSHARING STATION 
		//************************************************************
		
		if (event.getIteration() == controler.getConfig().controler().getLastIteration()) {
			
			DefaultCategoryDataset time2StationWGraph = new DefaultCategoryDataset();
			
			for (Id<Person> personId: agentRentalsMap.keySet()) {
				
				for (RentalInfo i : agentRentalsMap.get(personId).getArr()) {
					double accessTime = i.getAccessEndTime() - i.getAccessStartTime();
					double bookingTime = i.getEndTime() - i.getStartTime();
				    
					time2StationWGraph.setValue(accessTime, personId, "personID" + personId);
				}	
			}			
			
			//Create a chart
			JFreeChart chartT2S = ChartFactory.createBarChart("Time2Station", "UserID", "Time", time2StationWGraph, PlotOrientation.VERTICAL, true, true, true);
			JFreeChart plotT2S = ChartFactory.createLineChart("Time2Station", "UserID", "Time", time2StationWGraph);
			
		    try {
	    		ChartUtilities.saveChartAsPNG(new File(this.controler.getControlerIO().getOutputFilename("chart_Time2StationW.png")), chartT2S, 600, 450);
	    		ChartUtilities.saveChartAsPNG(new File(this.controler.getControlerIO().getOutputFilename("plot_Time2StationW.png")), plotT2S, 600, 450);	
		    } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//************************************************************
		//PROVA Grafico LENGTH OF THE CARSHARING JOURNEY PER ITERATION
		//************************************************************
		
		DefaultCategoryDataset journeyLengthITER = new DefaultCategoryDataset();
		
		for (Id<Person> personId: agentRentalsMap.keySet()) {
			
			for (RentalInfo i : agentRentalsMap.get(personId).getArr()) {
			    
				journeyLengthITER.setValue(i.getDistance(), personId, "personID" + personId);
			}
			
		}
	    int iterk = controler.getIterationNumber();	
		//Create a chart
		JFreeChart chartJourneyLength = ChartFactory.createBarChart("journeyLength" + iterk, "UserID", "length", journeyLengthITER, PlotOrientation.VERTICAL, false, true, true);
		
	    try {
	    	//Write something to get the iteration number in order to print one chart per iteration
		ChartUtilities.saveChartAsPNG(new File(this.controler.getControlerIO().getIterationFilename(event.getIteration(),"chartJourneyLength.png")), chartJourneyLength, 600, 450);	
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			    
	}			
}

