/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.contrib.carsharing.runExample;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Inject;

/**
 * @author nagel
 *
 */
public class RunCarsharingIT {
	private final static Logger log = Logger.getLogger(RunCarsharingIT.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void test() {
		Config config = ConfigUtils.loadConfig(utils.getClassInputDirectory() + "config.xml",
				new FreeFloatingConfigGroup(), new OneWayCarsharingConfigGroup(), new TwoWayCarsharingConfigGroup(),
				new CarsharingConfigGroup());

		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		config.network().setInputFile("network.xml");

		config.plans().setInputFile("plans.xml");
		config.plans().setInputPersonAttributeFile("plansAttributes.xml");

		// config.facilities().setInputFile("facilities.xml" );
		// config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.fromFile);

		config.plansCalcRoute().setInsertingAccessEgressWalk(false); // otherwise does not work. kai,feb'16

		CarsharingConfigGroup csConfig = (CarsharingConfigGroup) config.getModule(CarsharingConfigGroup.GROUP_NAME);
		csConfig.setvehiclelocations(utils.getClassInputDirectory() + "/CarsharingStations.xml");
		csConfig.setmembership(utils.getClassInputDirectory() + "/CSMembership.xml");
		// OneWayCarsharingConfigGroup oneWayConfig = (OneWayCarsharingConfigGroup)
		// config.getModule( OneWayCarsharingConfigGroup.GROUP_NAME ) ;
		// oneWayConfig.setvehiclelocations(
		// utils.getClassInputDirectory()+"/Stations.txt");

		// TwoWayCarsharingConfigGroup twoWayConfig = (TwoWayCarsharingConfigGroup)
		// config.getModule( TwoWayCarsharingConfigGroup.GROUP_NAME ) ;
		// twoWayConfig.setvehiclelocations(
		// utils.getClassInputDirectory()+"/CarsharingStations.xml");

		config.subtourModeChoice().setBehavior(SubtourModeChoice.Behavior.fromAllModesToSpecifiedModes);
		config.subtourModeChoice().setProbaForRandomSingleTripMode(0.);

		// config.global().setNumberOfThreads(1);
		// config.qsim().setNumberOfThreads(1);

		// ---

		Scenario scenario = ScenarioUtils.loadScenario(config);
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		
		//--------------------------- Parse income & vot -----------------------
		parseCustomeAttr(utils.getClassInputDirectory() + config.plans().getInputFile(), scenario);
		
		// ---

		final Controler controler = new Controler(scenario);
		// controler.setDirtyShutdown(true);

		RunCarsharing.installCarSharing(controler);

		final MyAnalysis myAnalysis = new MyAnalysis();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.bind(MyAnalysis.class).toInstance(myAnalysis);
				this.addControlerListenerBinding().toInstance(myAnalysis);
			}
		});

		// ---

		controler.run();

		log.info("done");
	}

	public static void parseCustomeAttr(String FileName,Scenario sc) {

		try {

			File fXmlFile = new File(FileName);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			
			//for skipping population_v6.dtd
			dBuilder.setEntityResolver(new EntityResolver() {
		        @Override
		        public InputSource resolveEntity(String publicId, String systemId)
		                throws SAXException, IOException {
		            if (systemId.contains("population_v6.dtd")) {
		                return new InputSource(new StringReader(""));
		            } else {
		                return null;
		            }
		        }
		    });

			Document doc = dBuilder.parse(fXmlFile);

			// optional, but recommended
			// read this -
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			//System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

			NodeList nList = doc.getElementsByTagName("person");

			System.out.println("----------------------------");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);

				//System.out.println("\nCurrent Element :" + nNode.getNodeName());

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;
					String id = eElement.getAttribute("id");
					String personIncome = eElement.getAttribute("income");
					String personVot = eElement.getAttribute("vot");
					
					Id<Person> personId = Id.create(id, Person.class);
					
					Integer income = new Integer(personIncome);
					Double vot = new Double(personVot);

		
					Person person = sc.getPopulation().getPersons().get(personId);
					if (income >= 0) {
						person.getAttributes().putAttribute("income", income);
						System.out.println("--------------- id: "+person.getId()+",  income: "+person.getAttributes().getAttribute("income"));
					} else {
						person.getAttributes().putAttribute("income", -1);
						System.err.println("Income is not a positive number.");
					}
					
					if (vot >= 0) {
						person.getAttributes().putAttribute("vot", vot);
						System.out.println("--------------- id: "+person.getId()+",  vot: "+person.getAttributes().getAttribute("vot"));
					} else {
						person.getAttributes().putAttribute("vot", -1.0);
						System.err.println("Income is not a positive number.");
					}
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class MyAnalysis implements AfterMobsimListener {
		@Inject
		private LegHistogram histogram;

		void testOutput(int iteration) {
			int nofLegs = 0;
			for (int nofDepartures : this.histogram.getDepartures()) {
				nofLegs += nofDepartures;
			}
			log.info("number of legs:\t" + nofLegs + "\t100%");
			for (String legMode : this.histogram.getLegModes()) {
				int nOfModeLegs = 0;
				for (int nofDepartures : this.histogram.getDepartures(legMode)) {
					nOfModeLegs += nofDepartures;
				}
				/*
				 * if ( iteration==0 ) { if ( TransportMode.walk.equals(legMode) ) {
				 * Assert.assertEquals( 117803, nOfModeLegs ); } else if (
				 * "oneway_vehicle".equals(legMode) ) { Assert.assertEquals( 0, nOfModeLegs ) ;
				 * } else if ( TransportMode.car.equals(legMode) ) { Assert.assertEquals(
				 * 820905, nOfModeLegs ) ; } else if ( "egress_walk_ow".equals(legMode) ) {
				 * Assert.assertEquals( 0, nOfModeLegs ) ; } else if (
				 * "access_walk_ow".equals(legMode) ) { Assert.assertEquals( 0, nOfModeLegs ) ;
				 * } } else if (iteration == 10) {
				 * 
				 * if ( TransportMode.walk.equals(legMode) ) { Assert.assertEquals(0,
				 * nOfModeLegs ); // Assert.assertEquals(8, nOfModeLegs ); } else if (
				 * "bike".equals(legMode) ) { Assert.assertEquals( 2, nOfModeLegs ) ; } else if
				 * ( TransportMode.car.equals(legMode) ) { Assert.assertEquals( 0, nOfModeLegs )
				 * ; } else if ( "twoway_vehicle".equals(legMode) ) { Assert.assertEquals( 10,
				 * nOfModeLegs ) ; // Assert.assertEquals( 8, nOfModeLegs ) ; } else if (
				 * "oneway_vehicle".equals(legMode) ) { Assert.assertEquals( 0, nOfModeLegs ) ;
				 * 
				 * } else if ( "egress_walk_ow".equals(legMode) ) { Assert.assertEquals( 0,
				 * nOfModeLegs ) ; } else if ( "access_walk_ow".equals(legMode) ) {
				 * Assert.assertEquals( 0, nOfModeLegs ) ; } else if (
				 * "egress_walk_tw".equals(legMode) ) { Assert.assertEquals( 4, nOfModeLegs ) ;
				 * } else if ( "access_walk_tw".equals(legMode) ) { Assert.assertEquals( 4,
				 * nOfModeLegs ) ; } else if ( "egress_walk_ff".equals(legMode) ) {
				 * Assert.assertEquals( 0, nOfModeLegs ) ; } else if (
				 * "access_walk_ff".equals(legMode) ) { Assert.assertEquals( 0, nOfModeLegs ) ;
				 * } }
				 * 
				 * else if ( iteration==20 ) { if ( TransportMode.walk.equals(legMode) ) {
				 * Assert.assertEquals(4, nOfModeLegs ); } else if (
				 * "twoway_vehicle".equals(legMode) ) { Assert.assertEquals( 10, nOfModeLegs ) ;
				 * // Assert.assertEquals( 8, nOfModeLegs ) ; } else if (
				 * TransportMode.car.equals(legMode) ) { Assert.assertEquals( 0, nOfModeLegs ) ;
				 * } else if ( "egress_walk_tw".equals(legMode) ) { Assert.assertEquals( 4,
				 * nOfModeLegs ) ; } else if ( "access_walk_tw".equals(legMode) ) {
				 * Assert.assertEquals( 4, nOfModeLegs ) ; } else if (
				 * "access_walk_ff".equals(legMode) ) { Assert.assertEquals( 1, nOfModeLegs ) ;
				 * } }
				 */
			}

		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent event) {
			testOutput(event.getIteration());
		}

	}
}
