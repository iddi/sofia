/*******************************************************************************
 * Copyright (c) 2009,2011 Tecnalia Research and Innovation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Cristina López (Fundacion European Software Institute) - initial API, implementation and documentation
 *******************************************************************************/ 

package eu.sofia.adk.osgi.commands.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import eu.sofia.adk.gateway.exception.GatewayException;
import eu.sofia.adk.gateway.service.IGateway;
import eu.sofia.adk.gateway.service.IGatewayFactory;
import eu.sofia.adk.osgi.commands.Activator;
import eu.sofia.adk.osgi.commands.data.Element;
import eu.sofia.adk.sib.exception.SIBException;
import eu.sofia.adk.sib.service.ISIB;
import eu.sofia.adk.sib.service.ISIBFactory;

/**
 * This class extend the OSGi commands provider by default.
 * The extensions are exclusively part of the SOFIA project.
 * 
 * All the SOFIA commands start with the keyword "sspace" followed by the operation to execute.
 * This is the list of operations:
 * 	 sspace help
 * 	 sspace help props
 * 	 sspace list
 * 	 sspace info 
 * 	 sspace create
 * 	 sspace destroy
 * 	 sspace start
 * 	 sspace stop
 * 
 * The syntax of the commands can be checked typing "sspace help" in the OSGi console
 * 
 * 
 * @author Cristina López, cristina.lopez@tecnalia.com, Tecnalia
 *
 */
public class SofiaCommandProvider implements CommandProvider{
	
	/**
	 * Gateway Manager Services Map
	 */
	protected HashMap<String, IGatewayFactory> gwMgtMap=null;
	
	/**
	 * SIB Factory Service
	 */
	protected ISIBFactory sibMgt=null;
	
	/**
	 * Abstract Gateway Factory Service
	 */
	protected IGatewayFactory gwMgt=null;
	
