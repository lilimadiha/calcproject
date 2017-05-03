/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package examples.message;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author U
 */
public class CalcSender extends Agent 
{	
    private CalcGUI calcGui;
    static final Base64 base64 = new Base64();
    private AID calcServiceAgentAID = null;
    
    //object to string
    public String serializeObjectToString(Object object) throws IOException 
    {
        String s = null;
        
        try 
        {
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(arrayOutputStream);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream);         
        
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
            gzipOutputStream.close();
            
            objectOutputStream.flush();
            objectOutputStream.close();
            
            s = new String(base64.encode(arrayOutputStream.toByteArray()));
            arrayOutputStream.flush();
            arrayOutputStream.close();
        }
        catch(Exception ex){}
        
        return s;
    }
    
    //string to object
    public Object deserializeObjectFromString(String objectString) throws IOException, ClassNotFoundException 
    {
        Object obj = null;
        try
        {    
            ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(base64.decode(objectString));
            GZIPInputStream gzipInputStream = new GZIPInputStream(arrayInputStream);
            ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream);
            obj =  objectInputStream.readObject();
            
            objectInputStream.close();
            gzipInputStream.close();
            arrayInputStream.close();
        }
        catch(Exception ex){}
        return obj;
    }
        
    protected void setup() 
    {        
        calcGui = new CalcGUI(this);
	calcGui.showGui();
        
        //for receiving calculation result	
	addBehaviour(new CyclicBehaviour(this) 
	{            
            public void action() 
            { 
                ACLMessage msg= receive();
                
		if (msg != null) {
                    calcGui.appendLog("\n");
                    calcGui.appendLog("Message received from " + msg.getSender());
                    
                    String msgContent = msg.getContent();
                    calcGui.appendLog("Message content [Base64 string]: " + msgContent);
                    calcGui.appendLog("Msg performative: " + ACLMessage.getPerformative(msg.getPerformative()));                   
                    
                    try
                    {
                        Calculation calc = (Calculation)deserializeObjectFromString(msgContent);
                        
                        if (calc.isSuccess()) {
                            calcGui.appendLog("Calculation - result   : " + calc.getResult());                                                     
                            calcGui.appendLog("Calculation - info   : " + calc.getInfo());
                        } else {
                            calcGui.appendLog("Calculation - info   : " + calc.getInfo());
                            calcGui.appendLog("Msg performative: " + ACLMessage.getPerformative(msg.getPerformative()));
                        }
                        
                        calcGui.showResult(calc);                                                
                    }
                    catch(Exception ex)
                    {
                        calcGui.appendLog("StrToObj conversion error: " + ex.getMessage());
                    }
                }
                
                calcGui.appendLog("[CalcAgentPlus] CyclicBehaviour Block");
                block();
            }
        });
    }
    
    public void getCalcServiceAgent() {
  	try {
            String serviceType = "basic-calculator";
            calcGui.appendLog("Searching the DF/Yellow-Pages for " + serviceType + " service");
            calcGui.appendLog("Service properties: plus, minus, multiply, divide");
            
            // Build the description used as template for the search
            DFAgentDescription template = new DFAgentDescription();
            
            ServiceDescription templateSd = new ServiceDescription();
            templateSd.setType(serviceType);
            templateSd.addProperties(new Property("operation1", "plus"));
            //templateSd.addProperties(new Property("operation2", "minus"));
            //templateSd.addProperties(new Property("operation3", "multiply"));
            //templateSd.addProperties(new Property("operation4", "divide"));
            template.addServices(templateSd);
  		
            SearchConstraints sc = new SearchConstraints();
            // We want to receive 10 results at most
            sc.setMaxResults(new Long(10));
  		
            DFAgentDescription[] results = DFService.search(this, template, sc);
            if (results.length > 0) {
  		calcGui.appendLog("Agent "+getLocalName()+" found the following " + serviceType + " services:");
  		for (int i = 0; i < results.length; ++i) {
                    DFAgentDescription dfd = results[i];
                    AID agentAID = dfd.getName();
                    calcGui.popup("Agent name: " + agentAID);
                    calcGui.appendLog("Agent name: " + agentAID);
                    calcGui.appendLog("\n"); 
  		}
                
                //just use the first one
                DFAgentDescription dfd = results[0];
                calcServiceAgentAID = dfd.getName();
                
                //enable calcGui.combobox and submit button
                calcGui.enabledGUI();
            }	
            else {
                calcGui.appendLog("Agent "+getLocalName()+" did not find any " + serviceType + " service");
                calcGui.popup("No " + serviceType + " agent service found!");
            }
  	}
  	catch (FIPAException fe) {
            fe.printStackTrace();
  	}
        calcGui.appendLog("\n");        
    }
    
    public void requestCalculation(Calculation calObj) {
        calcGui.clearLog();
        calcGui.appendLog("Receiving calc request and calculation object from CalcGUI");
        calcGui.appendLog("Calculation object - operation: " + calObj.getOperation());
        calcGui.appendLog("Calculation object - operand1 : " + calObj.getOperand1());
        calcGui.appendLog("Calculation object - operand2 : " + calObj.getOperand2());   
        calcGui.appendLog("\n");
        
        //Send messages to "cap - CalcAgentPlus"  
        calcGui.appendLog("Preparing ACL msg: INFORM");   
	ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        
        calcGui.appendLog("Convert calculation obj to String Base64");   
        String strObj = ""; 
        try
        {
            strObj = serializeObjectToString(calObj);
        }
        catch (Exception ex)
        {
            System.out.println("\n[CalcSender] ObjToStr conversion error: " + ex.getMessage());
        }
        
	msg.setContent(strObj);
        String operator = calObj.getOperation();
        
        if (operator == "plus") {
            msg.addReceiver(new AID("cap", AID.ISLOCALNAME));
        } else if(operator == "minus") {
            msg.addReceiver(new AID("cas", AID.ISLOCALNAME));
        } else if(operator == "multiply") {
            msg.addReceiver(new AID("cam", AID.ISLOCALNAME));
        } else if(operator == "divide") {
            msg.addReceiver(new AID("cad", AID.ISLOCALNAME));
        } 

        send(msg);
        
        calcGui.appendLog("Sending Message to cap");
        calcGui.appendLog("Message content [Base64 string]: " + strObj);   
    }
}