	/**
	 * Constructor
	 */
	public SofiaCommandProvider(){
		gwMgtMap= Activator.getDefault().getgwmgtstc().getGwMgtMap();
		sibMgt= Activator.getDefault().getMgtstc().getSibManager();
		
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.console.CommandProvider#getHelp()
	 */
	public String getHelp() {
		StringBuffer buffer= new StringBuffer(60);
		buffer.append("---SOFIA Commands---\n\t");
		buffer.append("sspace list -> List the available Servers and Gateways\n\t");
		buffer.append("sspace create -sib -name=XXX -> Creates a Sib instance\n\t");
		buffer.append("sspace create -gw -name=XXX -type="+getGatewayTypes()+" -idSib=99999 -props=p1=v1,p2=v2,...,pn=vn -> Creates a Gateway instance\n\t");
		buffer.append("sspace destroy [-sib|-gw] -id=99999 -> Destroys a Sib or Gateway instance\n\t");
		buffer.append("sspace start [-sib|-gw] -id=99999 -> Starts a Sib or Gateway\n\t");
		buffer.append("sspace stop [-sib|-gw] -id=99999 -> Stops a Sib or Gateway\n\t");
		buffer.append("sspace info [-sib|-gw] -id=99999 -> List the properties of a Sib or Gateway\n\t");
		return buffer.toString();
	}
	
	/** 
	 * This method prints in the console the default configurable properties of the different types of Gateway
	 * The method is invoked when the command "sspace help props" is executed
	 * @return String
	 */
	public String getHelpProps(){
		StringBuffer buffer= new StringBuffer(60);
		Iterator<String> it=gwMgtMap.keySet().iterator();
		while (it.hasNext()){
			String type= it.next();
			buffer.append("These are the default configurable properties values for a Gateway of type "+type+"\n\t");
			Properties properties=gwMgtMap.get(type).getConfigurableProperties();
			Iterator<Object> it2=properties.keySet().iterator();			
			while(it2.hasNext()){
				Object label=it2.next();
				String value= properties.get(label).toString();
				buffer.append(label+" :"+value+"\n\t");
			}
			buffer.append("\n");
			
		}
		return buffer.toString();
	}
	
	/**
	 * Method that defines the commands of SOFIA
	 * @param ci
	 */
	public void _sspace(CommandInterpreter ci){
		String operation= ci.nextArgument();
		if(operation!=null){
			if(operation.equals("list")){
				list(ci);
			} else if (operation.equals("create")){
				create(ci);
			} else if (operation.equals("destroy")){
				destroy(ci);
			} else if (operation.equals("start")){
				start(ci);
			} else if (operation.equals("stop")){
				stop(ci);
			} else if (operation.equals("info")){
				info(ci);
			}else if (operation.equals("help")){
				String props= ci.nextArgument();
				if(props==null || props.equals("")){
					ci.println(getHelp());
				}
				else if (props.equals("props")){
					ci.println(getHelpProps());
				}
			} else{
				ci.println("The operation introduced does not exits. Please, choose another operation from the list: ");
				ci.println(getHelp());
			}
			
		}else{
			ci.println("The syntax of the command is not correct.");
			ci.println(getHelp());
		}
		
	}

	/**
	 * Method that implements the logic of the command "list"
	 * @param ci
	 */
	public void list(CommandInterpreter ci){
		
		if (Activator.getDefault().getSibList().size()>0){
				ci.println("List of active SIBs and Gateways: ");		
				ci.println(String.format("%5s %27s %27s %10s %7s", " Id  ", "Sib Name         ", "Gw Name          ", "Gw Type   ", "Status"));
				String line="----- --------------------------- --------------------------- ---------- -------";
				ci.println(line);
				
				for (int i=0; i<Activator.getDefault().getSibList().size();i++){
					
					Element element=Activator.getDefault().getSibList().get(i);
					ISIB sib= Activator.getDefault().getSibList().get(i).getSib();
					String status="";
					if (sib.isRunning()){
						status="Started";
					}
					else if (sib.isStopped()){
						status="Stopped";
					}
					//Print the SIB related information
					ci.println(String.format("%5s %27s %27s %10s %7s", Integer.toString(sib.getId()), sib.getName(), " "," ",status));
					
					//Print the SIB Gateways related information
					for (int j=0; j< element.getGateways().size();j++){
						IGateway gw= element.getGateways().get(j);
						String gwStatus="";
						if (gw.isRunning()){
							gwStatus="Started";
						}
						else if (gw.isStopped()){
							gwStatus="Stopped";
						}
						ci.println(String.format("%5s %27s %27s %10s %7s", Integer.toString(gw.getId()),"", gw.getName(), gw.getType(),gwStatus));
					}
					
				}
		}
		else{
			ci.println("There are not active servers.");	
		}
				
	}
	
	/**
	 * Method that implements the logic of the command "create"
	 * @param ci
	 */
	public void create(CommandInterpreter ci){
		try{
			boolean error=true;
			String op= ci.nextArgument();
			String param=ci.nextArgument();
			if(op!=null && param!=null && param.startsWith("-name=") && (op.equals("-sib") || op.equals("-gw"))){
				//Create Sib instance
				String name=param.substring(6);
				if (name!=null && !name.isEmpty())
				{
					if (op.equals("-sib")){
						error=false;
							int id=sibMgt.create(name,null);
							ci.println("Server "+name+ " created with id "+id);	
					}
					//Create Gateway instance
					else if (op.equals("-gw")){
						
						String typeParam=ci.nextArgument();

						if (typeParam!=null && typeParam.startsWith("-type=")){
							
							String gwType=typeParam.substring(6);
							if(gwType!=null && !gwType.isEmpty() && gwTypeExits(gwType) ){	
									
								String sibParam=ci.nextArgument();
								if (sibParam!=null && sibParam.startsWith("-idSib=")){
									
									String idSib=sibParam.substring(7);
									error=false;
									String propsParam= ci.nextArgument();
									IGatewayFactory gwMgt=gwMgtMap.get(gwType);
									ISIB sibServer= getSIBbyID(Integer.parseInt(idSib));
									Properties gwProps= gwMgt.getConfigurableProperties();
									if (propsParam!=null ){
										//ArrayList with Ip addresses in case gw is TCPIP that will be used to store the IPs introduced by user
										ArrayList<String> ipAddresses = new ArrayList<String>();
										if (propsParam.startsWith("-props=")){
											String props=propsParam.substring(7);
											
											if(props!=null && !props.isEmpty()){
												String[] propsList=props.split(",");
												for (int i=0; i< propsList.length;i++){
													String current= propsList[i];
													String[] values=current.split("=");
													
													//need to check if the gateway provided is TPC/IP
													if (gwType.equals("TCP/IP") && values[0].equals("IPADDRESS"))
													{
														//then if the prop key is IPADDRESS then we buid the ArrayList that must be inserted in the props as ADDRESSES
														//in order for the GW to start correctly
														ipAddresses.add(values[1]);
													
													}
													else
														gwProps.put(values[0], values[1]);								
												}
												//in case the gw is TCP/IP, add the ADDRESSES property 
												if (gwType.equals("TCP/IP"))
													gwProps.put("ADDRESSES", ipAddresses);
											}
											else{
												error=true;
												ci.println(getHelpProps());
											}
										}
										else{
											error=true;
											ci.println(getHelpProps());
										}
									}
									
									if(error==false)
									{
										int id=gwMgt.create(name, sibServer, gwProps);
										ci.println("Gateway "+name+ " created with id "+id);
									}
								}							
							}
							else if (!gwTypeExits(gwType)){
								ci.println("The gateway type introduced does not exits.");
								ci.println("The available gateway types are: "+getGatewayTypes());
								ci.println("");
							}	
						}
					}
				}
			}
			if(error==true){
				createErrorMessage(ci);
			}
		}
		catch(SIBException e){ e.printStackTrace();}
		catch(GatewayException e){ e.printStackTrace();}
		catch(Exception e){	e.printStackTrace();}
	}
	
	/**
	 * Method that implements the logic of the command destroy
	 * @param ci
	 */
	public void destroy(CommandInterpreter ci){
		try{
			boolean error=true;
			String op= ci.nextArgument();
			String param= ci.nextArgument();
			if(op!=null && param!=null && param.startsWith("-id=")&& (op.equals("-sib") || op.equals("-gw"))){
				String id=param.substring(4);
				if (id!=null && !id.isEmpty())
				{
					error=false;
					if (op.equals("-sib")){
						List<IGateway> gwList= getSIBGateways(Integer.parseInt(id));
						
						while(gwList.size()>0){
							IGateway gw=gwList.get(0);
							if (gw.isRunning()){
								gw.stop();
							}
							IGatewayFactory gwMgt=gwMgtMap.get(gw.getType());
							gwMgt.destroy(gw.getId());
							ci.println("Gateway "+id+ " deleted");
						}
						sibMgt.destroy(Integer.parseInt(id));
						ci.println("Server "+id+ " deleted");					
					}
					else if (op.equals("-gw")){
						IGateway gw=getGatewayById(Integer.parseInt(id));
						if (gw.isRunning()){
							gw.stop();
						}
						IGatewayFactory gwMgt=gwMgtMap.get(gw.getType());
						gwMgt.destroy(Integer.parseInt(id));
						ci.println("Gateway "+id+ " deleted");
					}
				}				
			} 
			if (error==true){
				destroyErrorMessage(ci);
			}
		}
		catch(SIBException e){ 
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (GatewayException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Method that implements the logic of the command "start"
	 * @param ci
	 */
	public void start(CommandInterpreter ci){
		try{
			boolean error=true;
			String op=ci.nextArgument();
			String param= ci.nextArgument();
			if(op!=null && param!=null && param.startsWith("-id=") && (op.equals("-sib") || op.equals("-gw"))){
				String id= param.substring(4);
				if(id!=null && !id.isEmpty()){
					error=false;
					if (op.equals("-sib")){
						getSIBbyID(Integer.parseInt(id)).start();
						ci.println("Server "+id+ " started sucessfully");
					}
					else if (op.equals("-gw")){
						IGateway gw=getGatewayById(Integer.parseInt(id));
						if (gw.getSIB().isRunning()){
							gw.start();
							ci.println("Gateway "+id+ " started sucessfully");
						}
						else{
							ci.println("In order to start the Gateway, the corresponding SIB server must have been previously started");
						}
					}
				}
			}
			if (error==true){
				startErrorMessage(ci);
			}
		}
		catch(SIBException e){ 
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (GatewayException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Method that implements the logic of the command "stop"
	 * @param ci
	 */
	public void stop(CommandInterpreter ci){
		try{
			boolean error=true;
			String op= ci.nextArgument();
			String param=ci.nextArgument();
			if(op!=null && param!=null && param.startsWith("-id=") && (op.equals("-sib") || op.equals("-gw"))){
				String id= param.substring(4);
				if(id!=null && !id.isEmpty())
				{
					error=false;
					if (op.equals("-sib")){
						List<IGateway> gwList= getSIBGateways(Integer.parseInt(id));
						for (int i=0; i<gwList.size();i++){
							IGateway gw=gwList.get(i);
							if (gw.isRunning()){
								gw.stop();
							}
							ci.println("gateway "+gw.getId()+ " stopped sucessfully");
						}
						
						getSIBbyID(Integer.parseInt(id)).stop();
						ci.println("Server "+id+ " stopped sucessfully");
					}
					else if (op.equals("-gw")){
						getGatewayById(Integer.parseInt(id)).stop();
						ci.println("gateway "+id+ " stopped sucessfully");
					}
				}
			}
			if (error==true){
				stopErrorMessage(ci);
			}
		}
		catch(SIBException e){ 
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (GatewayException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Method that implements the logic of the command "info"
	 * @param ci
	 */
	public void info(CommandInterpreter ci){
		try{
			boolean error=true;
			String el= ci.nextArgument();
			String param=ci.nextArgument();
			if(el!=null && param!=null && param.startsWith("-id=") && (el.equals("-sib") || el.equals("-gw"))){
				String id= param.substring(4);
				if(id!=null && !id.isEmpty())
				{
					error=false;
					if (el.equals("-sib") ){
						
						ISIB sib= getSIBbyID(Integer.parseInt(id));
						if (sib!=null){
							ci.println("Id: "+sib.getId());
							ci.println("Name: "+sib.getName());
							String status="";
							if (sib.isRunning()){
								status="Started";
							} else if (sib.isStopped()){
								status="Stopped";
							}
							ci.println("Status: "+ status);
						}
						else{
							ci.println("It does not exist a SIB with the identifier provided");
						}
					}
					else if (el.equals("-gw") ){
						IGateway gw= getGatewayById(Integer.parseInt(id));
						if (gw!=null){
							ci.println("Id: "+gw.getId());
							ci.println("Name: "+gw.getName());
							String status="";
							if (gw.isRunning()){
								status="Started";
							} else if (gw.isStopped()){
								status="Stopped";
							}
							ci.println("Status: "+status);
							Properties props=gw.getProperties();
							Iterator<Object> it=props.keySet().iterator();
							 while(it.hasNext()){
								 Object p= it.next();
								 String label=p.toString();
								 String value=props.get(p).toString();
								 ci.println(label+": "+ value);			 
							 }
						}
						else{
							ci.println("It does not exist a Gateway with the identifier provided");
						}
					}
				}
			}
			if (error==true){
				infoErrorMessage(ci);
			}

		}
		catch(Exception e){e.printStackTrace();}
	}
	
	/**
	 * This method returns a String containing the gateway types that are available is the OSGi registry
	 * @return String
	 */
	private String getGatewayTypes(){
		String _return="";
		
		_return=gwMgtMap.keySet().toString();
		
		return _return;
	}
	
	/**
	 * This method checks if the type received as parameter exits in the OSGi registry
	 * @param type
	 * @return boolean
	 */
	private boolean gwTypeExits(String type){
		boolean exits=false;
		Object[] types=(Object[]) gwMgtMap.keySet().toArray();
		int i=0;
		while (i<types.length && exits==false){
			String current=types[i].toString();
			if (current.equals(type)){
				exits=true;
			}
			i++;
		}
		
		return exits;
	}
	
	/**
	 * This method seeks a SIB server in the list of SIB instances registered in OSGi
	 * @param id
	 * @return ISIB
	 */
	private ISIB getSIBbyID(int id){
		ISIB sib=null;
		List<Element> elements= Activator.getDefault().getSibList();
		
		int i=0;
		boolean found=false;
		while (i<elements.size() && found==false){
			if (elements.get(i).getSib().getId()==id){
				found=true;
				sib= elements.get(i).getSib();
			}
			else {
				i++;
			}
			
		}
		
		return sib;
		
	}
	
	/**
	 * This method returns the list of Gateways of an specific SIB server 
	 * @param id
	 * @return List<IGateway>
	 */
	private List<IGateway> getSIBGateways(int id){
		List<IGateway> gwList= new ArrayList<IGateway>();
		
		List<Element> elements= Activator.getDefault().getSibList();
		int i=0;
		boolean found=false;
		while (i<elements.size() && found==false){
			if (elements.get(i).getSib().getId()==id){
				found=true;
				gwList= elements.get(i).getGateways();
			}
			else {
				i++;
			}			
		}
		return gwList;
		
	}
	
	/**
	 * This method seeks a Gateway in the list og Gateway instance registered in OSGi
	 * @param id
	 * @return
	 */
	private IGateway getGatewayById(int id){
		IGateway gw= null;
		
		int i=0;
		boolean found=false;
		
		
		while (i<Activator.getDefault().getSibList().size() && found==false){
			Element element= Activator.getDefault().getSibList().get(i);
			for (int j=0; j<element.getGateways().size();j++){
				IGateway current= element.getGateways().get(j);
				if (current.getId()==id){
					found=true;
					gw=current;
				}
			}				
			i++;
		}
		
		return gw;
	}
	/*****  ERROR MESSAGES ******/
	
	/**
	 * Create command error message
	 */
	private void createErrorMessage(CommandInterpreter ci/*, int error*/){
		ci.println("The syntax of the create command is not correct.");
		/*switch(error){
			case 0:
				ci.println("Please, specify the operation to perform.");
				break;
			case 1:
				ci.println("Please, specify the name of the element to create.");
				break;
			case 2:
				ci.println("Please, specify the type of gateway.");
				break;
			case 3:
				ci.println("Please, specify the SIB related to the gateway.");
				break;
			default: 
				ci.println("Please, revise the syntax following the instructions below: ");
				break;
		
		}*/
		ci.println("sspace create -sib -name=XXX -> Creates a Sib instance");
		ci.println("sspace create -gw -name=XXX -type="+getGatewayTypes()+" -idSib=99999 -props=p1=v1,p2=v2,...,pn=vn -> Creates a Gateway instance");
	}
	
	/**
	 * Destroy command error message
	 * @param ci
	 */
	private void destroyErrorMessage(CommandInterpreter ci){
		ci.println("The syntax of the destroy command is not correct.");
		ci.println("sspace destroy [-sib|-gw] -id=99999 -> Destroys a Sib or Gateway instance");
	}
	
	/**
	 * Start command error message
	 * @param ci
	 */
	private void startErrorMessage(CommandInterpreter ci){
		ci.println("The syntax of the start command is not correct.");
		ci.println("sspace start [-sib|-gw] -id=99999 -> Starts a Sib or Gateway");
	}
	
	/**
	 * Stop command error message
	 * @param ci
	 */
	private void stopErrorMessage(CommandInterpreter ci){
		ci.println("The syntax of the stop command is not correct.");
		ci.println("sspace stop [-sib|-gw] -id=99999 -> Stops a Sib or Gateway");
	}
	
	/**
	 * Info command error message
	 * @param ci
	 */
	private void infoErrorMessage (CommandInterpreter ci){
		ci.println("the syntax of the info command is not correct.");
		ci.println("sspace info [-sib|-gw] -id=99999 -> List the properties of a Sib or Gateway");
	}

}
